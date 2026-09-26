package com.futuretv.player

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.net.wifi.WifiManager
import android.text.InputType
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.net.NetworkInterface
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Locale
import java.util.TimeZone
import org.json.JSONObject

class ActivationActivity : Activity() {
    private lateinit var mac: String
    private lateinit var status: TextView
    private lateinit var verifyButton: TextView
    private lateinit var connectButton: TextView
    private lateinit var extraSettingsButton: TextView
    private lateinit var testApiButton: TextView
    // Guarda a última config recebida do painel só pra alimentar o botão
    // "Testar API do painel" sem precisar de outra consulta de rede -- o
    // mesmo teste que já existia dentro de Configurações (showServerTestDialog
    // na MainActivity), só que disponível aqui também, igual pedido: o
    // usuário quer poder testar a API antes mesmo de entrar no app.
    private var lastFetchedConfig: RemoteAppConfig? = null
    private lateinit var connectionProgress: ProgressBar
    private lateinit var connectionPercent: TextView
    private lateinit var connectionClock: TextView
    private lateinit var connectionMessage: TextView
    private var checking = false
    private var loadingStartedAt = 0L
    private var mainOpened = false
    private var keepImporterAlive = false
    // Cada tentativa de conexão (painel ou manual) ganha um número novo. Callbacks
    // assíncronos só aplicam o resultado se ainda forem a tentativa mais recente --
    // isso evita que uma checagem antiga do painel, que só termina depois do
    // usuário já ter confirmado DNS/usuário/senha, sobrescreva ou bloqueie a
    // tentativa manual mais nova.
    private var connectionGeneration = 0
    private val integration = AppIntegrationRepository()
    private val playlistRepository by lazy { PlaylistRepository(this) }
    private val handler = Handler(Looper.getMainLooper())
    private var loadingPanelList = false
    private val clockTicker = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1_000)
        }
    }

    private val periodicCheck = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val manualMode = prefs.getString(PREF_SOURCE_MODE, SOURCE_PANEL) == SOURCE_MANUAL
            if (!manualMode && !checking && !loadingPanelList) {
                verifyAccess(false)
            }
            handler.postDelayed(this, 5_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        setContentView(R.layout.activity_activation)

        // BUG CRÍTICO corrigido: antes, TODA vez que a Activity abria, o app
        // calculava um MAC novo (via DeviceIdentifier.resolve) e sobrescrevia
        // o que já estava salvo -- mesmo instalando o APK por cima (update),
        // sem desinstalar. Isso fazia o MAC mudar a cada build novo instalado
        // (o Android às vezes reatribui o ANDROID_ID quando a assinatura de
        // debug do build muda, e a MAC real do WiFi pode não estar disponível
        // em vários aparelhos por restrição do próprio Android), obrigando a
        // recadastrar o MAC no painel toda hora. Agora, se já existe um MAC
        // salvo (de uma instalação anterior, ou digitado manualmente em
        // Configurações > Dispositivo/MAC), ele é sempre reaproveitado -- só
        // calcula um novo na primeira vez, quando não há nada salvo ainda.
        // Isso só funciona instalando por cima (update); desinstalar apaga
        // os dados do app e força um MAC novo na próxima abertura, sem jeito
        // de evitar isso -- é como o Android funciona.
        val earlyPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedMac = earlyPrefs.getString(PREF_MAC_ADDRESS, "").orEmpty()
        mac = savedMac.ifBlank {
            DeviceIdentifier.resolve(this).also { resolved ->
                earlyPrefs.edit().putString(PREF_MAC_ADDRESS, resolved).apply()
            }
        }
        val macValue = findViewById<TextView>(R.id.macValue)
        val macFormatted = findViewById<TextView>(R.id.macFormatted)
        macValue.text = mac
        macFormatted.text = "12 caracteres hexadecimais • toque para copiar"
        status = findViewById(R.id.activationStatus)
        verifyButton = findViewById(R.id.recheckButton)
        connectButton = findViewById(R.id.connectButton)
        extraSettingsButton = findViewById(R.id.extraSettingsButton)
        testApiButton = findViewById(R.id.testApiButton)
        connectionProgress = findViewById(R.id.connectionProgress)
        connectionPercent = findViewById(R.id.connectionPercent)
        connectionClock = findViewById(R.id.connectionClock)
        connectionMessage = findViewById(R.id.connectionMessage)
        loadingStartedAt = SystemClock.elapsedRealtime()
        handler.post(clockTicker)
        macValue.setOnClickListener { copyMac() }
        macFormatted.setOnClickListener { copyMac() }
        findViewById<TextView>(R.id.copyMacButton).setOnClickListener { copyMac() }
        connectButton.setOnClickListener { verifyAccess(true) }
        verifyButton.setOnClickListener {
            playlistRepository.invalidateCacheFreshness()
            verifyAccess(true)
        }
        extraSettingsButton.setOnClickListener { showExtraSettingsDialog() }
        testApiButton.setOnClickListener { showServerApiTestDialog() }
        connectButton.requestFocus()

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val manualDns = prefs.getString(PREF_MANUAL_DNS, "").orEmpty()
        val manualUser = prefs.getString(PREF_MANUAL_USER, "").orEmpty()
        val manualPassword = prefs.getString(PREF_MANUAL_PASSWORD, "").orEmpty()
        val manualConfigured = manualDns.isNotBlank() && manualUser.isNotBlank() && manualPassword.isNotBlank()
        // BUG corrigido: "SEU TESTE AQUI" carregava o catálogo de teste e abria
        // o app na hora, mas PREF_ACCESS_ALLOWED nunca era LIDO em lugar nenhum
        // -- então, ao fechar e reabrir o app, o onCreate ignorava que um teste
        // já tinha sido liberado e voltava a chamar verifyAccess(false) na cara
        // dura, que consulta o painel de VERDADE (não a API do Servidor do
        // teste). Como um teste não é um dispositivo "assinante" registrado no
        // nosso painel (e não deveria ser -- ver isTrialActiveFor em
        // MainActivity), a resposta ficava sempre "aguardando cadastro",
        // travando pra sempre na tela de MAC mesmo com o catálogo do teste já
        // salvo e pronto no SQLite local. Agora, enquanto o teste ainda estiver
        // dentro da validade (PREF_TRIAL_ACTIVE_UNTIL) e for do mesmo MAC, reabre
        // direto na MainActivity reaproveitando esse catálogo já baixado.
        val trialStillActive = prefs.getBoolean(PREF_ACCESS_ALLOWED, false) &&
            prefs.getString(PREF_TRIAL_MAC, "").equals(mac, ignoreCase = true) &&
            prefs.getLong(PREF_TRIAL_ACTIVE_UNTIL, 0L) > System.currentTimeMillis()
        if (prefs.getString(PREF_SOURCE_MODE, SOURCE_PANEL) == SOURCE_MANUAL && manualConfigured) {
            // Configuração extra já salva: conecta direto, sem depender do painel/MAC.
            setConnectionProgress(0, "Conectando com a configuração extra (DNS/usuário/senha)...")
            connectManual(manualDns, manualUser, manualPassword, showProgress = false)
        } else if (trialStillActive) {
            setConnectionProgress(90, "Teste ainda válido. Abrindo Future...")
            status.text = "Teste ainda válido. Abrindo Future..."
            status.setTextColor(getColor(R.color.success))
            openMainActivity(importInProgress = false)
        } else {
            setConnectionProgress(0, "Aguardando conexão com o painel...")
            verifyAccess(false)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                -> {
                    val targets = listOf(
                        findViewById<View>(R.id.macValue),
                        findViewById<View>(R.id.copyMacButton),
                        connectButton,
                        verifyButton,
                        extraSettingsButton,
                        testApiButton,
                    )
                    val current = targets.indexOf(currentFocus).coerceAtLeast(0)
                    val delta = if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) -1 else 1
                    return targets[(current + delta).coerceIn(0, targets.lastIndex)].requestFocus()
                }
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                -> {
                    val focused = currentFocus
                    if (focused != null && focused.isShown && focused.isEnabled && focused.isClickable) {
                        focused.performClick()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun updateClock() {
        if (!::connectionClock.isInitialized || loadingStartedAt == 0L) return
        val elapsed = ((SystemClock.elapsedRealtime() - loadingStartedAt) / 1000L).coerceAtLeast(0L)
        connectionClock.text = "◷ %02d:%02d".format(Locale.US, elapsed / 60, elapsed % 60)
    }

    private fun setConnectionProgress(value: Int, message: String) {
        if (!::connectionProgress.isInitialized) return
        connectionProgress.progress = value.coerceIn(0, 100)
        connectionPercent.text = "${value.coerceIn(0, 100)}%"
        connectionMessage.text = message
    }

    private fun explainFailure(reason: String): String {
        val safeReason = reason
            .replace(Regex("([?&](username|password)=)[^&\\s]+", RegexOption.IGNORE_CASE), "$1***")
            .replace(Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE), "servidor da lista")
        return when {
            reason.contains("403") -> "A lista foi recusada pelo servidor (HTTP 403). Verifique a URL/credenciais no painel."
            reason.contains("522") -> "O servidor da lista não respondeu (HTTP 522). Tentando novamente ou usando o cache local."
            reason.contains("timeout", true) || reason.contains("timed out", true) -> "O servidor da lista demorou demais para responder."
            reason.contains("HTML", true) -> "O servidor devolveu uma página de bloqueio, não uma lista M3U."
            safeReason.isNotBlank() -> "Falha ao baixar a lista do painel: $safeReason"
            else -> "Falha ao baixar a lista do painel."
        }
    }

    private fun copyMac() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("MAC do dispositivo", mac))
        Toast.makeText(this, "MAC copiado", Toast.LENGTH_SHORT).show()
    }

    // Mesmo teste que já existia em Configurações -> "Testar API do servidor"
    // (MainActivity.showServerTestDialog), disponível aqui também: testa a
    // "test_api_url" cadastrada no painel na seção PRÓPRIA do Future.
    //
    // BUG corrigido: antes, esse botão só olhava lastFetchedConfig (que só
    // vem preenchido depois que o MAC já respondeu ALGUMA config) e, pior,
    // o caminho de fallback (fetchDeviceConfig -> /api/device/check) usado
    // quando o MAC ainda não está cadastrado não traz test_api_url nenhum
    // -- então o teste NUNCA funcionava antes do cadastro. Pedido explícito
    // do usuário: o cliente precisa poder testar a API do painel ANTES de
    // cadastrar o MAC, pra conhecer o aplicativo. Agora busca direto na
    // rota pública /api/v5/apps/future/preview (não depende de MAC/cadastro
    // nenhum) e só cai pro último valor já recebido via fetchConfig se essa
    // rota nova falhar por algum motivo (painel ainda sem o endpoint, etc.).
    //
    // Pedido explícito adicional: esse teste precisa deixar o MAC + nome do
    // CLIENTE de verdade registrado no painel (não um texto fixo tipo "Seu
    // teste aqui") -- por isso agora pede nome e telefone/WhatsApp ANTES de
    // rodar o teste, e manda os dois pro nosso painel
    // (/api/v5/maximus-test-result, já aceita app_id="future"). O diagnóstico
    // de conectividade contra a "API do Servidor" continua rodando depois,
    // igual antes.
    private fun showServerApiTestDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(4))
        }
        val info = TextView(this).apply {
            text = "Informe seu nome e WhatsApp pra gerar o teste no painel."
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, 0, 0, dp(12))
        }
        val nameInput = EditText(this).apply {
            hint = "Seu nome"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        }
        val phoneInput = EditText(this).apply {
            hint = "WhatsApp (DDD + número)"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_PHONE
        }
        container.addView(info)
        container.addView(nameInput)
        container.addView(phoneInput)

        AlertDialog.Builder(this)
            .setTitle("Seu teste aqui")
            .setView(container)
            .setPositiveButton("Gerar teste") { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                if (name.isBlank() || phone.isBlank()) {
                    Toast.makeText(this, "Preencha nome e WhatsApp pra gerar o teste", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                runServerApiTest(name, phone)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun runServerApiTest(name: String, phone: String) {
        Toast.makeText(this, "Gerando teste no painel...", Toast.LENGTH_SHORT).show()
        // Registra MAC + nome + telefone no NOSSO painel primeiro -- é a
        // nossa própria rota, sempre funciona independente da API externa
        // abaixo, então o revendedor já vê esse teste no dashboard mesmo que
        // o diagnóstico de conectividade falhe.
        integration.reportMaximusTestResult(JSONObject().apply {
            put("mac", mac)
            put("name", name)
            put("phone", phone)
            put("source", "future")
            put("app_id", "future")
        })
        integration.fetchFutureTestApiUrl { previewResult ->
            runOnUiThread {
                val apiUrl = previewResult.getOrNull()?.takeIf { it.startsWith("http", true) }
                    ?: lastFetchedConfig?.testApiUrl?.trim()?.takeIf { it.startsWith("http", true) }
                    ?: ""
                if (apiUrl.isBlank()) {
                    AlertDialog.Builder(this)
                        .setTitle("Cadastro enviado")
                        .setMessage("$name foi registrado no painel com esse MAC. A API do Servidor ainda não foi configurada, então não deu pra gerar o teste.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@runOnUiThread
                }
                // Achado no código de verdade do Maximus (MacPanelClient.
                // registerTestDevice, repo MaximusPlayerNativeExact): o botão
                // "TESTE" não faz um simples ping -- ele faz POST {"mac": mac}
                // pra essa mesma "API do Servidor", que devolve dns/usuário/
                // senha de uma conta de teste JÁ PROVISIONADA, e o app carrega
                // o catálogo na hora com isso, sem esperar cadastro manual no
                // nosso painel. Reproduz exatamente esse contrato aqui.
                integration.requestPanelTrial(apiUrl, mac) { result ->
                    runOnUiThread {
                        result.onSuccess { json ->
                            val dns = json.optString("dns").trim()
                            val username = json.optString("username").trim()
                            val password = json.optString("password").trim()
                            if (dns.isBlank() || username.isBlank() || password.isBlank()) {
                                status.text = "Teste solicitado. Aguarde a liberação no painel e toque em VERIFICAR."
                                status.setTextColor(getColor(R.color.warning))
                                AlertDialog.Builder(this)
                                    .setTitle("Teste solicitado")
                                    .setMessage("$name foi registrado com esse MAC. O painel ainda não devolveu uma conta de teste pronta -- aguarde a liberação e toque em VERIFICAR.")
                                    .setPositiveButton("OK", null)
                                    .show()
                                return@onSuccess
                            }
                            val server = if (dns.startsWith("http", true)) dns.trimEnd('/') else "http://${dns.trimEnd('/')}"
                            val playlistUrl = "$server/get.php?username=${URLEncoder.encode(username, "UTF-8")}&password=${URLEncoder.encode(password, "UTF-8")}&type=m3u_plus&output=mpegts"
                            // Marca esse MAC como "em teste local válido até X" ANTES de
                            // abrir a MainActivity. Sem isso, ela chama fetchConfig(mac) no
                            // painel de verdade (nosso próprio painel, não a API do
                            // Servidor) pra decidir se libera a tela -- e como um teste
                            // recém-gerado ainda NÃO é um dispositivo "autorizado"/assinante
                            // de verdade lá, a resposta correta do painel é
                            // registered=false/allowed=false, o que fazia a MainActivity
                            // travar o próprio teste que acabou de ser liberado com o popup
                            // "Acesso indisponível" -- ver isTrialActiveFor() em MainActivity.
                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                .putLong(PREF_TRIAL_ACTIVE_UNTIL, resolveTrialExpiryMillis(json.optString("expiresAt").trim()))
                                .putString(PREF_TRIAL_MAC, mac)
                                .apply()
                            loadTrialPlaylistAndOpen(playlistUrl)
                        }.onFailure {
                            AlertDialog.Builder(this)
                                .setTitle("Não foi possível gerar o teste")
                                .setMessage("$name foi registrado no painel, mas a API do Servidor não respondeu: ${it.message ?: "erro desconhecido"}")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                }
            }
        }
    }

    /** Carrega o catálogo a partir da playlist de teste devolvida pelo painel
     * e abre o app -- mesmo caminho de sucesso já usado em verifyAccess(),
     * só que alimentado por uma conta de teste em vez da lista definitiva do
     * cliente cadastrado. */
    private fun loadTrialPlaylistAndOpen(playlistUrl: String) {
        setConnectionProgress(60, "Teste liberado! Carregando canais, filmes e séries...")
        status.text = "Teste liberado. Carregando conteúdo..."
        status.setTextColor(getColor(R.color.success))
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(PREF_IMPORT_IN_PROGRESS, true).apply()
        playlistRepository.loadIfChanged(
            listOf(playlistUrl),
            onProgress = { progress ->
                runOnUiThread {
                    setConnectionProgress(progress, if (progress >= 95) "Finalizando catálogo..." else "Organizando canais, filmes e séries...")
                }
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putInt(PREF_IMPORT_PROGRESS_PERCENT, progress).apply()
            },
            onCatalogReady = { stats ->
                runOnUiThread {
                    if (!mainOpened && stats.total > 0) {
                        setConnectionProgress(86, "Catálogo inicial pronto. Organizando o restante em segundo plano...")
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(PREF_ACCESS_ALLOWED, true).apply()
                        keepImporterAlive = true
                        openMainActivity(importInProgress = true)
                    }
                }
            },
            callback = { playlistResult ->
                runOnUiThread {
                    playlistResult.onSuccess {
                        setConnectionProgress(100, "Conectado. Em breve você terá em mãos o melhor conteúdo para assistir.")
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                            .putBoolean(PREF_ACCESS_ALLOWED, true)
                            .putBoolean(PREF_IMPORT_IN_PROGRESS, false)
                            .apply()
                        if (mainOpened) {
                            keepImporterAlive = false
                            playlistRepository.shutdown()
                            finish()
                        } else {
                            openMainActivity(importInProgress = false)
                        }
                    }.onFailure {
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(PREF_IMPORT_IN_PROGRESS, false).apply()
                        status.text = "Não foi possível carregar o teste: ${it.message.orEmpty()}"
                        status.setTextColor(getColor(R.color.warning))
                    }
                }
            },
        )
    }

    // Tenta ler a validade real do teste devolvida pela API do Servidor
    // (campo opcional "expiresAt", visto no contrato do Maximus). Se vier
    // vazio ou num formato que não bate com nenhum padrão comum, usa um
    // prazo padrão -- é só uma janela de tolerância local pra não travar o
    // teste com "Acesso indisponível" enquanto ele ainda deveria estar
    // valendo; o painel/API do Servidor continuam sendo a fonte de verdade
    // de quando o teste realmente expira de verdade.
    private fun resolveTrialExpiryMillis(expiresAtRaw: String): Long {
        val fallback = System.currentTimeMillis() + DEFAULT_TRIAL_DURATION_MS
        if (expiresAtRaw.isBlank()) return fallback
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(expiresAtRaw)?.time
            }.getOrNull()
            if (parsed != null && parsed > 0L) return parsed
        }
        return fallback
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun showExtraSettingsDialog() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_extra_settings, null)
        val dnsInput = view.findViewById<EditText>(R.id.extraDnsInput)
        val userInput = view.findViewById<EditText>(R.id.extraUserInput)
        val passwordInput = view.findViewById<EditText>(R.id.extraPasswordInput)
        val errorLabel = view.findViewById<TextView>(R.id.extraSettingsError)
        val cancelButton = view.findViewById<TextView>(R.id.extraCancelButton)
        val confirmButton = view.findViewById<TextView>(R.id.extraConfirmButton)
        val usePanelButton = view.findViewById<TextView>(R.id.extraUsePanelButton)

        dnsInput.setText(prefs.getString(PREF_MANUAL_DNS, ""))
        userInput.setText(prefs.getString(PREF_MANUAL_USER, ""))
        passwordInput.setText(prefs.getString(PREF_MANUAL_PASSWORD, ""))

        val currentlyManual = prefs.getString(PREF_SOURCE_MODE, SOURCE_PANEL) == SOURCE_MANUAL
        usePanelButton.visibility = if (currentlyManual) View.VISIBLE else View.GONE

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setView(view)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setOnShowListener {
            val displayWidth = resources.displayMetrics.widthPixels
            dialog.window?.setLayout(
                (displayWidth * 0.9f).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
        }

        cancelButton.setOnClickListener { dialog.dismiss() }

        usePanelButton.setOnClickListener {
            prefs.edit().putString(PREF_SOURCE_MODE, SOURCE_PANEL).apply()
            dialog.dismiss()
            Toast.makeText(this, "Usando o painel/MAC novamente", Toast.LENGTH_SHORT).show()
            verifyAccess(true)
        }

        confirmButton.setOnClickListener {
            val dns = normalizeDns(dnsInput.text?.toString().orEmpty())
            val user = userInput.text?.toString()?.trim().orEmpty()
            val password = passwordInput.text?.toString()?.trim().orEmpty()
            if (dns.isBlank() || user.isBlank() || password.isBlank()) {
                errorLabel.text = "Preencha DNS, usuário e senha."
                errorLabel.visibility = View.VISIBLE
                return@setOnClickListener
            }
            prefs.edit()
                .putString(PREF_SOURCE_MODE, SOURCE_MANUAL)
                .putString(PREF_MANUAL_DNS, dns)
                .putString(PREF_MANUAL_USER, user)
                .putString(PREF_MANUAL_PASSWORD, password)
                .apply()
            dialog.dismiss()
            connectManual(dns, user, password, showProgress = true)
        }

        dialog.show()
        dnsInput.requestFocus()
    }

    private fun normalizeDns(raw: String): String {
        var value = raw.trim().trimEnd('/')
        if (value.isBlank()) return ""
        if (!value.contains("://")) value = "http://$value"
        return value
    }

    private fun buildXtreamUrl(dns: String, user: String, password: String): String {
        val encodedUser = URLEncoder.encode(user, "UTF-8")
        val encodedPassword = URLEncoder.encode(password, "UTF-8")
        return "$dns/get.php?username=$encodedUser&password=$encodedPassword&type=m3u_plus&output=ts"
    }

    private fun connectManual(dns: String, user: String, password: String, showProgress: Boolean) {
        // Uma tentativa manual explícita sempre assume prioridade: não é bloqueada
        // por uma checagem de painel ainda em andamento (ex.: a verificação
        // automática que já dispara sozinha ao abrir o app).
        val myGeneration = ++connectionGeneration
        checking = true
        loadingPanelList = true
        status.text = if (showProgress) "Conectando com a configuração extra..." else "Conectando direto por DNS/usuário/senha..."
        status.setTextColor(getColor(R.color.text_secondary))
        setConnectionProgress(15, "Conectando direto por DNS/usuário/senha (sem depender do painel)...")
        verifyButton.isEnabled = false
        connectButton.isEnabled = false
        extraSettingsButton.isEnabled = false
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putBoolean(PREF_IMPORT_IN_PROGRESS, true)
            .apply()
        val manualUrl = buildXtreamUrl(dns, user, password)
        playlistRepository.loadIfChanged(
            listOf(manualUrl),
            onProgress = { progress ->
                runOnUiThread {
                    if (myGeneration != connectionGeneration) return@runOnUiThread
                    setConnectionProgress(progress, if (progress >= 95) "Finalizando catálogo..." else "Organizando canais, filmes e séries...")
                }
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putInt(PREF_IMPORT_PROGRESS_PERCENT, progress).apply()
            },
            onCatalogReady = { stats ->
                runOnUiThread {
                    if (myGeneration != connectionGeneration) return@runOnUiThread
                    if (!mainOpened && stats.total > 0) {
                        setConnectionProgress(86, "Catálogo inicial pronto. Organizando o restante em segundo plano...")
                        status.text = "Catálogo inicial pronto. Abrindo Future..."
                        status.setTextColor(getColor(R.color.success))
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                            .putBoolean(PREF_ACCESS_ALLOWED, true)
                            .apply()
                        keepImporterAlive = true
                        openMainActivity(importInProgress = true)
                    }
                }
            },
            callback = { playlistResult ->
                runOnUiThread {
                    if (myGeneration != connectionGeneration) return@runOnUiThread
                    checking = false
                    loadingPanelList = false
                    verifyButton.isEnabled = true
                    connectButton.isEnabled = true
                    extraSettingsButton.isEnabled = true
                    playlistResult.onSuccess { snapshot ->
                        if (snapshot.rejectedDuplicate > 0) {
                            Toast.makeText(
                                this@ActivationActivity,
                                "Importação: ${snapshot.seenTotal} itens no M3U, ${snapshot.totalCount} salvos, ${snapshot.rejectedDuplicate} rejeitados por chave duplicada.",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                        setConnectionProgress(100, "Conectado. Em breve você terá em mãos o melhor conteúdo para assistir.")
                        status.text = "Catálogo completo."
                        status.setTextColor(getColor(R.color.success))
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                            .putBoolean(PREF_ACCESS_ALLOWED, true)
                            .putBoolean(PREF_IMPORT_IN_PROGRESS, false)
                            .apply()
                        handler.removeCallbacks(periodicCheck)
                        if (mainOpened) {
                            keepImporterAlive = false
                            playlistRepository.shutdown()
                            finish()
                        } else {
                            openMainActivity(importInProgress = false)
                        }
                    }.onFailure {
                        if (mainOpened) {
                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                .putBoolean(PREF_IMPORT_IN_PROGRESS, false)
                                .apply()
                            keepImporterAlive = false
                            playlistRepository.shutdown()
                            finish()
                        } else {
                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                .putBoolean(PREF_IMPORT_IN_PROGRESS, false)
                                .apply()
                            val reason = it.message.orEmpty()
                            val message = explainFailure(reason)
                            setConnectionProgress(40, message)
                            status.text = message
                            status.setTextColor(getColor(R.color.warning))
                        }
                    }
                }
            },
        )
    }

    private fun verifyAccess(showProgress: Boolean) {
        val myGeneration = ++connectionGeneration
        checking = true
        if (showProgress) status.text = "Consultando o painel..."
        setConnectionProgress(10, "Conectando sua lista de filmes, séries e canais...")
        verifyButton.isEnabled = false
        connectButton.isEnabled = false
        integration.fetchConfig(mac) { result ->
            runOnUiThread {
                if (myGeneration != connectionGeneration) return@runOnUiThread
                checking = false
                verifyButton.isEnabled = true
                connectButton.isEnabled = true
                result.onSuccess { config ->
                    lastFetchedConfig = config
                    if (!config.registered || !config.allowed) {
                        setConnectionProgress(20, "Aguardando o cadastro deste MAC no painel...")
                        status.text = "Aguardando cadastro e liberação no painel..."
                        status.setTextColor(getColor(R.color.warning))
                        return@onSuccess
                    }
                    if (config.playlistUrls.isEmpty()) {
                        setConnectionProgress(30, "MAC liberado. Aguardando a lista cadastrada no painel...")
                        status.text = "MAC liberado, aguardando a lista cadastrada no painel..."
                        status.setTextColor(getColor(R.color.warning))
                        return@onSuccess
                    }
                    loadingPanelList = true
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        .putBoolean(PREF_IMPORT_IN_PROGRESS, true)
                        .apply()
                    setConnectionProgress(60, "Lista encontrada. Conectando sua lista de filmes, séries e canais...")
                    verifyButton.isEnabled = false
                    connectButton.isEnabled = false
                    status.text = "Lista do painel encontrada. Carregando canais, filmes e séries..."
                            playlistRepository.loadIfChanged(
                        config.playlistUrls,
                        onProgress = { progress ->
                            runOnUiThread {
                                if (myGeneration != connectionGeneration) return@runOnUiThread
                                setConnectionProgress(progress, if (progress >= 95) "Finalizando catálogo..." else "Organizando canais, filmes e séries...")
                            }
                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putInt(PREF_IMPORT_PROGRESS_PERCENT, progress).apply()
                        },
                        onCatalogReady = { stats ->
                            runOnUiThread {
                                if (myGeneration != connectionGeneration) return@runOnUiThread
                                if (!mainOpened && stats.total > 0) {
                                    // O primeiro lote já foi COMMITADO. A tela principal pode
                                    // consultar SQLite enquanto o restante da M3U continua.
                                    setConnectionProgress(86, "Catálogo inicial pronto. Organizando o restante em segundo plano...")
                                    status.text = "Catálogo inicial pronto. Abrindo Future..."
                                    status.setTextColor(getColor(R.color.success))
                                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                        .putBoolean(PREF_ACCESS_ALLOWED, true)
                                        .apply()
                                    keepImporterAlive = true
                                    openMainActivity(importInProgress = true)
                                }
                            }
                        },
                        callback = { playlistResult ->
                            runOnUiThread {
                                if (myGeneration != connectionGeneration) return@runOnUiThread
                                loadingPanelList = false
                                playlistResult.onSuccess {
                                    setConnectionProgress(100, "Conectado. Em breve você terá em mãos o melhor conteúdo para assistir.")
                                    status.text = "Catálogo completo."
                                    status.setTextColor(getColor(R.color.success))
                                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                        .putBoolean(PREF_ACCESS_ALLOWED, true)
                                        .putBoolean(PREF_IMPORT_IN_PROGRESS, false)
                                        .apply()
                                    handler.removeCallbacks(periodicCheck)
                                    if (mainOpened) {
                                        // A MainActivity já está visível; apenas liberar o
                                        // importador agora que seu trabalho terminou.
                                        keepImporterAlive = false
                                        playlistRepository.shutdown()
                                        finish()
                                    } else {
                                        openMainActivity(importInProgress = false)
                                    }
                                }.onFailure {
                                    if (mainOpened) {
                                        // O primeiro lote continua disponível para a MainActivity;
                                        // não devolver o usuário à tela de MAC por uma falha tardia.
                                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                            .putBoolean(PREF_IMPORT_IN_PROGRESS, false)
                                            .apply()
                                        keepImporterAlive = false
                                        playlistRepository.shutdown()
                                        finish()
                                    } else {
                                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                            .putBoolean(PREF_IMPORT_IN_PROGRESS, false)
                                            .apply()
                                        verifyButton.isEnabled = true
                                        connectButton.isEnabled = true
                                        val reason = it.message.orEmpty()
                                        val message = explainFailure(reason)
                                        setConnectionProgress(40, message)
                                        status.text = message
                                        status.setTextColor(getColor(R.color.warning))
                                    }
                                }
                            }
                        },
                    )
                }.onFailure {
                    setConnectionProgress(20, "O painel não respondeu. Tentando novamente automaticamente...")
                    status.text = "Não foi possível consultar o painel. Toque em CONECTAR para tentar novamente."
                    status.setTextColor(getColor(R.color.warning))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(periodicCheck)
        handler.postDelayed(periodicCheck, 5_000)
    }

    override fun onPause() {
        handler.removeCallbacks(periodicCheck)
        super.onPause()
    }

    private fun openMainActivity(importInProgress: Boolean) {
        if (mainOpened) return
        mainOpened = true
        handler.removeCallbacks(periodicCheck)
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_CATALOG_IMPORT_IN_PROGRESS, importInProgress)
        })
        // Sempre finaliza esta tela, mesmo com o import ainda em segundo plano:
        // keepImporterAlive (checado em onDestroy) garante que a importação
        // continua rodando de qualquer forma. Isso evita que o botão VOLTAR,
        // pressionado na tela principal antes do import 100% terminar, volte
        // para esta tela de ativação, que ficaria "por baixo" dela sem isso.
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        handler.removeCallbacks(clockTicker)
        if (!keepImporterAlive) playlistRepository.shutdown()
        integration.shutdown()
        super.onDestroy()
    }

    companion object {
        const val PREFS_NAME = "maximus_device_preferences"
        const val PREF_MAC_ADDRESS = "mac_address"
        const val PREF_IMPORT_IN_PROGRESS = "catalog_import_in_progress"
        const val PREF_IMPORT_PROGRESS_PERCENT = "catalog_import_progress_percent"
        private const val PREF_ACCESS_ALLOWED = "access_allowed"

        // Fonte alternativa (DNS/usuário/senha), independente do painel/MAC.
        const val PREF_SOURCE_MODE = "source_mode"
        const val PREF_MANUAL_DNS = "manual_dns"
        const val PREF_MANUAL_USER = "manual_user"
        const val PREF_MANUAL_PASSWORD = "manual_password"
        const val SOURCE_PANEL = "panel"
        const val SOURCE_MANUAL = "manual"

        // Controle do teste gerado por "SEU TESTE AQUI" (ver runServerApiTest):
        // guarda até quando o teste local vale e pra qual MAC foi liberado, pra
        // MainActivity saber que não deve travar com "Acesso indisponível"
        // quando o painel (corretamente) ainda não marca esse MAC como
        // assinante autorizado -- ver loadRemoteConfiguration/isTrialActiveFor.
        const val PREF_TRIAL_ACTIVE_UNTIL = "trial_active_until"
        const val PREF_TRIAL_MAC = "trial_mac"
        // Usado só quando a API do Servidor não devolve "expiresAt" -- janela
        // de tolerância local padrão pra um teste que não informou validade.
        private const val DEFAULT_TRIAL_DURATION_MS = 24L * 60 * 60 * 1000
    }
}

/**
 * BUG CRÍTICO corrigido: essa função NUNCA tentava o MAC real do aparelho --
 * sempre gerava um identificador falso (SHA-256 do ANDROID_ID). Isso batia
 * com o padrão usado no resto da família de apps (Evolux, Rencia/Supreme)
 * só por acidente/coincidência de formato, mas o valor mostrado/cadastrado
 * no painel NUNCA era o MAC de verdade do aparelho -- se o cliente já tinha
 * o MAC de verdade anotado (da caixa, de outro app, etc.), o painel jamais
 * ia bater com o que esse app mostrava. Agora tenta, nesta ordem: MAC da
 * WifiManager -> MAC de alguma NetworkInterface nomeada (wlan0/eth0/wifi0)
 * -> qualquer interface ativa não-loopback -> só por último, o fallback
 * estável derivado do ANDROID_ID (SHA-256), igual já era feito antes --
 * mesmo padrão usado em MacAddressProvider.getFixedMac() no resto da família.
 */
object DeviceIdentifier {
    private const val PLACEHOLDER_1 = "020000000000"
    private const val PLACEHOLDER_2 = "000000000000"

    fun resolve(context: Context): String {
        val realMac = readRealMac(context)
        if (realMac != null) return format(realMac)
        return format(stableFallback(context))
    }

    private fun readRealMac(context: Context): String? {
        readFromWifiManager(context)?.let { return it }
        readFromNamedInterface("wlan0")?.let { return it }
        readFromNamedInterface("wifi0")?.let { return it }
        readFromNamedInterface("eth0")?.let { return it }
        readFromAnyActiveInterface()?.let { return it }
        return null
    }

    private fun readFromWifiManager(context: Context): String? = runCatching {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        @Suppress("DEPRECATION")
        val mac = wifiManager.connectionInfo?.macAddress
        normalizeCandidate(mac)
    }.getOrNull()

    private fun readFromNamedInterface(name: String): String? = runCatching {
        val netInterface = NetworkInterface.getByName(name) ?: return null
        normalizeCandidate(macBytesToString(netInterface.hardwareAddress))
    }.getOrNull()

    private fun readFromAnyActiveInterface(): String? = runCatching {
        for (netInterface in Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (netInterface.isLoopback || !netInterface.isUp) continue
            val candidate = normalizeCandidate(macBytesToString(netInterface.hardwareAddress))
            if (candidate != null) return candidate
        }
        null
    }.getOrNull()

    private fun macBytesToString(bytes: ByteArray?): String? {
        if (bytes == null || bytes.size < 6) return null
        return bytes.joinToString("") { String.format(Locale.US, "%02X", it.toInt() and 0xFF) }
    }

    /** Filtra MACs placeholder/inválidos que emuladores e alguns aparelhos
     * devolvem (ex.: "02:00:00:00:00:00" da WifiManager quando sem permissão
     * de localização, ou tudo zero). */
    private fun normalizeCandidate(raw: String?): String? {
        val compact = raw?.filter { it.isLetterOrDigit() }?.uppercase() ?: return null
        if (compact.length != 12) return null
        if (compact == PLACEHOLDER_1 || compact == PLACEHOLDER_2) return null
        return compact
    }

    private fun stableFallback(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val digest = MessageDigest.getInstance("SHA-256").digest(androidId.toByteArray(Charsets.UTF_8))
        val bytes = digest.copyOf(6)
        // Bit "administrado localmente" ligado -- convenção pra deixar claro
        // que não é um MAC de fabricante de verdade, evitando colisão com
        // faixas reais.
        bytes[0] = ((bytes[0].toInt() and 0xFC) or 0x02).toByte()
        return bytes.joinToString("") { String.format(Locale.US, "%02X", it.toInt() and 0xFF) }
    }

    fun format(compact: String): String = compact.replace(":", "").chunked(2).joinToString(":")
}
