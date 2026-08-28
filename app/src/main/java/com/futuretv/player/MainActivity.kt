package com.futuretv.player

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private data class UserProfile(
    val id: String,
    val name: String,
    val avatar: Int,
)

private data class ChannelEditorial(
    val eyebrow: String,
    val description: String,
    val tags: String,
    val currentProgram: String,
    val currentDescription: String,
    val time: String,
    val nextProgram: String,
)

class MainActivity : Activity() {
    private enum class PreviewMode { NONE, TRAILER, CONTENT }
    private enum class PreviewScale(val label: String) { NORMAL("NORMAL"), STRETCH("ESTICAR"), ZOOM("ZOOM") }
    private enum class HomeSeriesCategory { DORAMAS, TURKISH_NOVELAS, NOVELAS, REELSHORTS, ANIMES }

    private val pageSize = 120
    private lateinit var channelList: RecyclerView
    private lateinit var videoPreview: FrameLayout
    private lateinit var previewScroll: ScrollView
    private lateinit var categoryList: LinearLayout
    private lateinit var sortRow: LinearLayout
    private lateinit var sortRecentButton: TextView
    private lateinit var sortAlphaButton: TextView
    private lateinit var sortRatingButton: TextView
    private lateinit var navItems: LinearLayout
    private lateinit var appLogo: ImageView
    private lateinit var remoteBackground: ImageView
    private lateinit var brandMark: TextView
    private lateinit var brandSubtitle: TextView
    private lateinit var searchHint: TextView
    private lateinit var liveHeader: TextView
    private lateinit var greeting: TextView
    private lateinit var importProgressBanner: LinearLayout
    private lateinit var importProgressText: TextView
    private lateinit var importProgressBar: ProgressBar
    private lateinit var channelHeading: TextView
    private lateinit var videoPreviewText: TextView
    private lateinit var previewLogo: ImageView
    private lateinit var heroImage: ImageView
    private lateinit var tvFrameOverlay: ImageView
    private lateinit var liveBadge: TextView
    private lateinit var detailEyebrow: TextView
    private lateinit var detailChannelName: TextView
    private lateinit var detailTags: TextView
    private lateinit var detailDescription: TextView
    private lateinit var nowCard: View
    private lateinit var nowLabel: TextView
    private lateinit var currentProgram: TextView
    private lateinit var currentProgramDescription: TextView
    private lateinit var epgUpcoming: LinearLayout
    private lateinit var programTime: TextView
    private lateinit var nextProgram: TextView
    private lateinit var actionRow: LinearLayout
    private lateinit var previewScaleButton: TextView
    private lateinit var vodSection: View
    private lateinit var vodCards: LinearLayout
    private lateinit var vodTitle: TextView
    private lateinit var homePanel: View
    private lateinit var homeOrbitRoot: FrameLayout
    private lateinit var homeSidebarTabs: HomeSidebarTabsView
    private lateinit var constellationTransition: ConstellationTransitionView
    private var constellationSound: MediaPlayer? = null
    private lateinit var homeUserHeader: View
    private lateinit var homeProfileImage: ImageView
    private lateinit var homeUserName: TextView
    private lateinit var internalShell: FrameLayout
    private lateinit var internalContent: LinearLayout
    private var internalShellInstalled = false
    private var cacheRestoreInFlight = false
    private var internalClockText: TextView? = null
    private var internalDateText: TextView? = null
    private lateinit var orbitLines: OrbitLinesView
    private lateinit var homeClockText: TextView
    private lateinit var homeDateText: TextView
    private var orbitCenterCard: FrameLayout? = null
    private var orbitBubbles: List<View> = emptyList()
    private var orbitSatelliteGroups: List<List<View>> = emptyList()
    private var orbitSatelliteParents: Map<View, View> = emptyMap()
    private val homeClockTicker = object : Runnable {
        override fun run() {
            updateHomeClock()
            if (homeMode) {
                advanceHomeRotation()
                mainHandler.postDelayed(this, 4_000)
            }
        }
    }
    private var homeRotationEntries: List<CatalogEntry> = emptyList()
    private var homeRotationIndex = 0
    private var homeMode = false
    private var settingsMode = false
    private var settingsPanel: View? = null
    private var homeFeaturedKey: String? = null
    private var exactHomeHotspots: List<View> = emptyList()
    private var exactHomeBadge: TextView? = null
    private var exactHomeProgramTitle: TextView? = null
    private var exactHomeProgramMeta: TextView? = null
    private var exactHomeChannelLogo: ImageView? = null
    private var exactHomeProgramEntry: CatalogEntry? = null
    private var homeRotationRequestId = 0
    private var activeProfileId = ""
    private var profileDialog: Dialog? = null
    private var activeProfileName = "SELECIONE UM PERFIL"
    private var activeProfileAvatar = 0
    private val profileAvatarAssets by lazy {
        listOf(
            R.drawable.profile_avatar_1, R.drawable.profile_avatar_2, R.drawable.profile_avatar_3, R.drawable.profile_avatar_4,
            R.drawable.profile_avatar_5, R.drawable.profile_avatar_6, R.drawable.profile_avatar_7, R.drawable.profile_avatar_8,
            R.drawable.profile_avatar_9, R.drawable.profile_avatar_10, R.drawable.profile_avatar_11, R.drawable.profile_avatar_12,
            R.drawable.profile_avatar_14, R.drawable.profile_avatar_15, R.drawable.profile_avatar_16, R.drawable.profile_avatar_17,
            R.drawable.profile_avatar_18, R.drawable.profile_avatar_19, R.drawable.profile_avatar_20, R.drawable.profile_avatar_21,
            R.drawable.profile_avatar_22, R.drawable.profile_avatar_23, R.drawable.profile_avatar_24, R.drawable.profile_avatar_25,
            R.drawable.profile_avatar_26, R.drawable.profile_avatar_27, R.drawable.profile_avatar_28, R.drawable.profile_avatar_29,
            R.drawable.profile_avatar_30, R.drawable.profile_avatar_31, R.drawable.profile_avatar_32, R.drawable.profile_avatar_34,
            R.drawable.profile_avatar_35,
            R.drawable.profile_kid_avatar_1, R.drawable.profile_kid_avatar_2, R.drawable.profile_kid_avatar_3,
            R.drawable.profile_kid_avatar_4, R.drawable.profile_kid_avatar_5, R.drawable.profile_kid_avatar_6,
            R.drawable.profile_kid_avatar_7,
        )
    }
    private var trailerFocusEntryKey: String? = null
    private var trailerFocusToken = 0
    private var miniPlayer: ExoPlayer? = null
    private var miniPlayerView: PlayerView? = null
    private var miniPlayerEntryKey: String? = null
    private var miniPlayerDialog: Dialog? = null
    private var miniTrailerView: WebView? = null
    private var radioVisualizer: RadioWaveView? = null
    private var previewMode = PreviewMode.NONE
    private var previewScale = PreviewScale.STRETCH
    private var seriesSeasonsDialog: Dialog? = null
    private var seriesEpisodesDialog: Dialog? = null

    private val repository by lazy { PlaylistRepository(this) }
    private val appIntegration = AppIntegrationRepository()
    private val radioRepository by lazy { RadioRepository(this) }
    private val imageLoader = ImageLoader()
    private val epgRepository = EpgRepository()
    private var epgByChannel: Map<String, List<EpgProgram>> = emptyMap()
    private lateinit var catalogAdapter: CatalogAdapter
    private var catalog = CatalogSnapshot(emptyList())
    private var databaseBackedCatalog = false
    private val pagedItems = ArrayList<CatalogEntry>()
    private var pageLoading = false
    private var pageFinished = false
    private var pageRequestId = 0
    private var categoryRequestId = 0
    private val categoryCache = mutableMapOf<MediaKind, List<String>>()
    private var selectedEntry: CatalogEntry? = null
    private val enrichedMetadata = mutableMapOf<String, CatalogMetadata>()
    private var selectedCategory = "Todos"
    private var query = ""
    private var favoritesOnly = false
    // Suprime o "selecionar primeiro item se não achar o selecionado atual na
    // página" de loadNextPage() -- usado quando estamos prestes a abrir um
    // item especifico (comando de voz, card de destaque da Home) logo depois
    // de trocar de seção, pra essa lógica de fallback não sobrescrever a
    // seleção um instante depois.
    private var suppressAutoSelectFirst = false
    private var ratingSortRequestId = 0
    private var currentKind = MediaKind.LIVE
    private var sortMode = SortMode.RECENT
    private var remoteBannerUrl = ""
    private var remoteEpgUrl = ""
    private var radioMode = false
    private var voiceMode = false
    private var radioDialog: Dialog? = null
    private var remoteConfig: RemoteAppConfig? = null
    private var radioEntries: List<CatalogEntry> = emptyList()
    private var focusCatalogWhenReady = false
    private var focusCategoryWhenReady = false
    private var parentalUnlocked = false
    private var catalogImportInProgress = false
    private var catalogImportWatcherStarted = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val catalogImportWatcher = object : Runnable {
        override fun run() {
            if (!catalogImportInProgress) return
            repository.loadCached { snapshot ->
                runOnUiThread {
                    val importPrefs = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE)
                    val stillLoading = importPrefs.getBoolean(ActivationActivity.PREF_IMPORT_IN_PROGRESS, true)
                    val percent = importPrefs.getInt(ActivationActivity.PREF_IMPORT_PROGRESS_PERCENT, 0)
                    updateImportProgressBanner(stillLoading, percent)
                    if (snapshot != null && snapshot.totalCount > 0 && snapshot.totalCount != catalog.totalCount) {
                        applyPartialCatalogSnapshot(snapshot, stillLoading)
                    }
                    catalogImportInProgress = stillLoading
                    if (stillLoading) mainHandler.postDelayed(this, 2_000)
                }
            }
        }
    }

    private fun updateImportProgressBanner(stillLoading: Boolean, percent: Int) {
        if (!stillLoading) {
            if (importProgressBanner.visibility != View.GONE) importProgressBanner.visibility = View.GONE
            return
        }
        importProgressBanner.visibility = View.VISIBLE
        importProgressBar.progress = percent.coerceIn(0, 100)
        importProgressText.text = when {
            percent <= 0 -> "⏳ Preparando seu conteúdo..."
            percent >= 95 -> "⏳ $percent%  •  Quase lá! Finalizando o catálogo..."
            else -> "⏳ $percent%  •  Seu conteúdo está sendo carregado. Já já você terá o melhor conteúdo!"
        }
    }

    private val editorials = mapOf(
        "animal planet" to ChannelEditorial(
            "Natureza e vida selvagem",
            "Documentários, expedições e histórias sobre animais, seus habitats e a relação entre as pessoas e o mundo natural.",
            "Animais   •   Natureza   •   Documentários",
            "Explorando a Selva",
            "Uma expedição acompanha espécies e paisagens selvagens em diferentes regiões do planeta.",
            "12:51 – 13:42",
            "A seguir  •  Predadores do Mundo  •  13:42",
        ),
        "cartoon network" to ChannelEditorial(
            "Desenhos e diversão",
            "Animações, aventuras e personagens para a família acompanhar ao longo do dia.",
            "Infantil   •   Animação   •   Família",
            "Aventuras no Mundo Colorido",
            "Uma turma de amigos descobre novas histórias em um universo cheio de imaginação.",
            "12:40 – 13:10",
            "A seguir  •  O Mundo de Greg  •  13:10",
        ),
        "discovery" to ChannelEditorial(
            "Ciência, aventura e descoberta",
            "Séries e documentários que exploram ciência, tecnologia, engenharia, aventura e os mistérios do mundo.",
            "Ciência   •   Aventura   •   Documentários",
            "Mestres da Engenharia",
            "Projetos impressionantes e as pessoas que transformam grandes ideias em realidade.",
            "12:30 – 13:30",
            "A seguir  •  Sobrevivência Extrema  •  13:30",
        ),
        "national geographic" to ChannelEditorial(
            "Conhecimento e exploração",
            "Produções sobre ciência, história, cultura, viagens e vida selvagem com imagens de diferentes lugares do planeta.",
            "Ciência   •   Viagens   •   Natureza",
            "Segredos do Oceano",
            "Uma jornada pelas profundezas do mar revela comportamentos e ambientes ainda pouco conhecidos.",
            "12:10 – 13:20",
            "A seguir  •  Grandes Civilizações  •  13:20",
        ),
        "espn" to ChannelEditorial(
            "Esportes e competição",
            "Eventos esportivos ao vivo, programas de debate, notícias e análises para acompanhar os principais campeonatos.",
            "Esportes   •   Ao vivo   •   Análises",
            "ESPN na Área",
            "Notícias, comentários e os principais destaques esportivos do dia.",
            "12:00 – 13:00",
            "A seguir  •  Linha de Passe  •  13:00",
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        setContentView(R.layout.activity_main)
        catalogImportInProgress = intent.getBooleanExtra(EXTRA_CATALOG_IMPORT_IN_PROGRESS, false)
        sortMode = runCatching {
            SortMode.valueOf(getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).getString(PREF_SORT_ALPHA, SortMode.RECENT.name) ?: SortMode.RECENT.name)
        }.getOrDefault(SortMode.RECENT)
        bindViews()
        installConstellationTransition()
        installInternalShell()
        setupCatalogList()
        showHome()
        loadRemoteConfiguration()
    }

    private fun installConstellationTransition() {
        val content = findViewById<FrameLayout>(android.R.id.content)
        constellationTransition = ConstellationTransitionView(this)
        content.addView(constellationTransition, FrameLayout.LayoutParams(-1, -1))
    }

    private fun playConstellationTransition() {
        if (transitionStarsEnabled() && ::constellationTransition.isInitialized) constellationTransition.play()
        if (!transitionSoundEnabled()) {
            constellationSound?.let { sound -> if (sound.isPlaying) sound.pause(); sound.seekTo(0) }
            return
        }
        runCatching {
            val player = constellationSound ?: MediaPlayer.create(this, R.raw.benkirb_shine_7_268909)?.also {
                it.setVolume(0.82f, 0.82f)
                it.setOnCompletionListener { completed -> completed.seekTo(0) }
            }?.also { constellationSound = it }
            player?.let {
                if (it.isPlaying) it.seekTo(0) else it.start()
            }
        }
    }

    private fun bindViews() {
        appLogo = findViewById(R.id.appLogo)
        remoteBackground = findViewById(R.id.remoteBackground)
        brandMark = findViewById(R.id.brandMark)
        brandSubtitle = findViewById(R.id.brandSubtitle)
        channelList = findViewById(R.id.channelList)
        videoPreview = findViewById(R.id.videoPreview)
        videoPreview.layoutParams = videoPreview.layoutParams.apply {
            height = (resources.displayMetrics.heightPixels * 0.42f).toInt()
        }
        previewScroll = findViewById(R.id.previewScroll)
        categoryList = findViewById(R.id.categoryList)
        sortRow = findViewById(R.id.sortRow)
        sortRecentButton = findViewById(R.id.sortRecentButton)
        sortAlphaButton = findViewById(R.id.sortAlphaButton)
        sortRatingButton = findViewById(R.id.sortRatingButton)
        sortRecentButton.setOnClickListener { applySortMode(SortMode.RECENT) }
        sortAlphaButton.setOnClickListener { applySortMode(SortMode.ALPHABETICAL) }
        sortRatingButton.setOnClickListener { applySortMode(SortMode.RATING) }
        paintSortButtons()
        navItems = findViewById(R.id.navItems)
        searchHint = findViewById(R.id.searchHint)
        liveHeader = findViewById(R.id.liveHeader)
        greeting = findViewById(R.id.greeting)
        importProgressBanner = findViewById(R.id.importProgressBanner)
        importProgressText = findViewById(R.id.importProgressText)
        importProgressBar = findViewById(R.id.importProgressBar)
        channelHeading = findViewById(R.id.channelHeading)
        videoPreviewText = findViewById(R.id.videoPreviewText)
        previewLogo = findViewById(R.id.previewLogo)
        heroImage = findViewById(R.id.heroImage)
        tvFrameOverlay = findViewById(R.id.tvFrameOverlay)
        tvFrameOverlay.visibility = View.GONE
        liveBadge = findViewById(R.id.liveBadge)
        detailEyebrow = findViewById(R.id.detailEyebrow)
        detailChannelName = findViewById(R.id.detailChannelName)
        detailTags = findViewById(R.id.detailTags)
        detailDescription = findViewById(R.id.detailDescription)
        nowCard = findViewById(R.id.nowCard)
        nowLabel = findViewById(R.id.nowLabel)
        currentProgram = findViewById(R.id.currentProgram)
        currentProgramDescription = findViewById(R.id.currentProgramDescription)
        epgUpcoming = findViewById(R.id.epgUpcoming)
        programTime = findViewById(R.id.programTime)
        nextProgram = findViewById(R.id.nextProgram)
        actionRow = findViewById(R.id.actionRow)
        previewScaleButton = findViewById(R.id.previewScaleButton)
        previewScaleButton.background = rounded(0xCC101827, 10f)
        previewScaleButton.isFocusable = true
        previewScaleButton.isClickable = true
        previewScaleButton.setOnClickListener { cyclePreviewScale() }
        previewScaleButton.setOnFocusChangeListener { view, hasFocus ->
            view.background = rounded(if (hasFocus) 0xFF2BFFB0 else 0xCC101827, 10f)
            (view as TextView).setTextColor(if (hasFocus) Color.rgb(5, 6, 10) else Color.WHITE)
        }
        listOf<View>(detailDescription, nowCard, nextProgram).forEach { view ->
            view.isFocusable = true
            view.isClickable = true
            view.setOnClickListener { }
        }
        vodSection = findViewById(R.id.vodSection)
        vodSection.visibility = View.GONE
        vodCards = findViewById(R.id.vodCards)
        vodTitle = findViewById(R.id.vodTitle)
        homePanel = findViewById(R.id.homePanel)
        homeOrbitRoot = findViewById(R.id.homeOrbitRoot)
        homeSidebarTabs = HomeSidebarTabsView(this).apply {
            val sidebarWidth = (resources.displayMetrics.widthPixels * 0.22f).toInt()
            layoutParams = FrameLayout.LayoutParams(sidebarWidth, -1, Gravity.START)
            setTabs(listOf(
                HomeSidebarTabsView.Tab("DORAMAS", R.drawable.home_sidebar_icon_doramas) { openHomeSeriesCategory(HomeSeriesCategory.DORAMAS) },
                HomeSidebarTabsView.Tab("NOVELAS TURCAS", R.drawable.home_sidebar_icon_turkish_novelas) { openHomeSeriesCategory(HomeSeriesCategory.TURKISH_NOVELAS) },
                HomeSidebarTabsView.Tab("NOVELAS", R.drawable.home_sidebar_icon_novelas) { openHomeSeriesCategory(HomeSeriesCategory.NOVELAS) },
                HomeSidebarTabsView.Tab("REELSHORTS", R.drawable.home_sidebar_icon_reelshorts) { openHomeSeriesCategory(HomeSeriesCategory.REELSHORTS) },
                HomeSidebarTabsView.Tab("ANIMES", R.drawable.home_sidebar_icon_animes) { openHomeSeriesCategory(HomeSeriesCategory.ANIMES) },
            ))
        }
        (homePanel as? ViewGroup)?.addView(homeSidebarTabs)
        homeUserHeader = findViewById(R.id.homeUserHeader)
        homeProfileImage = findViewById(R.id.homeProfileImage)
        homeUserName = findViewById(R.id.homeUserName)
        homeProfileImage.background = ovalBackground(0x553B6A9C)
        homeProfileImage.outlineProvider = ViewOutlineProvider.BACKGROUND
        homeProfileImage.clipToOutline = true
        homeUserHeader.isFocusable = true
        // O container só participa do foco do D-pad. O clique do perfil fica
        // restrito ao avatar, para nunca cobrir uma aba da sidebar.
        homeUserHeader.isClickable = false
        homeUserHeader.contentDescription = "Cabeçalho do perfil"
        homeProfileImage.isClickable = true
        homeProfileImage.isFocusable = true
        homeProfileImage.contentDescription = "Trocar perfil"
        homeProfileImage.setOnClickListener { showProfileDialog() }
        homeUserHeader.setOnFocusChangeListener { view, hasFocus ->
            view.alpha = if (hasFocus) 1f else 0.94f
            view.background = rounded(if (hasFocus) 0x553D79AA else 0x00000000, 18f)
        }
        homeUserHeader.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN &&
                (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A)
            ) {
                showProfileDialog()
                true
            } else {
                false
            }
        }
        loadActiveProfile()
        updateProfileHeader()
        orbitLines = findViewById(R.id.orbitLines)
        homeClockText = findViewById(R.id.homeClockText)
        homeDateText = findViewById(R.id.homeDateText)
        findViewById<View>(R.id.homeNavSearch).setOnClickListener { showSearchDialog() }
        searchHint.isFocusable = true
        searchHint.isClickable = true
        searchHint.setOnClickListener { showSearchDialog() }
        (categoryList.parent as? View)?.isFocusable = false
        (categoryList.parent as? ViewGroup)?.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        findViewById<View>(R.id.navScroll).isFocusable = false
        (findViewById<View>(R.id.navScroll) as? ViewGroup)?.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        previewScaleButton.visibility = View.GONE
        videoPreview.isFocusable = true
        videoPreview.isFocusableInTouchMode = true
        videoPreview.isClickable = true
        videoPreview.descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        previewScroll.isFocusable = false
        previewScroll.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        videoPreview.setOnClickListener { selectedEntry?.let { handleEntryClick(it) } }
    }

    private var dpadCenterLongPressHandled = false
    private val dpadCenterLongPressRunnable = Runnable {
        dpadCenterLongPressHandled = true
        handleDpadCenterLongPress()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_A,
            -> {
                // Trata pressão longa manualmente: o app processava o toque no
                // OK imediatamente ao pressionar (ACTION_DOWN), então o Android
                // nunca tinha chance de detectar "segurou" (isso depende de
                // observar o botão continuar pressionado até um tempo limite).
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) {
                            dpadCenterLongPressHandled = false
                            mainHandler.postDelayed(dpadCenterLongPressRunnable, android.view.ViewConfiguration.getLongPressTimeout().toLong())
                        }
                        return true
                    }
                    KeyEvent.ACTION_UP -> {
                        mainHandler.removeCallbacks(dpadCenterLongPressRunnable)
                        if (!dpadCenterLongPressHandled) activateDpadTarget()
                        return true
                    }
                }
            }
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                -> if (moveDpad(event.keyCode)) return true
                KeyEvent.KEYCODE_BACK -> {
                    onBackPressed()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun handleDpadCenterLongPress() {
        val focused = currentFocus ?: return
        if (isWithin(focused, channelList)) {
            selectedEntry?.let { quickToggleFavorite(it) }
        }
    }

    private fun activateDpadTarget(): Boolean {
        val focused = currentFocus ?: return false
        val sideNavigation = findViewById<View>(R.id.sideNavigation)
        val categoryParent = categoryList.parent as? View
        val target = when {
            navigationRowForFocus(focused) != null -> navigationRowForFocus(focused)
            focused === searchHint -> searchHint
            catalogRowForFocus(focused) != null -> catalogRowForFocus(focused)
            isWithin(focused, channelList) -> channelList.getChildAt(0)
            focused === categoryParent -> categoryList.getChildAt(0)
            isWithin(focused, categoryList) -> focused
            isWithin(focused, actionRow) -> {
                var current: View? = focused
                while (current != null && current.parent !== actionRow) current = current.parent as? View
                current ?: actionRow.getChildAt(0)
            }
            isWithin(focused, videoPreview) -> videoPreview
            focused === previewScroll -> videoPreview
            isWithin(focused, sideNavigation) -> focused.takeIf { it.isClickable }
            else -> focused.takeIf { it.isClickable }
        } ?: return false
        if (!target.isShown || !target.isEnabled || !target.isClickable) return false
        target.performClick()
        return true
    }

    private fun moveDpad(keyCode: Int): Boolean {
        val focused = currentFocus ?: return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> focusFirstCategory()
            KeyEvent.KEYCODE_DPAD_DOWN -> focusFirstCategory() || focusFirstCatalogItem()
            KeyEvent.KEYCODE_DPAD_LEFT -> true
            else -> false
        }
        if (homeMode) return moveHomeDpad(focused, keyCode)
        if (settingsMode) return moveSettingsDpad(focused, keyCode)

        if (focused === searchHint) {
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> focusNavigationForCurrentSection()
                KeyEvent.KEYCODE_DPAD_UP -> focusNavigationForCurrentSection()
                KeyEvent.KEYCODE_DPAD_DOWN -> focusFirstCategory()
                KeyEvent.KEYCODE_DPAD_RIGHT -> focusFirstCategory()
                else -> false
            }
        }
        val focusedNavRow = navigationRowForFocus(focused)
        if (focusedNavRow != null || isWithin(focused, findViewById(R.id.sideNavigation))) {
            val index = focusedNavRow?.let { navItems.indexOfChild(it) } ?: navItems.indexOfChild(
                (0 until navItems.childCount)
                    .map { navItems.getChildAt(it) }
                    .firstOrNull { isNavigationSelected(it.tag as? String ?: "") },
            ).coerceAtLeast(0)
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> if (focusedNavRow != null) focusNavigation(index - 1) else true
                KeyEvent.KEYCODE_DPAD_DOWN -> if (focusedNavRow != null) focusNavigation(index + 1) else focusNavigation(index)
                KeyEvent.KEYCODE_DPAD_RIGHT -> focusFirstCategory() || focusFirstCatalogItem() || focusPreview()
                KeyEvent.KEYCODE_DPAD_LEFT -> true
                else -> false
            }
        }
        if (focused === categoryList.parent) {
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> focusNavigationForCurrentSection()
                KeyEvent.KEYCODE_DPAD_RIGHT -> focusFirstCategory()
                KeyEvent.KEYCODE_DPAD_UP -> searchHint.requestFocus()
                KeyEvent.KEYCODE_DPAD_DOWN -> focusFirstSortButton() || focusFirstCatalogItem()
                else -> false
            }
        }
        if (isWithin(focused, categoryList)) {
            val index = categoryList.indexOfChild(focused)
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (selectedCategory == ContentSafety.LOCKED_CATEGORY) {
                        parentalUnlocked = false
                        selectedCategory = "Todos"
                        selectedEntry = null
                        clearPreviewForSection(currentKind)
                        renderCategories()
                        renderCatalog()
                    }
                    if (index <= 0) focusNavigationForCurrentSection() else focusCategoryAt(index - 1)
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> if (index >= categoryList.childCount - 1) focusFirstSortButton() || focusFirstCatalogItem() else focusCategoryAt(index + 1)
                KeyEvent.KEYCODE_DPAD_UP -> searchHint.requestFocus()
                KeyEvent.KEYCODE_DPAD_DOWN -> focusFirstSortButton() || focusFirstCatalogItem()
                else -> false
            }
        }
        if (isWithin(focused, sortRow)) {
            val index = sortRow.indexOfChild(focused)
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> if (index <= 1) focusFirstCategory() else sortRow.getChildAt(index - 1).requestFocus()
                KeyEvent.KEYCODE_DPAD_RIGHT -> if (index >= sortRow.childCount - 1) focusFirstCatalogItem() else sortRow.getChildAt(index + 1).requestFocus()
                KeyEvent.KEYCODE_DPAD_UP -> focusFirstCategory()
                KeyEvent.KEYCODE_DPAD_DOWN -> focusFirstCatalogItem()
                else -> false
            }
        }
        if (isWithin(focused, channelList)) {
            val row = catalogRowForFocus(focused)
            val position = row?.let { channelList.getChildAdapterPosition(it) } ?: RecyclerView.NO_POSITION
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> focusFirstCategory() || focusNavigationForCurrentSection()
                KeyEvent.KEYCODE_DPAD_RIGHT -> focusPreview() || focusFirstAction()
                KeyEvent.KEYCODE_DPAD_UP -> if (position <= 0) focusFirstSortButton() || focusFirstCategory() || searchHint.requestFocus() else moveCatalogFocus(-1)
                KeyEvent.KEYCODE_DPAD_DOWN -> moveCatalogFocus(1)
                else -> false
            }
        }
        if (isWithin(focused, previewScroll)) {
            if (isWithin(focused, nowCard)) {
                return when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> videoPreview.requestFocus()
                    KeyEvent.KEYCODE_DPAD_UP -> videoPreview.requestFocus()
                    KeyEvent.KEYCODE_DPAD_RIGHT -> true
                    KeyEvent.KEYCODE_DPAD_DOWN -> focusNextProgram() || focusFirstAction() || true
                    else -> false
                }
            }
            if (isWithin(focused, nextProgram)) {
                return when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> nowCard.requestFocus()
                    KeyEvent.KEYCODE_DPAD_UP -> nowCard.requestFocus()
                    KeyEvent.KEYCODE_DPAD_RIGHT -> true
                    KeyEvent.KEYCODE_DPAD_DOWN -> focusFirstAction() || true
                    else -> false
                }
            }
            if (focused === detailDescription) {
                return when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> focusLastAction() || videoPreview.requestFocus()
                    KeyEvent.KEYCODE_DPAD_UP -> focusLastAction() || videoPreview.requestFocus()
                    KeyEvent.KEYCODE_DPAD_RIGHT -> true
                    KeyEvent.KEYCODE_DPAD_DOWN -> true
                    else -> false
                }
            }
            if (isWithin(focused, videoPreview)) {
                return when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> focusSelectedCatalogItem() || focusFirstCategory()
                    KeyEvent.KEYCODE_DPAD_RIGHT -> focusProgrammingArea() || focusFirstAction()
                    KeyEvent.KEYCODE_DPAD_UP -> searchHint.requestFocus()
                    KeyEvent.KEYCODE_DPAD_DOWN -> focusProgrammingArea() || focusFirstAction() || previewScaleButton.requestFocus()
                    else -> false
                }
            }
            if (isWithin(focused, actionRow)) {
                val actionView = actionRow.indexOfChild(focused).takeIf { it >= 0 }
                return when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> focusPreview()
                    KeyEvent.KEYCODE_DPAD_UP -> focusProgrammingArea() || videoPreview.requestFocus()
                    KeyEvent.KEYCODE_DPAD_RIGHT -> focusPreview() || true
                    KeyEvent.KEYCODE_DPAD_DOWN -> actionView?.let { focusAction(it + 1) } ?: false || detailDescription.requestFocus() || true
                    else -> false
                }
            }
            // Fallback for the ScrollView/preview container itself. Android TV can
            // focus the container when a dynamic child is recreated; never let the
            // default geometric algorithm send the user back to the sidebar.
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> focusSelectedCatalogItem() || focusFirstCategory() || true
                KeyEvent.KEYCODE_DPAD_RIGHT -> focusProgrammingArea() || focusFirstAction() || true
                KeyEvent.KEYCODE_DPAD_UP -> searchHint.requestFocus() || true
                KeyEvent.KEYCODE_DPAD_DOWN -> focusProgrammingArea() || focusFirstAction() || true
                else -> false
            }
        }
        if (isWithin(focused, findViewById(R.id.channelColumn))) {
            // A column/header/RecyclerView wrapper may briefly own focus while
            // rows are being rebound. Resolve directions to the content region.
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> focusNavigationForCurrentSection() || true
                KeyEvent.KEYCODE_DPAD_RIGHT -> focusFirstCategory() || true
                KeyEvent.KEYCODE_DPAD_UP -> searchHint.requestFocus() || true
                KeyEvent.KEYCODE_DPAD_DOWN -> focusFirstCategory() || focusFirstCatalogItem() || true
                else -> false
            }
        }
        return false
    }

    private fun moveSettingsDpad(focused: View, keyCode: Int): Boolean {
        val panel = settingsPanel
        val navRow = navigationRowForFocus(focused)
        if (navRow != null || isWithin(focused, findViewById(R.id.sideNavigation))) {
            val index = navRow?.let { navItems.indexOfChild(it) } ?: (0 until navItems.childCount)
                .firstOrNull { isNavigationSelected(navItems.getChildAt(it).tag as? String ?: "") }
                ?.coerceAtLeast(0)
                ?: 0
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> if (navRow != null) focusNavigation(index - 1) else true
                KeyEvent.KEYCODE_DPAD_DOWN -> if (navRow != null) focusNavigation(index + 1) else focusNavigation(index)
                KeyEvent.KEYCODE_DPAD_RIGHT -> focusFirstSettingsOption()
                KeyEvent.KEYCODE_DPAD_LEFT -> true
                else -> false
            }
        }
        if (panel != null && isWithin(focused, panel)) {
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> focusNavigationForCurrentSection()
                KeyEvent.KEYCODE_DPAD_RIGHT -> true
                else -> false
            }
        }
        return false
    }

    private fun moveHomeDpad(focused: View, keyCode: Int): Boolean {
        if (::homeSidebarTabs.isInitialized && homeSidebarTabs.ownsFocus(focused)) {
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (homeSidebarTabs.focusedTabIndex() == 0) homeUserHeader.requestFocus() else homeSidebarTabs.moveFocus(-1)
                    true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> homeSidebarTabs.moveFocus(1)
                KeyEvent.KEYCODE_DPAD_RIGHT -> exactHomeHotspots.firstOrNull()?.requestFocus() ?: true
                KeyEvent.KEYCODE_DPAD_LEFT -> true
                else -> false
            }
        }
        if (focused === homeUserHeader) {
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (::homeSidebarTabs.isInitialized && homeSidebarTabs.firstTabId() != View.NO_ID) homeSidebarTabs.focusFirstTab() else exactHomeHotspots.firstOrNull()?.requestFocus()
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP -> true
                else -> false
            }
        }
        if (focused in exactHomeHotspots) return moveExactHomeHotspot(focused, keyCode)
        val center = orbitCenterCard
        val bubbles = orbitBubbles
        if (focused === center) {
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> focusNavigationForCurrentSection()
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> bubbles.firstOrNull()?.requestFocus() ?: true
                else -> false
            }
        }
        if (focused in bubbles) {
            val index = bubbles.indexOf(focused)
            val satellites = orbitSatelliteGroups.getOrNull(index).orEmpty()
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> if (index == 0) focusNavigationForCurrentSection() else bubbles[(index - 1 + bubbles.size) % bubbles.size].requestFocus()
                KeyEvent.KEYCODE_DPAD_RIGHT -> satellites.firstOrNull()?.requestFocus() ?: bubbles[(index + 1) % bubbles.size].requestFocus()
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> center?.requestFocus() ?: true
                else -> false
            }
        }
        val parent = orbitSatelliteParents[focused]
        if (parent != null) {
            val group = orbitSatelliteGroups.firstOrNull { parent in it || it.isNotEmpty() && orbitSatelliteParents[it.first()] === parent }.orEmpty()
            val index = group.indexOf(focused)
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> parent.requestFocus()
                KeyEvent.KEYCODE_DPAD_RIGHT -> if (index >= 0 && group.isNotEmpty()) group[(index + 1) % group.size].requestFocus() else parent.requestFocus()
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> parent.requestFocus()
                else -> false
            }
        }
        return false
    }

    private fun moveExactHomeHotspot(focused: View, keyCode: Int): Boolean {
        val source = focused.layoutParams as? FrameLayout.LayoutParams ?: return false
        val sourceX = source.leftMargin + focused.width / 2f
        val sourceY = source.topMargin + focused.height / 2f
        val candidates = exactHomeHotspots.filter { it !== focused && it.isShown && it.isEnabled }
        val target = candidates.mapNotNull { candidate ->
            val params = candidate.layoutParams as? FrameLayout.LayoutParams ?: return@mapNotNull null
            val dx = params.leftMargin + candidate.width / 2f - sourceX
            val dy = params.topMargin + candidate.height / 2f - sourceY
            val primary: Float
            val secondary: Float
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (dx >= -dp(8)) return@mapNotNull null
                    primary = -dx
                    secondary = kotlin.math.abs(dy)
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (dx <= dp(8)) return@mapNotNull null
                    primary = dx
                    secondary = kotlin.math.abs(dy)
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (dy >= -dp(8)) return@mapNotNull null
                    primary = -dy
                    secondary = kotlin.math.abs(dx)
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (dy <= dp(8)) return@mapNotNull null
                    primary = dy
                    secondary = kotlin.math.abs(dx)
                }
                else -> return@mapNotNull null
            }
            candidate to (primary + secondary * 0.42f)
        }.minByOrNull { it.second }?.first
        if (target != null) {
            target.requestFocus()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP && sourceY <= homeUserHeader.height + dp(80)) {
            homeUserHeader.requestFocus()
            return true
        }
        // Consome a direção nas bordas para não deixar o Android saltar para
        // uma view invisível ou para fora da composição orbital.
        return true
    }

    private fun focusNavigation(index: Int): Boolean {
        if (navItems.childCount == 0) return false
        val targetIndex = index.coerceIn(0, navItems.childCount - 1)
        val target = navItems.getChildAt(targetIndex)
        target.isFocusable = true
        val focused = target.requestFocus()
        if (!focused) target.post { target.requestFocus() }
        return true
    }

    private fun navigationRowForFocus(view: View?): View? {
        var current = view
        while (current != null && current.parent !== navItems) {
            current = current.parent as? View
        }
        return current?.takeIf { it.parent === navItems }
    }

    private fun focusNavigationForCurrentSection(): Boolean {
        val target = (0 until navItems.childCount)
            .map { navItems.getChildAt(it) }
            .firstOrNull { isNavigationSelected(it.tag as? String ?: "") }
            ?: navItems.getChildAt(0)
            ?: return false
        target.isFocusable = true
        val focused = target.requestFocus()
        if (!focused) target.post { target.requestFocus() }
        updateNavigationVisuals(target)
        return true
    }

    private fun focusFirstCategory(): Boolean {
        if (!categoryList.isShown || (categoryList.parent as? View)?.isShown != true) return false
        if (categoryList.childCount == 0) {
            focusCategoryWhenReady = true
            return true
        }
        return focusCategoryAt(0)
    }

    private fun focusFirstSortButton(): Boolean {
        if (!sortRow.isShown) return false
        sortRecentButton.isFocusable = true
        val focused = sortRecentButton.requestFocus()
        if (!focused) sortRecentButton.post { sortRecentButton.requestFocus() }
        return focused
    }

    private fun focusCategoryAt(index: Int): Boolean {
        val target = categoryList.getChildAt(index.coerceIn(0, (categoryList.childCount - 1).coerceAtLeast(0))) ?: return false
        target.isFocusable = true
        val focused = target.requestFocus()
        if (!focused) target.post { target.requestFocus() }
        categoryList.parent?.let { parent ->
            if (parent is HorizontalScrollView) parent.smoothScrollTo(target.left, 0)
        }
        // Report whether focus actually moved, so callers chaining fallbacks with
        // `||` (e.g. DPAD_RIGHT from the sidebar) don't get silently swallowed
        // when requestFocus() fails and only the deferred retry above succeeds.
        return focused
    }

    private fun focusFirstCatalogItem(): Boolean {
        val item = channelList.getChildAt(0)
        if (item == null) {
            focusCatalogWhenReady = true
            channelList.postDelayed({
                if (focusCatalogWhenReady && channelList.childCount > 0) {
                    focusCatalogWhenReady = false
                    focusFirstCatalogItem()
                }
            }, 120L)
            return true
        }
        item.isFocusable = true
        val focused = item.requestFocus()
        if (!focused) item.post { item.requestFocus() }
        channelList.smoothScrollToPosition(0)
        // Same reasoning as focusCategoryAt(): only claim success when requestFocus()
        // actually moved focus, otherwise `||` fallback chains stop dead here.
        return focused
    }

    private fun moveCatalogFocus(delta: Int): Boolean {
        val focused = currentFocus ?: return false
        val row = catalogRowForFocus(focused) ?: return focusFirstCatalogItem()

        val position = channelList.getChildAdapterPosition(row)
        if (position == RecyclerView.NO_POSITION) return false
        val targetPosition = position + delta
        if (targetPosition < 0) return focusFirstCategory() || searchHint.requestFocus()
        if (targetPosition >= catalogAdapter.itemCount) return focusFirstAction() || true
        val attached = channelList.findViewHolderForAdapterPosition(targetPosition)?.itemView
        if (attached != null) {
            attached.isFocusable = true
            if (!attached.requestFocus()) attached.post { attached.requestFocus() }
            return true
        }
        channelList.smoothScrollToPosition(targetPosition)
        channelList.postDelayed({
            channelList.findViewHolderForAdapterPosition(targetPosition)?.itemView?.requestFocus()
        }, 120L)
        return true
    }

    private fun focusSelectedCatalogItem(): Boolean {
        val key = selectedEntry?.key ?: return false
        val position = catalogAdapter.positionOf(key)
        if (position < 0) return false
        val attached = channelList.findViewHolderForAdapterPosition(position)?.itemView
        if (attached != null) {
            attached.isFocusable = true
            return attached.requestFocus()
        }
        // O item existe na lista mas está fora da tela agora -- rola até ele
        // sem voltar pro início, e foca assim que a view for criada.
        channelList.scrollToPosition(position)
        channelList.postDelayed({
            channelList.findViewHolderForAdapterPosition(position)?.itemView?.let {
                it.isFocusable = true
                it.requestFocus()
            }
        }, 120L)
        return true
    }

    private fun catalogRowForFocus(view: View): View? {
        var current: View? = view
        while (current != null && current !== channelList) {
            if (current.parent === channelList) return current
            current = current.parent as? View
        }
        return null
    }

    private fun focusPreview(): Boolean {
        if (previewScroll.visibility != View.VISIBLE || !videoPreview.isShown) return false
        videoPreview.isFocusable = true
        videoPreview.isFocusableInTouchMode = true
        val focused = videoPreview.requestFocus()
        if (!focused) videoPreview.post { videoPreview.requestFocus() }
        return focused
    }

    private fun configureExplicitFocusGraph() {
        fun ensureFocusId(view: View?): Int? {
            if (view == null) return null
            if (view.id == View.NO_ID) view.id = View.generateViewId()
            return view.id
        }
        fun link(source: View?, left: View? = null, right: View? = null, up: View? = null, down: View? = null) {
            val sourceId = ensureFocusId(source) ?: return
            ensureFocusId(left)?.let { source?.nextFocusLeftId = it }
            ensureFocusId(right)?.let { source?.nextFocusRightId = it }
            ensureFocusId(up)?.let { source?.nextFocusUpId = it }
            ensureFocusId(down)?.let { source?.nextFocusDownId = it }
        }

        val selectedNav = (0 until navItems.childCount)
            .map { navItems.getChildAt(it) }
            .firstOrNull { isNavigationSelected(it.tag as? String ?: "") }
            ?: navItems.getChildAt(0)
        val firstCategory = categoryList.getChildAt(0)
        val lastCategory = categoryList.getChildAt(categoryList.childCount - 1)
        val firstSort = sortRecentButton
        val lastSort = sortRatingButton
        val firstCatalog = channelList.getChildAt(0)
        val firstAction = actionRow.getChildAt(0)
        val lastAction = actionRow.getChildAt(actionRow.childCount - 1)
        val programmingTarget = when {
            nowCard.visibility == View.VISIBLE && nowCard.isShown -> nowCard
            nextProgram.visibility == View.VISIBLE && nextProgram.text.isNotBlank() -> nextProgram
            detailDescription.visibility == View.VISIBLE && detailDescription.text.isNotBlank() -> detailDescription
            else -> null
        }

        for (index in 0 until navItems.childCount) {
            val row = navItems.getChildAt(index)
            link(
                row,
                left = if (index > 0) navItems.getChildAt(index - 1) else null,
                right = firstCategory,
                up = if (index > 0) navItems.getChildAt(index - 1) else row,
                down = if (index < navItems.childCount - 1) navItems.getChildAt(index + 1) else row,
            )
        }
        for (index in 0 until categoryList.childCount) {
            val category = categoryList.getChildAt(index)
            link(
                category,
                left = if (index > 0) categoryList.getChildAt(index - 1) else selectedNav,
                right = if (index < categoryList.childCount - 1) categoryList.getChildAt(index + 1) else firstSort,
                up = searchHint,
                down = firstSort,
            )
        }
        for (index in 1 until sortRow.childCount) {
            val sortButton = sortRow.getChildAt(index)
            link(
                sortButton,
                left = if (index > 1) sortRow.getChildAt(index - 1) else firstCategory,
                right = if (index < sortRow.childCount - 1) sortRow.getChildAt(index + 1) else firstCatalog,
                up = firstCategory,
                down = firstCatalog,
            )
        }
        val catalogColumns = (channelList.layoutManager as? androidx.recyclerview.widget.GridLayoutManager)?.spanCount ?: 1
        for (index in 0 until channelList.childCount) {
            val row = channelList.getChildAt(index)
            val hasLeftCard = catalogColumns > 1 && index % catalogColumns > 0
            val hasRightCard = catalogColumns > 1 && index % catalogColumns < catalogColumns - 1 && index + 1 < channelList.childCount
            link(
                row,
                left = if (hasLeftCard) channelList.getChildAt(index - 1) else firstCategory,
                right = if (hasRightCard) channelList.getChildAt(index + 1) else videoPreview,
                up = if (index >= catalogColumns) channelList.getChildAt(index - catalogColumns) else firstSort,
                down = if (index + catalogColumns < channelList.childCount) channelList.getChildAt(index + catalogColumns) else firstAction,
            )
        }
        link(videoPreview, left = firstCatalog ?: firstCategory, right = firstAction, up = searchHint, down = firstAction)
        for (index in 0 until actionRow.childCount) {
            val action = actionRow.getChildAt(index)
            link(
                action,
                left = videoPreview,
                right = programmingTarget,
                up = if (index > 0) actionRow.getChildAt(index - 1) else videoPreview,
                down = if (index < actionRow.childCount - 1) actionRow.getChildAt(index + 1) else programmingTarget,
            )
        }
        link(detailDescription, left = lastAction, right = programmingTarget, up = lastAction ?: videoPreview, down = programmingTarget)
        link(nowCard, left = lastAction, right = programmingTarget, up = lastAction ?: videoPreview, down = nextProgram ?: detailDescription)
        link(nextProgram, left = lastAction, right = programmingTarget, up = nowCard ?: lastAction, down = nextProgram)
    }

    private fun focusFirstAction(): Boolean {
        val target = actionRow.getChildAt(0)?.takeIf { it.visibility == View.VISIBLE && it.isShown } ?: return false
        target.isFocusable = true
        val focused = target.requestFocus()
        if (!focused) target.post { target.requestFocus() }
        return true
    }

    private fun focusLastAction(): Boolean {
        for (index in actionRow.childCount - 1 downTo 0) {
            if (focusAction(index)) return true
        }
        return false
    }

    private fun focusAction(index: Int): Boolean {
        val target = actionRow.getChildAt(index)?.takeIf { it.visibility == View.VISIBLE && it.isShown } ?: return false
        target.isFocusable = true
        val focused = target.requestFocus()
        if (!focused) target.post { target.requestFocus() }
        return true
    }

    private fun focusProgrammingArea(): Boolean {
        val target = when {
            nowCard.visibility == View.VISIBLE && nowCard.isShown -> nowCard
            nextProgram.visibility == View.VISIBLE && nextProgram.text.isNotBlank() -> nextProgram
            detailDescription.visibility == View.VISIBLE && detailDescription.text.isNotBlank() -> detailDescription
            else -> null
        } ?: return false
        val focused = target.requestFocus()
        if (focused) previewScroll.post { previewScroll.smoothScrollTo(0, target.top.coerceAtLeast(0)) }
        return focused
    }

    private fun focusNextProgram(): Boolean {
        if (nextProgram.visibility == View.VISIBLE && nextProgram.text.isNotBlank()) return nextProgram.requestFocus()
        return false
    }

    private fun isWithin(view: View?, parent: View): Boolean {
        var current = view
        while (current != null) {
            if (current === parent) return true
            current = current.parent as? View
        }
        return false
    }

    override fun onBackPressed() {
        if (seriesEpisodesDialog?.isShowing == true || seriesSeasonsDialog?.isShowing == true || radioDialog?.isShowing == true) {
            super.onBackPressed()
            return
        }
        if (miniPlayer != null || miniTrailerView != null) {
            stopMiniPlayer()
            return
        }
        if (!homeMode) {
            showHome()
            return
        }
        super.onBackPressed()
    }

    private fun cyclePreviewScale() {
        previewScale = when (previewScale) {
            PreviewScale.NORMAL -> PreviewScale.STRETCH
            PreviewScale.STRETCH -> PreviewScale.ZOOM
            PreviewScale.ZOOM -> PreviewScale.NORMAL
        }
        applyPreviewScale()
    }

    private fun applyPreviewScale() {
        val content = (miniPlayerView as View?) ?: miniTrailerView ?: return
        previewScaleButton.text = "MODO: ${previewScale.label}"
        when (previewScale) {
            PreviewScale.NORMAL -> {
                content.scaleX = 1f
                content.scaleY = 1f
                if (content is PlayerView) content.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                if (content is WebView) applyYoutubeVideoScale(content, "contain", 1f)
            }
            PreviewScale.STRETCH -> {
                content.scaleX = 1f
                content.scaleY = 1f
                if (content is PlayerView) content.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                if (content is WebView) applyYoutubeVideoScale(content, "fill", 1f)
            }
            PreviewScale.ZOOM -> {
                if (content is PlayerView) content.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                content.pivotX = content.width / 2f
                content.pivotY = content.height / 2f
                content.scaleX = 1.18f
                content.scaleY = 1.18f
                if (content is WebView) applyYoutubeVideoScale(content, "cover", 1.18f)
            }
        }
    }

    private fun showPreviewScaleControl() {
        previewScaleButton.visibility = View.VISIBLE
        previewScaleButton.text = "MODO: ${previewScale.label}"
        applyPreviewScale()
    }

    /**
     * A versão anterior mantinha um LinearLayout legado no XML e apenas
     * redesenhava partes dele. Isso deixava a tela de Séries exatamente como
     * a captura enviada: a coluna velha e o preview velho continuavam sendo
     * a hierarquia efetivamente exibida.
     *
     * Aqui os três componentes de dados existentes são retirados da árvore
     * antiga e reparentados uma única vez dentro do shell FUTURE. Os
     * repositórios, adapter, EPG e PlayerActivity continuam sendo reutilizados;
     * somente a estrutura visual passa a ser outra.
     */
    private fun installInternalShell() {
        if (internalShellInstalled) return
        val root = findViewById<FrameLayout>(R.id.rootShell)
        // Capturar as views enquanto ainda estão na árvore XML. Depois do
        // removeView(), Activity.findViewById() não consegue mais encontrá-las.
        val sidebar = findViewById<LinearLayout>(R.id.sideNavigation)
        val catalogColumn = findViewById<LinearLayout>(R.id.channelColumn)
        val detailsScroll = findViewById<ScrollView>(R.id.previewScroll)
        val legacyHeader = findViewById<View>(R.id.liveHeaderRow)
        val legacyHeading = findViewById<View>(R.id.channelHeadingRow)
        val legacyImport = findViewById<View>(R.id.importProgressBanner)
        val sortRowView = findViewById<LinearLayout>(R.id.sortRow)
        val detailsContent = findViewById<LinearLayout>(R.id.previewContent)
        val detailsContainer = findViewById<LinearLayout>(R.id.channelDetails)
        val panel = videoPreview
        val legacyOverlay = tvFrameOverlay
        val homeIndex = root.indexOfChild(homePanel).coerceAtLeast(0)
        val legacyRow = sidebar.parent as? ViewGroup ?: return

        legacyRow.removeView(sidebar)
        legacyRow.removeView(catalogColumn)
        legacyRow.removeView(detailsScroll)
        root.removeView(legacyRow)

        internalShell = FrameLayout(this).apply {
            id = View.generateViewId()
            setBackgroundColor(Color.TRANSPARENT)
            clipChildren = false
            clipToPadding = false
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        root.addView(internalShell, homeIndex.coerceAtMost(root.childCount))

        sidebar.apply {
            layoutParams = FrameLayout.LayoutParams(dp(92), -1).apply { gravity = Gravity.START }
            // A curva é desenhada por SidebarGlassDrawable; os ícones e
            // textos abaixo permanecem views reais para o destaque acompanhar
            // a aba clicada, sem usar um print como navegação.
            background = SidebarGlassDrawable()
            elevation = dp(12).toFloat()
            setPadding(dp(2), dp(8), dp(2), dp(8))
        }
        internalShell.addView(sidebar)

        internalContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xA6050B19.toInt())
            layoutParams = FrameLayout.LayoutParams(-1, -1).apply { leftMargin = dp(92) }
            clipChildren = false
            clipToPadding = false
        }
        internalShell.addView(internalContent)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(16), dp(28), dp(12))
            layoutParams = LinearLayout.LayoutParams(-1, dp(108))
        }
        val profile = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
        }
        val avatar = ImageView(this).apply {
            setImageResource(R.drawable.future_logo_safe)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = ovalBackground(0x443B6A9C)
            layoutParams = LinearLayout.LayoutParams(dp(66), dp(66)).apply { marginEnd = dp(12) }
            contentDescription = "Perfil FUTURE"
        }
        val profileText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        profileText.addView(TextView(this).apply {
            text = "FUTURE USER"
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(223, 237, 255))
        })
        internalClockText = TextView(this).apply {
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) }
        }
        internalDateText = TextView(this).apply {
            textSize = 11f
            setTextColor(Color.rgb(166, 187, 220))
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        profileText.addView(internalClockText)
        profileText.addView(internalDateText)
        profile.addView(avatar)
        profile.addView(profileText)
        header.addView(profile)
        header.addView(TextView(this).apply {
            text = "●  CONECTADO"
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(112, 255, 214))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(-2, dp(46)).apply { marginEnd = dp(16) }
        })
        searchHint.apply {
            val oldParent = parent as? ViewGroup
            oldParent?.removeView(this)
            text = "⌕"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            contentDescription = "Buscar no catálogo"
            setPadding(0, 0, 0, 0)
            background = rounded(0xB31A2B3B, 14f)
            layoutParams = LinearLayout.LayoutParams(dp(62), dp(54))
        }
        header.addView(searchHint)
        internalContent.addView(header)

        catalogColumn.apply {
            layoutParams = LinearLayout.LayoutParams(0, -1, 0.54f)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(24), dp(8), dp(10), dp(16))
            clipChildren = false
        }
        legacyHeader.visibility = View.GONE
        legacyHeading.visibility = View.GONE
        legacyImport.visibility = View.GONE
        sortRowView.visibility = View.VISIBLE
        sortRowView.clipChildren = false
        sortRowView.clipToPadding = false
        sortRowView.setPadding(0, dp(2), 0, dp(2))
        // O shell deixa a faixa pronta, mas cada seção decide quando exibi-la.
        setInternalCategoryVisibility(false)
        detailsScroll.apply {
            layoutParams = LinearLayout.LayoutParams(0, -1, 0.46f).apply {
                setMargins(dp(8), dp(2), dp(24), dp(14))
            }
            setPadding(0, 0, 0, 0)
            setBackgroundColor(Color.TRANSPARENT)
            clipToPadding = false
            isFillViewport = true
        }
        detailsContent.apply {
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(8), dp(2), dp(8), dp(10))
        }
        panel.apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(176))
            background = rounded(0xD9162A4B, 12f)
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(12).toFloat())
                }
            }
        }
        heroImage.apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0.96f
        }
        legacyOverlay.visibility = View.GONE
        actionRow.orientation = LinearLayout.HORIZONTAL
        actionRow.setPadding(0, dp(2), 0, dp(6))
        detailsContainer.removeView(detailDescription)
        detailsContainer.removeView(nowCard)
        detailsContainer.removeView(nextProgram)
        detailsContainer.removeView(actionRow)
        detailsContainer.addView(nowCard)
        detailsContainer.addView(nextProgram)
        detailsContainer.addView(detailDescription)
        detailsContainer.addView(actionRow)
        detailDescription.textSize = 13f
        detailDescription.setLineSpacing(dp(2).toFloat(), 1f)
        detailChannelName.textSize = 26f
        detailTags.textSize = 12f
        nowCard.setPadding(dp(14), dp(12), dp(14), dp(12))
        currentProgram.textSize = 18f
        currentProgramDescription.textSize = 12f
        programTime.textSize = 11f
        nextProgram.textSize = 12f
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            clipChildren = false
            clipToPadding = false
        }
        body.addView(catalogColumn)
        body.addView(detailsScroll)
        internalContent.addView(body)
        internalShellInstalled = true
        internalShell.visibility = View.GONE
    }

    private fun showInternalShell() {
        if (!internalShellInstalled) return
        internalShell.visibility = View.VISIBLE
        if (::homeSidebarTabs.isInitialized) homeSidebarTabs.visibility = View.GONE
        findViewById<View>(R.id.sideNavigation).visibility = View.VISIBLE
        // Configurações esconde os dois painéis do catálogo. Toda entrada
        // posterior em uma seção precisa reexibi-los explicitamente.
        findViewById<View>(R.id.channelColumn).visibility = View.VISIBLE
        findViewById<View>(R.id.previewScroll).visibility = View.VISIBLE
    }

    private fun hideInternalShell() {
        if (internalShellInstalled) internalShell.visibility = View.GONE
    }

    private fun setInternalCategoryVisibility(visible: Boolean) {
        val parent = categoryList.parent as? View ?: return
        parent.visibility = if (visible) View.VISIBLE else View.GONE
        parent.layoutParams = parent.layoutParams.apply {
            height = if (visible) dp(64) else 0
        }
        if (::sortRow.isInitialized) {
            sortRow.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    /**
     * O snapshot usado pelo cache SQLite pode vir sem entries em memória. Se
     * o usuário troca de aba durante a janela de carregamento, a tela não pode
     * cair no caminho visibleItems() vazio: reidrata o estado paginado a partir
     * dos stats locais e deixa renderCatalog() consultar o banco.
     */
    private fun restoreCachedCatalogIfNeeded() {
        if (databaseBackedCatalog || cacheRestoreInFlight) return
        cacheRestoreInFlight = true
        repository.loadCached { cached ->
            runOnUiThread {
                cacheRestoreInFlight = false
                if (cached == null || cached.totalCount <= 0 || !cached.databaseBacked) return@runOnUiThread
                catalog = cached
                databaseBackedCatalog = true
                categoryCache.clear()
                if (!homeMode && !settingsMode && !radioMode) {
                    renderCategories()
                    renderCatalog()
                    if (selectedEntry == null && !isWithin(currentFocus, channelList)) selectFirstVisible()
                }
            }
        }
    }

    private fun setupCatalogList() {
        catalogAdapter = CatalogAdapter(
            imageLoader = imageLoader,
            fallbackLogo = ::fallbackLogo,
            onSelected = {
                selectEntry(it, false)
                scheduleTrailerPreview(it)
            },
            onClicked = { handleEntryClick(it) },
            onLongClicked = { quickToggleFavorite(it) },
        )
        channelList.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 2)
        channelList.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        channelList.adapter = catalogAdapter
        channelList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val manager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (manager.findLastVisibleItemPosition() >= catalogAdapter.itemCount - 12) loadNextPage()
            }
        })
    }

    private fun renderNavigation() {
        // A mesma sidebar curva da Home é usada em todas as páginas internas.
        // O logo embutido da navegação antiga fica sempre oculto aqui para não
        // duplicar a marca nem alterar a altura dos itens.
        setSidebarBrandVisible(false)
        findViewById<View>(R.id.sideNavigation).background = SidebarGlassDrawable()
        navItems.removeAllViews()
        findViewById<ScrollView>(R.id.navScroll).apply {
            isFillViewport = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        }
        navItems.layoutParams = FrameLayout.LayoutParams(-1, -1)
        // Ordem e conjunto visíveis seguem a referência aprovada: sem a
        // navegação antiga duplicada de Início/Voz no shell interno.
        val items = listOf(
            Triple("CANAIS", R.drawable.home_nav_live, "Canais"),
            Triple("FILMES", R.drawable.home_nav_movies, "Filmes"),
            Triple("SÉRIES", R.drawable.home_nav_series, "Séries"),
            Triple("FAVORITOS", R.drawable.home_nav_favorites, "Favoritos"),
            Triple("RÁDIOS", R.drawable.home_nav_radio, "Rádios"),
            Triple("VOZ", R.drawable.home_nav_microphone, "Microfone"),
            Triple("AJUSTES", R.drawable.home_nav_settings, "Configurações"),
        )
        items.forEachIndexed { index, (label, iconRes, captionText) ->
            lateinit var icon: ImageView
            lateinit var caption: TextView
            val row = LinearLayout(this).apply {
                id = View.generateViewId()
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isFocusable = true
                isClickable = true
                tag = label
                clipChildren = false
                clipToPadding = false
                setPadding(0, dp(2), 0, dp(2))
                layoutParams = LinearLayout.LayoutParams(-1, 0, 1f).apply { setMargins(0, 0, 0, 0) }
                setOnClickListener {
                    when (label) {
                        "INÍCIO" -> showHome()
                        "CANAIS" -> switchSection(MediaKind.LIVE)
                        "FILMES" -> switchSection(MediaKind.MOVIE)
                        "SÉRIES" -> switchSection(MediaKind.SERIES)
                        "FAVORITOS" -> switchFavorites()
                        "RÁDIOS" -> switchRadio()
                        "VOZ" -> startVoiceCommand()
                        "AJUSTES" -> showSettingsScreen()
                    }
                }
                setOnFocusChangeListener { view, hasFocus ->
                    updateNavigationVisuals(if (hasFocus) view else navItems.findFocus())
                }
            }
            icon = ImageView(this).apply {
                setImageResource(iconRes)
                imageTintList = null
                scaleType = ImageView.ScaleType.FIT_CENTER
                scaleX = 1f
                scaleY = 1f
                alpha = if (isNavigationSelected(label)) 1f else 0.72f
                background = null
                    layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { setMargins(0, 0, 0, dp(1)) }
                setPadding(0, 0, 0, 0)
            }
            caption = TextView(this).apply {
                text = captionText
                gravity = Gravity.CENTER
                includeFontPadding = false
                textSize = 10f
                setTextColor(if (isNavigationSelected(label)) Color.rgb(232, 242, 255) else Color.rgb(170, 186, 220))
                setShadowLayer(4f, 0f, 1f, 0xCC000000.toInt())
                layoutParams = LinearLayout.LayoutParams(-1, dp(22))
            }
            icon.visibility = View.VISIBLE
            caption.visibility = View.VISIBLE
            row.addView(icon)
            row.addView(caption)
            row.background = sidebarRowDrawable(isNavigationSelected(label))
            navItems.addView(row)
        }
    }

    private fun updateNavigationVisuals(focusedView: View?) {
        for (index in 0 until navItems.childCount) {
            val child = navItems.getChildAt(index)
            val row = child as? LinearLayout ?: continue
            val label = row.tag as? String ?: continue
            val active = if (focusedView != null) child === focusedView else isNavigationSelected(label)
            row.background = sidebarRowDrawable(active)
            (row.getChildAt(0) as? ImageView)?.alpha = if (active) 1f else 0.62f
            (row.getChildAt(0) as? ImageView)?.imageTintList = null
            (row.getChildAt(1) as? TextView)?.setTextColor(
                if (active) Color.rgb(232, 242, 255) else Color.rgb(170, 186, 220)
            )
        }
    }

    private fun sidebarRowDrawable(active: Boolean): GradientDrawable {
        return if (active) {
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0x7A6E8FC9, 0x4A5E79B8, 0x207A9CDB),
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), 0x8BC7E8FF.toInt())
            }
        } else {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setColor(0x00000000)
            }
        }
    }

    private fun isNavigationSelected(label: String): Boolean = when {
        settingsMode -> label == "AJUSTES"
        homeMode -> label == "INÍCIO"
        favoritesOnly -> label == "FAVORITOS"
        radioMode -> label == "RÁDIOS"
        voiceMode -> label == "VOZ"
        currentKind == MediaKind.LIVE -> label == "CANAIS"
        currentKind == MediaKind.MOVIE -> label == "FILMES"
        else -> label == "SÉRIES"
    }

    private fun profilePreferences() = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE)

    private fun loadProfiles(): MutableList<UserProfile> {
        val raw = profilePreferences().getString(PREF_PROFILES, null).orEmpty()
        val profiles = mutableListOf<UserProfile>()
        if (raw.isNotBlank()) {
            runCatching {
                val array = JSONArray(raw)
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").trim()
                    val name = item.optString("name").trim()
                    if (id.isNotBlank() && name.isNotBlank()) profiles += UserProfile(id, name, item.optInt("avatar", 0).coerceIn(0, profileAvatarAssets.lastIndex.coerceAtLeast(0)))
                }
            }
        }
        return profiles
            .filter { it.id != "default" }
            .distinctBy { it.id }
            .toMutableList()
    }

    private fun saveProfiles(profiles: List<UserProfile>) {
        val array = JSONArray()
        profiles.forEach { profile ->
            array.put(JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("avatar", profile.avatar)
            })
        }
        profilePreferences().edit().putString(PREF_PROFILES, array.toString()).apply()
    }

    private fun loadActiveProfile() {
        val profiles = loadProfiles()
        val requestedId = profilePreferences().getString(PREF_ACTIVE_PROFILE_ID, "").orEmpty()
        val active = profiles.firstOrNull { it.id == requestedId } ?: profiles.firstOrNull()
        if (active == null) {
            activeProfileId = ""
            activeProfileName = "SELECIONE UM PERFIL"
            activeProfileAvatar = 0
            profilePreferences().edit().remove(PREF_ACTIVE_PROFILE_ID).apply()
        } else {
            activeProfileId = active.id
            activeProfileName = active.name
            activeProfileAvatar = active.avatar
            if (requestedId != active.id) profilePreferences().edit().putString(PREF_ACTIVE_PROFILE_ID, active.id).apply()
        }
        saveProfiles(profiles)
    }

    private fun updateProfileHeader() {
        if (!::homeUserName.isInitialized) return
        homeUserName.text = activeProfileName
        if (activeProfileId.isBlank()) {
            homeProfileImage.setImageResource(R.drawable.future_logo_safe)
        } else {
            homeProfileImage.setImageResource(profileAvatarAssets.getOrElse(activeProfileAvatar) { profileAvatarAssets.first() })
        }
        homeProfileImage.contentDescription = if (activeProfileId.isBlank()) "Selecionar perfil" else "Perfil $activeProfileName. Clique para trocar"
    }

    private fun positionHomeSidebarTabs() {
        if (!::homePanel.isInitialized || !::homeSidebarTabs.isInitialized) return
        val width = homePanel.width
        val height = homePanel.height
        if (width <= 0 || height <= 0) return
        val params = homeSidebarTabs.layoutParams as? FrameLayout.LayoutParams ?: return
        // Proporção da imagem aprovada: painel branco largo na lateral,
        // cápsulas menores dentro dele e espaço de cabeçalho no topo.
        params.width = (width * 0.19f).toInt().coerceAtLeast(dp(240))
        params.height = height
        params.leftMargin = 0
        params.topMargin = 0
        homeSidebarTabs.layoutParams = params
    }

    private fun positionHomeProfileHeader() {
        if (!::homePanel.isInitialized || !::homeUserHeader.isInitialized) return
        val width = homePanel.width
        val height = homePanel.height
        if (width <= 0 || height <= 0) return
        val params = homeUserHeader.layoutParams as? FrameLayout.LayoutParams ?: return
        // Cabeçalho compacto no canto superior esquerdo. A largura agora cobre
        // avatar, nome, relógio e data sem cortar os últimos dígitos.
        params.width = (width * 0.22f).toInt().coerceAtLeast(dp(250))
        // A área clicável termina antes da primeira aba; assim clicar em
        // NOVELAS nunca pode acionar o perfil por sobreposição.
        params.height = (height * 0.09f).toInt()
        params.leftMargin = (width * 0.008f).toInt().coerceAtLeast(dp(6))
        params.topMargin = (height * 0.015f).toInt()
        homeUserHeader.layoutParams = params
    }

    private fun showProfileDialog() {
        profileDialog?.dismiss()
        val profiles = loadProfiles()
        var selectedProfileId: String? = profiles.firstOrNull { it.id == activeProfileId }?.id
        var selectedAvatar = profiles.firstOrNull { it.id == activeProfileId }?.avatar ?: 0
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(24), dp(28), dp(22))
            background = rounded(0xF20A1730, 24f)
        }
        val title = TextView(this).apply {
            text = "ESCOLHER PERFIL"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = "Cada perfil mantém seus favoritos e seu histórico separados."
            setTextColor(Color.rgb(174, 205, 235))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5); bottomMargin = dp(16) }
        }
        root.addView(title)
        root.addView(subtitle)
        val newProfileButton = TextView(this).apply {
            text = "CRIAR NOVO PERFIL"
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(125, 246, 224))
            textSize = 11f
            isFocusable = true
            isClickable = true
            background = rounded(0x332A6B72, 10f)
            layoutParams = LinearLayout.LayoutParams(dp(190), dp(34)).apply { bottomMargin = dp(8) }
        }
        root.addView(newProfileButton)
        val cardsScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(-1, dp(150))
        }
        val cardsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        cardsScroll.addView(cardsRow)
        root.addView(cardsScroll)
        val nameInput = EditText(this).apply {
            hint = "Nome do perfil"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setText(profiles.firstOrNull { it.id == activeProfileId }?.name.orEmpty())
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(145, 174, 205))
            textSize = 15f
            setPadding(dp(14), 0, dp(14), 0)
            background = rounded(0x663C5E82, 12f)
            layoutParams = LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(12) }
        }
        root.addView(nameInput)
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(14) }
        }
        val cancel = TextView(this).apply {
            text = "CANCELAR"
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(187, 211, 238))
            textSize = 12f
            isFocusable = true
            isClickable = true
            background = rounded(0x332A4265, 12f)
            layoutParams = LinearLayout.LayoutParams(dp(130), dp(46)).apply { marginEnd = dp(10) }
        }
        val save = TextView(this).apply {
            text = "OK"
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(4, 16, 28))
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            isFocusable = true
            isClickable = true
            background = rounded(0xFF55E6D0, 12f)
            layoutParams = LinearLayout.LayoutParams(dp(130), dp(46))
        }
        actions.addView(cancel)
        actions.addView(save)
        root.addView(actions)

        val cardViews = mutableListOf<Pair<String?, View>>()
        fun repaintCards() {
            cardViews.forEach { (id, view) ->
                val avatar = (view.tag as? Int) ?: 0
                val active = if (id != null) id == selectedProfileId else avatar == selectedAvatar
                view.background = rounded(if (active) 0x663FE7EF else 0x261C3150, 16f).apply {
                    if (active) setStroke(dp(2), Color.rgb(86, 239, 226))
                }
            }
        }
        fun addProfileCard(profile: UserProfile?, avatar: Int, label: String, preserveSelectedProfile: Boolean = false) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isFocusable = true
                isClickable = true
                setPadding(dp(7), dp(7), dp(7), dp(7))
                layoutParams = LinearLayout.LayoutParams(dp(116), dp(132)).apply { setMargins(0, 0, dp(10), 0) }
                tag = avatar
            }
            val image = ImageView(this).apply {
                setImageResource(profileAvatarAssets.getOrElse(avatar) { profileAvatarAssets.first() })
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = ovalBackground(0x553B6A9C)
                outlineProvider = ViewOutlineProvider.BACKGROUND
                clipToOutline = true
                contentDescription = label
                layoutParams = LinearLayout.LayoutParams(dp(72), dp(72))
            }
            val labelView = TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(Color.WHITE)
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(-1, dp(28)).apply { topMargin = dp(4) }
            }
            card.addView(image)
            card.addView(labelView)
            card.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) view.alpha = 1f else view.alpha = 0.86f
            }
            card.setOnClickListener {
                if (profile != null) {
                    selectedProfileId = profile.id
                    selectedAvatar = profile.avatar
                    nameInput.setText(profile.name)
                    nameInput.setSelection(nameInput.text.length)
                } else if (!preserveSelectedProfile) {
                    selectedProfileId = null
                    nameInput.setText("")
                }
                selectedAvatar = avatar
                repaintCards()
            }
            cardsRow.addView(card)
            cardViews += (profile?.id to card)
        }
        newProfileButton.setOnClickListener {
            selectedProfileId = null
            selectedAvatar = 0
            nameInput.setText("")
            repaintCards()
            nameInput.requestFocus()
        }
        profiles.forEach { addProfileCard(it, it.avatar, it.name) }
        // A galeria contém somente os avatares fornecidos. Para criar um perfil,
        // basta escolher qualquer avatar, informar o nome e confirmar em OK.
        profileAvatarAssets.indices.forEach { avatar -> addProfileCard(null, avatar, "Avatar ${avatar + 1}", preserveSelectedProfile = true) }
        repaintCards()

        val dialog = Dialog(this)
        profileDialog = dialog
        dialog.setContentView(root)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnDismissListener { if (profileDialog === dialog) profileDialog = null }
        cancel.setOnClickListener { dialog.dismiss() }
        save.setOnClickListener {
            val name = nameInput.text.toString().trim().ifBlank { "Perfil ${profiles.size + 1}" }.take(28)
            val updated = profiles.toMutableList()
            val existingIndex = selectedProfileId?.let { id -> updated.indexOfFirst { it.id == id } } ?: -1
            val profile = if (existingIndex >= 0) {
                UserProfile(updated[existingIndex].id, name, selectedAvatar)
            } else {
                UserProfile("profile_${System.currentTimeMillis()}", name, selectedAvatar)
            }
            if (existingIndex >= 0) updated[existingIndex] = profile else updated += profile
            saveProfiles(updated)
            activeProfileId = profile.id
            activeProfileName = profile.name
            activeProfileAvatar = profile.avatar
            profilePreferences().edit().putString(PREF_ACTIVE_PROFILE_ID, activeProfileId).apply()
            updateProfileHeader()
            refreshForProfileChange()
            dialog.dismiss()
        }
        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes = window.attributes.apply { dimAmount = 0.72f }
            window.setLayout((resources.displayMetrics.widthPixels * 0.70f).toInt().coerceAtLeast(dp(700)), dp(430))
        }
        dialog.setOnShowListener {
            dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.70f).toInt().coerceAtLeast(dp(700)), dp(430))
            cardViews.firstOrNull { it.first == activeProfileId }?.second?.requestFocus()
        }
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.70f).toInt().coerceAtLeast(dp(700)), dp(430))
    }

    private fun refreshForProfileChange() {
        if (!homeMode && !settingsMode && !radioMode) {
            selectedEntry = null
            renderCategories()
            renderCatalog()
            selectFirstVisible()
        }
    }

    private fun showHome() {
        parentalUnlocked = false
        homeMode = true
        settingsMode = false
        settingsPanel?.let { (it.parent as? ViewGroup)?.removeView(it) }
        settingsPanel = null
        homeFeaturedKey = null
        homeRotationRequestId++
        homeRotationEntries = emptyList()
        homeRotationIndex = 0
        favoritesOnly = false
        radioMode = false
        voiceMode = false
        radioDialog?.dismiss()
        renderNavigation()
        setSidebarBrandVisible(false)
        homeUserHeader.visibility = View.VISIBLE
        positionHomeProfileHeader()
        updateProfileHeader()
        homePanel.visibility = View.VISIBLE
        homeSidebarTabs.visibility = View.VISIBLE
        (homePanel.layoutParams as? FrameLayout.LayoutParams)?.let { params ->
            params.leftMargin = 0
            homePanel.layoutParams = params
        }
        homePanel.post {
            positionHomeSidebarTabs()
            positionHomeProfileHeader()
            renderExactHomeHotspots()
            homeSidebarTabs.bringToFront()
            homeUserHeader.bringToFront()
        }
        hideInternalShell()
        findViewById<View>(R.id.sideNavigation).visibility = View.GONE
        findViewById<View>(R.id.channelColumn).visibility = View.GONE
        findViewById<View>(R.id.previewScroll).visibility = View.GONE
    }

    private fun renderExactHomeHotspots() {
        homeOrbitRoot.removeAllViews()
        val width = homeOrbitRoot.width
        val height = homeOrbitRoot.height
        if (width <= 0 || height <= 0) {
            homeOrbitRoot.post { renderExactHomeHotspots() }
            return
        }

        updateHomeClock()
        mainHandler.removeCallbacks(homeClockTicker)
        mainHandler.post(homeClockTicker)
        val heroSize = (minOf(width, height) * 0.45f).toInt()
        val heroLeft = (width * 0.535f - heroSize / 2f).toInt()
        val heroTop = (height * 0.505f - heroSize / 2f).toInt()
        val heroOverlay = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(heroSize, heroSize).apply {
                leftMargin = heroLeft
                topMargin = heroTop
            }
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        heroOverlay.addView(View(this).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x00000000, 0xEA020817.toInt()),
            )
        })
        val dynamicBadge = TextView(this).apply {
            text = "AO VIVO"
            setTextColor(Color.WHITE)
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            background = rounded(0xFFE03D48, 12f)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            layoutParams = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                topMargin = dp(22)
            }
        }
        val dynamicLogo = ImageView(this).apply {
            // A arte ocupa todo o círculo e continua inteira dentro da máscara.
            // O degradê inferior é um irmão separado e protege o texto.
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = 0.96f
            layoutParams = FrameLayout.LayoutParams(-1, -1, Gravity.CENTER).apply {
                setMargins(dp(5), dp(5), dp(5), dp(5))
            }
            setPadding(0, 0, 0, 0)
        }
        val dynamicTitle = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
                leftMargin = dp(18)
                rightMargin = dp(18)
                bottomMargin = dp(38)
            }
        }
        val dynamicMeta = TextView(this).apply {
            setTextColor(Color.rgb(180, 221, 255))
            textSize = 11f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
                leftMargin = dp(12)
                rightMargin = dp(12)
                bottomMargin = dp(14)
            }
        }
        val logoShade = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x00020817, 0x00020817, 0xD9020817.toInt()),
            )
        }
        heroOverlay.addView(dynamicBadge)
        heroOverlay.addView(dynamicLogo)
        heroOverlay.addView(logoShade)
        heroOverlay.addView(dynamicTitle)
        heroOverlay.addView(dynamicMeta)
        homeOrbitRoot.addView(heroOverlay)
        exactHomeBadge = dynamicBadge
        exactHomeChannelLogo = dynamicLogo
        exactHomeProgramTitle = dynamicTitle
        exactHomeProgramMeta = dynamicMeta

        data class Hotspot(
            val left: Float,
            val top: Float,
            val right: Float,
            val bottom: Float,
            val group: String,
            val action: () -> Unit,
        )

        val openLive = { switchSection(MediaKind.LIVE) }
        val openChannels = { switchSection(MediaKind.LIVE) }
        val openMovies = { switchSection(MediaKind.MOVIE) }
        val openSeries = { switchSection(MediaKind.SERIES) }
        val openFavorites = { switchFavorites() }
        val openRadios = { switchRadio() }
        val openSports = { openSportsDayCategory() }
        val openKids = { openCategoryByKeywords(listOf("kids", "infantil", "desenho", "animação", "animacao"), "Kids") }
        val openCenter = {
            // O centro pode estar exibindo uma entrada diferente da seleção
            // anterior da grade. Use sempre a entrada da rotação atual; assim,
            // Patati Patatá/EPG abre o canal correspondente e, se a entrada for
            // filme ou série, switchSection recebe o MediaKind correto.
            val displayedEntry = exactHomeProgramEntry ?: selectedEntry
            displayedEntry?.let { openFeaturedEntry(it) } ?: openLive()
        }

        val hotspots = listOf(
            // Canal central.
            Hotspot(0.34f, 0.24f, 0.68f, 0.76f, "center", openCenter),
            // Busca e hubs principais.
            Hotspot(0.88f, 0.00f, 1.00f, 0.16f, "search", { showSearchDialog() }),
            Hotspot(0.47f, 0.00f, 0.61f, 0.19f, "channels", openChannels),
            Hotspot(0.26f, 0.11f, 0.44f, 0.31f, "sports", openSports),
            Hotspot(0.62f, 0.09f, 0.80f, 0.30f, "movies", openMovies),
            Hotspot(0.74f, 0.30f, 0.94f, 0.57f, "series", openSeries),
            Hotspot(0.64f, 0.66f, 0.83f, 0.90f, "favorites", openFavorites),
            Hotspot(0.40f, 0.69f, 0.61f, 0.94f, "radios", openRadios),
            Hotspot(0.23f, 0.66f, 0.43f, 0.90f, "kids", openKids),
            // Satélites Esporte.
            Hotspot(0.25f, 0.10f, 0.30f, 0.18f, "sports", openSports),
            Hotspot(0.34f, 0.06f, 0.40f, 0.14f, "sports", openSports),
            Hotspot(0.20f, 0.20f, 0.27f, 0.29f, "sports", openSports),
            Hotspot(0.30f, 0.21f, 0.37f, 0.30f, "sports", openSports),
            // Satélites Cinema.
            Hotspot(0.70f, 0.04f, 0.77f, 0.13f, "movies", openMovies),
            Hotspot(0.76f, 0.10f, 0.83f, 0.20f, "movies", openMovies),
            Hotspot(0.70f, 0.18f, 0.77f, 0.28f, "movies", openMovies),
            Hotspot(0.80f, 0.19f, 0.87f, 0.29f, "movies", openMovies),
            // Satélites Séries.
            Hotspot(0.80f, 0.32f, 0.87f, 0.41f, "series", openSeries),
            Hotspot(0.87f, 0.34f, 0.94f, 0.44f, "series", openSeries),
            Hotspot(0.84f, 0.46f, 0.91f, 0.56f, "series", openSeries),
            Hotspot(0.76f, 0.47f, 0.83f, 0.57f, "series", openSeries),
            // Satélites Favoritos.
            Hotspot(0.73f, 0.64f, 0.80f, 0.73f, "favorites", openFavorites),
            Hotspot(0.80f, 0.70f, 0.87f, 0.80f, "favorites", openFavorites),
            Hotspot(0.69f, 0.78f, 0.76f, 0.88f, "favorites", openFavorites),
            Hotspot(0.78f, 0.82f, 0.85f, 0.92f, "favorites", openFavorites),
            // Satélites Rádios.
            Hotspot(0.43f, 0.76f, 0.50f, 0.85f, "radios", openRadios),
            Hotspot(0.51f, 0.77f, 0.58f, 0.87f, "radios", openRadios),
            Hotspot(0.45f, 0.88f, 0.52f, 0.98f, "radios", openRadios),
            Hotspot(0.56f, 0.86f, 0.63f, 0.95f, "radios", openRadios),
            // Satélites Kids.
            Hotspot(0.24f, 0.65f, 0.31f, 0.75f, "kids", openKids),
            Hotspot(0.20f, 0.76f, 0.27f, 0.86f, "kids", openKids),
            Hotspot(0.29f, 0.80f, 0.36f, 0.90f, "kids", openKids),
            Hotspot(0.33f, 0.70f, 0.40f, 0.80f, "kids", openKids),
        )

        // Cada categoria (principal + satélites) vira UMA região focável só,
        // usando o retângulo que envolve tudo daquele grupo -- antes eram
        // varias views separadas fazendo a mesma coisa, e o controle remoto
        // tinha que passar por cada uma individualmente pra sair da
        // categoria, em vez de ir direto pra próxima.
        val mergedHotspots = hotspots.groupBy { it.group }.map { (_, group) ->
            Hotspot(
                left = group.minOf { it.left },
                top = group.minOf { it.top },
                right = group.maxOf { it.right },
                bottom = group.maxOf { it.bottom },
                group = group.first().group,
                action = group.first().action,
            )
        }

        exactHomeHotspots = mergedHotspots.map { hotspot ->
            View(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ((hotspot.right - hotspot.left) * width).toInt().coerceAtLeast(24),
                    ((hotspot.bottom - hotspot.top) * height).toInt().coerceAtLeast(24),
                ).apply {
                    leftMargin = (hotspot.left * width).toInt()
                    topMargin = (hotspot.top * height).toInt()
                }
                isFocusable = true
                isClickable = true
                setOnClickListener { hotspot.action() }
                foreground = ovalFocusRing()
            }
        }
        exactHomeHotspots.forEach(homeOrbitRoot::addView)
        exactHomeHotspots.firstOrNull()?.let { first ->
            first.nextFocusUpId = homeUserHeader.id
            if (::homeSidebarTabs.isInitialized) {
                homeSidebarTabs.setRightFocusTargetId(first.id)
                val sidebarFirstId = homeSidebarTabs.firstTabId()
                if (sidebarFirstId != View.NO_ID) {
                    homeUserHeader.nextFocusDownId = sidebarFirstId
                    homeUserHeader.nextFocusRightId = sidebarFirstId
                }
            } else {
                homeUserHeader.nextFocusDownId = first.id
                homeUserHeader.nextFocusRightId = first.id
            }
            first.post { if (homeMode && currentFocus == null) first.requestFocus() }
        }
        loadHomeRotationEntries(++homeRotationRequestId)
    }

    private fun loadHomeRotationEntries(requestId: Int) {
        val results = mutableMapOf<MediaKind, List<CatalogEntry>>()
        var pending = 3
        val hidden = hiddenGroups()
        fun finish() {
            if (!homeMode || requestId != homeRotationRequestId) return
            val live = results[MediaKind.LIVE].orEmpty().distinctBy { it.key }
            val allDay = live.filter { isTwentyFourHourEntry(it) }
            val regularLive = live.filterNot { isTwentyFourHourEntry(it) }
            val movies = results[MediaKind.MOVIE].orEmpty().distinctBy { it.key }
            val series = results[MediaKind.SERIES].orEmpty().distinctBy { it.key }
            val buckets = listOf(allDay, regularLive, movies, series)
            val mixed = buildList {
                val maxSize = buckets.maxOfOrNull { it.size } ?: 0
                for (index in 0 until maxSize) {
                    buckets.forEach { bucket -> bucket.getOrNull(index)?.let(::add) }
                }
            }.distinctBy { it.key }
            homeRotationEntries = mixed
            if (mixed.isEmpty()) {
                updateExactHomeProgram(null)
                return
            }
            val currentKey = exactHomeProgramEntry?.key
            homeRotationIndex = mixed.indexOfFirst { it.key == currentKey }.takeIf { it >= 0 } ?: 0
            updateExactHomeProgram(mixed[homeRotationIndex])
        }
        listOf(MediaKind.LIVE, MediaKind.MOVIE, MediaKind.SERIES).forEach { kind ->
            repository.queryPage(
                kind = kind,
                group = "Todos",
                search = "",
                hidden = hidden,
                favorites = emptySet(),
                sortMode = SortMode.RECENT,
                limit = 18,
                offset = 0,
                seriesOnly = kind == MediaKind.SERIES,
                includeAdult = false,
            ) { entries ->
                runOnUiThread {
                    if (requestId != homeRotationRequestId) return@runOnUiThread
                    results[kind] = entries
                    pending--
                    if (pending == 0) finish()
                }
            }
        }
    }

    private fun isTwentyFourHourEntry(entry: CatalogEntry): Boolean {
        val value = "${entry.name} ${entry.groupTitle}".lowercase(Locale.ROOT)
        return value.contains("24 horas") || value.contains("24h") || value.contains("24/7") || value.contains("24 x 7") || value.contains("24x7")
    }

    private fun advanceHomeRotation() {
        if (!homeMode || homeRotationEntries.isEmpty()) return
        homeRotationIndex = (homeRotationIndex + 1) % homeRotationEntries.size
        val entry = homeRotationEntries[homeRotationIndex]
        homeFeaturedKey = entry.key
        updateExactHomeProgram(entry)
    }

    private fun updateExactHomeProgram(entry: CatalogEntry?) {
        exactHomeProgramEntry = entry
        if (entry == null) {
            exactHomeBadge?.text = "FUTURE"
            exactHomeBadge?.background = rounded(0xFF2E557C, 12f)
            exactHomeProgramTitle?.text = "Aguardando catálogo"
            exactHomeProgramMeta?.text = "Nenhum conteúdo disponível"
            exactHomeChannelLogo?.setImageResource(R.drawable.future_logo_safe)
            return
        }

        val isLive = entry.kind == MediaKind.LIVE
        val programs = if (isLive) epgProgramsFor(entry) else emptyList()
        val current = if (isLive) currentEpgProgram(programs) else null
        val next = current?.let { active -> programs.firstOrNull { it.start >= active.stop } }
        val displayTitle = when {
            current?.title?.isNotBlank() == true -> current.title
            entry.kind == MediaKind.SERIES -> seriesTitle(entry).ifBlank { entry.name }
            else -> entry.name
        }
        exactHomeBadge?.text = when (entry.kind) {
            MediaKind.LIVE -> "AO VIVO"
            MediaKind.MOVIE -> "FILME"
            MediaKind.SERIES -> "SÉRIE"
        }
        exactHomeBadge?.background = rounded(
            when (entry.kind) {
                MediaKind.LIVE -> 0xFFE03D48
                MediaKind.MOVIE -> 0xFFE29A35
                MediaKind.SERIES -> 0xFF8B5BD6
            },
            12f,
        )
        exactHomeProgramTitle?.text = displayTitle
        exactHomeProgramMeta?.text = when {
            isLive && current != null && next != null -> "${entry.name}  •  ${formatTime(current.start)}–${formatTime(current.stop)}  •  AO VIVO\nA seguir: ${next.title}  •  ${formatTime(next.start)}"
            isLive && current != null -> "${entry.name}  •  ${formatTime(current.start)}–${formatTime(current.stop)}  •  AO VIVO"
            isLive -> "${entry.name}  •  ${entry.groupTitle.ifBlank { "CANAIS" }}  •  AO VIVO"
            else -> "${entry.groupTitle.ifBlank { "Catálogo" }}  •  ${kindLabel(entry.kind)}"
        }
        val logo = entry.logoUrl.ifBlank { entry.backdropUrl }
        if (logo.isBlank()) {
            exactHomeChannelLogo?.setImageResource(fallbackLogo(entry))
        } else {
            exactHomeChannelLogo?.let { imageLoader.loadCropped(logo, it, fallbackLogo(entry)) }
        }
    }

    private fun setSidebarBrandVisible(visible: Boolean) {
        appLogo.visibility = if (visible) View.VISIBLE else View.GONE
        brandMark.visibility = if (visible) View.VISIBLE else View.GONE
        brandSubtitle.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun alignHomePanelToNavigation() {
        homePanel.post {
            val navigation = findViewById<View>(R.id.sideNavigation)
            val width = navigation.width
            if (width <= 0) return@post
            val params = homePanel.layoutParams as? FrameLayout.LayoutParams ?: return@post
            if (params.leftMargin != width) {
                params.leftMargin = width
                homePanel.layoutParams = params
            }
        }
    }

    private fun updateHomeClock() {
        val now = java.util.Calendar.getInstance()
        val clock = String.format("%02d:%02d", now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE))
        val meses = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
        val date = "${now.get(java.util.Calendar.DAY_OF_MONTH)} ${meses[now.get(java.util.Calendar.MONTH)]}"
        homeClockText.text = clock
        homeDateText.text = date
        internalClockText?.text = clock
        internalDateText?.text = date
        if (homeMode) exactHomeProgramEntry?.let { updateExactHomeProgram(it) }
    }

    private data class OrbitCategory(val label: String, val sublabel: String, val iconRes: Int, val color: Long, val action: () -> Unit)

    private val orbitSatelliteIcons = mapOf(
        "ESPORTE" to listOf(R.drawable.orbit_icon_sport, R.drawable.ic_nav_live, R.drawable.orbit_icon_favorites, R.drawable.ic_nav_radio),
        "CINEMATOGRÁFICOS" to listOf(R.drawable.orbit_icon_cinema, R.drawable.orbit_icon_series, R.drawable.orbit_icon_favorites, R.drawable.ic_nav_movies),
        "SÉRIES" to listOf(R.drawable.orbit_icon_series, R.drawable.ic_nav_live, R.drawable.orbit_icon_cinema, R.drawable.ic_nav_favorites),
        "FAVORITOS" to listOf(R.drawable.orbit_icon_favorites, R.drawable.orbit_icon_cinema, R.drawable.ic_nav_series, R.drawable.ic_nav_radio),
        "RÁDIOS" to listOf(R.drawable.orbit_icon_radio, R.drawable.ic_nav_voice, R.drawable.orbit_icon_radio, R.drawable.ic_nav_radio),
        "KIDS" to listOf(R.drawable.orbit_icon_kids, R.drawable.orbit_icon_favorites, R.drawable.orbit_icon_cinema, R.drawable.ic_nav_series),
    )

    // Constroi a tela inicial em formato de "orbita": um circulo central
    // (destaque ao vivo) com bolhas de categoria distribuidas ao redor,
    // conectadas por linhas -- inspirado na referencia visual enviada pelo
    // usuario. Tudo calculado em pixels reais na tela, nao em XML fixo, pra
    // se adaptar a qualquer tamanho de tela.
    private fun renderHomeHero() {
        updateHomeClock()
        mainHandler.removeCallbacks(homeClockTicker)
        mainHandler.post(homeClockTicker)
        homeOrbitRoot.post { buildOrbit() }
    }

    private fun buildOrbit() {
        homeOrbitRoot.removeAllViews()
        val w = homeOrbitRoot.width
        val h = homeOrbitRoot.height
        if (w <= 0 || h <= 0) return
        val centerX = w / 2f
        val centerY = h / 2f
        val centerSize = (minOf(w, h) * 0.43f).toInt()
        val centerHaloSize = centerSize + dp(24)

        val categories = listOf(
            OrbitCategory("ESPORTE", "Conexão", R.drawable.orbit_icon_sport, 0xFF2BFFB0) { openCategoryByKeywords(listOf("futebol", "esporte", "sport"), "Esporte") },
            OrbitCategory("CINEMATOGRÁFICOS", "FILMES", R.drawable.orbit_icon_cinema, 0xFFF5B93D) { switchSection(MediaKind.MOVIE) },
            OrbitCategory("SÉRIES", "Universo", R.drawable.orbit_icon_series, 0xFF4FC3F7) { switchSection(MediaKind.SERIES) },
            OrbitCategory("FAVORITOS", "Memórias", R.drawable.orbit_icon_favorites, 0xFFFF6EA8) { switchSection(MediaKind.LIVE); favoritesOnly = true; renderCategories(); renderCatalog() },
            OrbitCategory("RÁDIOS", "Melodia", R.drawable.orbit_icon_radio, 0xFFB388FF) { switchRadio() },
            OrbitCategory("KIDS", "Sonhos", R.drawable.orbit_icon_kids, 0xFFFF8A50) { openCategoryByKeywords(listOf("kids", "infantil", "desenho", "animação", "animacao"), "Kids") },
        )

        // Centro: esfera de conteúdo em destaque, com anel translúcido e leitura em camadas.
        val centerHalo = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(centerHaloSize, centerHaloSize).apply {
                leftMargin = (centerX - centerHaloSize / 2f).toInt()
                topMargin = (centerY - centerHaloSize / 2f).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x1A4968C7)
                setStroke(dp(2), 0xAA6FA8FF.toInt())
            }
            alpha = 0.9f
        }
        homeOrbitRoot.addView(centerHalo)

        val center = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(centerSize, centerSize).apply {
                leftMargin = (centerX - centerSize / 2f).toInt()
                topMargin = (centerY - centerSize / 2f).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF0A1231.toInt())
                setStroke(dp(2), 0xDD86B4FF.toInt())
            }
            clipToOutline = true
            isFocusable = true
            isClickable = true
            elevation = dp(8).toFloat()
        }
        val centerImage = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.future_home_hero)
        }
        val centerBadge = TextView(this).apply {
            text = "AO VIVO"
            setTextColor(Color.WHITE)
            textSize = 10f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = rounded(0xFFE23B3B, 8f)
            setPadding(dp(8), dp(3), dp(8), dp(3))
            layoutParams = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(14) }
        }
        val centerTitle = TextView(this).apply {
            text = "Selecione um canal"
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { setMargins(dp(12), 0, dp(12), dp(16)) }
        }
        center.addView(centerImage)
        center.addView(View(this).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x00000000, 0xE600071C.toInt()))
        })
        center.addView(centerBadge)
        center.addView(centerTitle)
        center.setOnClickListener { switchSection(MediaKind.LIVE) }
        center.foreground = ovalFocusRing()
        homeOrbitRoot.addView(center)
        orbitCenterCard = center

        repository.mostRecent(MediaKind.LIVE, hiddenGroups()) { entry ->
            if (entry != null) runOnUiThread {
                homeFeaturedKey = entry.key
                centerTitle.text = entry.name
                val source = entry.logoUrl.ifBlank { entry.backdropUrl }
                if (source.isNotBlank()) imageLoader.load(source, centerImage, R.drawable.future_home_hero)
                center.setOnClickListener { openFeaturedEntry(entry) }
            }
        }

        // Bolhas ao redor, distribuidas em circulo.
        val radius = minOf(w, h) * 0.38f
        val bubbleSize = (minOf(w, h) * 0.075f).toInt().coerceAtLeast(dp(54))
        val points = mutableListOf<PointF>()
        val satelliteLinks = mutableListOf<Pair<PointF, PointF>>()
        val bubbles = mutableListOf<View>()
        val satelliteGroups = mutableListOf<List<View>>()
        categories.forEachIndexed { index, category ->
            val angle = (Math.PI * 2 * index / categories.size) - Math.PI / 2
            val bx = centerX + radius * Math.cos(angle).toFloat()
            val by = centerY + radius * Math.sin(angle).toFloat()
            points.add(PointF(bx, by))

            val bubble = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize).apply {
                    leftMargin = (bx - bubbleSize / 2f).toInt()
                    topMargin = (by - bubbleSize / 2f).toInt()
                }
                background = orbitalBubbleDrawable(category.color)
                isFocusable = true
                isClickable = true
                foreground = ovalFocusRing()
            }
            val icon = ImageView(this).apply {
                setImageResource(category.iconRes)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(16), dp(16), dp(16), dp(16))
                layoutParams = FrameLayout.LayoutParams(-1, -1)
            }
            bubble.addView(icon)
            bubble.setOnFocusChangeListener { view, hasFocus -> view.scaleX = if (hasFocus) 1.15f else 1f; view.scaleY = if (hasFocus) 1.15f else 1f }
            bubble.setOnClickListener { category.action() }
            homeOrbitRoot.addView(bubble)
            bubbles.add(bubble)

            val satellitePoints = orbitSatelliteIcons[category.label].orEmpty()
            val satelliteSize = (bubbleSize * 0.43f).toInt().coerceAtLeast(dp(24))
            val satelliteDistance = bubbleSize * 0.98f
            val satellitesForCategory = mutableListOf<View>()
            satellitePoints.forEachIndexed { satelliteIndex, iconRes ->
                val satelliteAngle = angle + (-0.95 + satelliteIndex * 0.63)
                val distance = satelliteDistance + if (satelliteIndex % 2 == 0) dp(5) else 0
                val sx = bx + distance * Math.cos(satelliteAngle).toFloat()
                val sy = by + distance * Math.sin(satelliteAngle).toFloat()
                val satellite = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(satelliteSize, satelliteSize).apply {
                        leftMargin = (sx - satelliteSize / 2f).toInt()
                        topMargin = (sy - satelliteSize / 2f).toInt()
                    }
                    setImageResource(iconRes)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setPadding(dp(6), dp(6), dp(6), dp(6))
                    background = orbitalSatelliteDrawable(category.color)
                    foreground = ovalFocusRing()
                    alpha = 0.9f
                    isFocusable = true
                    isClickable = true
                    setOnFocusChangeListener { view, hasFocus ->
                        view.scaleX = if (hasFocus) 1.16f else 1f
                        view.scaleY = if (hasFocus) 1.16f else 1f
                    }
                    setOnClickListener { category.action() }
                }
                homeOrbitRoot.addView(satellite)
                satellitesForCategory += satellite
                satelliteLinks += PointF(bx, by) to PointF(sx, sy)
            }
            satelliteGroups += satellitesForCategory

            val caption = TextView(this).apply {
                text = "${category.sublabel}\n${category.label}"
                gravity = Gravity.CENTER
                setTextColor(0xFFE3ECFF.toInt())
                textSize = 11f
                letterSpacing = 0.04f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = FrameLayout.LayoutParams(dp(90), -2).apply {
                    val captionAbove = by < centerY
                    leftMargin = (bx - dp(45)).toInt()
                    topMargin = if (captionAbove) (by - bubbleSize / 2f - dp(38)).toInt() else (by + bubbleSize / 2f + dp(4)).toInt()
                }
            }
            homeOrbitRoot.addView(caption)
        }
        orbitBubbles = bubbles
        orbitSatelliteGroups = satelliteGroups
        orbitSatelliteParents = satelliteGroups.flatMapIndexed { index, group ->
            group.map { it to bubbles[index] }
        }.toMap()
        orbitLines.setPoints(PointF(centerX, centerY), points, satelliteLinks)
        orbitCenterCard?.post {
            if (homeMode && currentFocus == null) orbitCenterCard?.requestFocus()
        }
    }

    private fun ovalFocusRing(): android.graphics.drawable.Drawable {
        val selector = android.graphics.drawable.StateListDrawable()
        val focused = GradientDrawable().apply { shape = GradientDrawable.OVAL; setStroke(dp(3), Color.WHITE) }
        selector.addState(intArrayOf(android.R.attr.state_focused), focused)
        selector.addState(intArrayOf(), GradientDrawable().apply { shape = GradientDrawable.OVAL })
        return selector
    }

    private fun openRadios() {
        switchRadio()
    }

    private fun openSportsDayCategory() {
        if (!databaseBackedCatalog) {
            Toast.makeText(this, "Catálogo ainda carregando, tente novamente em instantes.", Toast.LENGTH_SHORT).show()
            return
        }
        repository.queryGroups(MediaKind.LIVE, hiddenGroups(), includeAdult = false) { groups ->
            runOnUiThread {
                fun normalized(value: String): String = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                    .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                    .lowercase(Locale.ROOT)
                    .trim()
                val match = groups.firstOrNull { normalized(it) == "jogos do dia" || normalized(it) == "jogos dos dias" }
                    ?: groups.firstOrNull { normalized(it).contains("jogos") && normalized(it).contains("dia") }
                    ?: groups.firstOrNull { normalized(it).contains("jogos") }
                    ?: groups.firstOrNull { group ->
                        val value = normalized(group)
                        value.contains("futebol") || value.contains("esporte") || value.contains("sport")
                    }
                if (match == null) {
                    Toast.makeText(this, "A pasta Jogos do Dia não foi encontrada em Canais.", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                switchSection(MediaKind.LIVE, autoSelectFirst = false)
                selectedCategory = match
                renderCategories()
                renderCatalog()
                selectFirstVisible()
            }
        }
    }

    private fun openCategoryByKeywords(keywords: List<String>, label: String) {
        if (!databaseBackedCatalog) { Toast.makeText(this, "Catálogo ainda carregando, tente novamente em instantes.", Toast.LENGTH_SHORT).show(); return }
        val hidden = hiddenGroups()
        fun tryKind(kind: MediaKind, onMiss: () -> Unit) {
            repository.queryGroups(kind, hidden, includeAdult = false) { groups ->
                runOnUiThread {
                    val match = groups.firstOrNull { group -> keywords.any { keyword -> group.contains(keyword, ignoreCase = true) } }
                    if (match != null) {
                        switchSection(kind, autoSelectFirst = false)
                        selectedCategory = match
                        renderCategories()
                        renderCatalog()
                        selectFirstVisible()
                    } else {
                        onMiss()
                    }
                }
            }
        }
        tryKind(MediaKind.MOVIE) {
            tryKind(MediaKind.SERIES) {
                val message = if (catalogImportInProgress) {
                    "Ainda carregando \"$label\" (o catálogo está sendo importado) — tente de novo em alguns segundos."
                } else {
                    "Nenhuma categoria de \"$label\" encontrada no seu catálogo."
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun normalizeHomeCategory(value: String): String =
        java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .trim()
            .lowercase()

    private fun openHomeSeriesCategory(category: HomeSeriesCategory) {
        if (!databaseBackedCatalog) {
            Toast.makeText(this, "Catálogo ainda carregando, tente novamente em instantes.", Toast.LENGTH_SHORT).show()
            return
        }
        val label = when (category) {
            HomeSeriesCategory.DORAMAS -> "Doramas"
            HomeSeriesCategory.TURKISH_NOVELAS -> "Novelas Turcas"
            HomeSeriesCategory.NOVELAS -> "Novelas"
            HomeSeriesCategory.REELSHORTS -> "Reelshorts"
            HomeSeriesCategory.ANIMES -> "Animes / Crunchyroll"
        }
        repository.queryGroups(MediaKind.SERIES, hiddenGroups(), includeAdult = false) { groups ->
            runOnUiThread {
                val match = groups.firstOrNull { group -> matchesHomeSeriesCategory(group, category) }
                if (match == null) {
                    val message = if (category == HomeSeriesCategory.ANIMES) {
                        "Nenhuma categoria da Crunchyroll foi encontrada no catálogo."
                    } else if (catalogImportInProgress) {
                        "Ainda carregando \"$label\" — tente novamente em alguns segundos."
                    } else {
                        "Nenhuma categoria de \"$label\" encontrada no seu catálogo."
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                switchSection(MediaKind.SERIES, autoSelectFirst = false)
                selectedCategory = match
                renderCategories()
                renderCatalog()
                selectFirstVisible()
            }
        }
    }

    private fun matchesHomeSeriesCategory(group: String, category: HomeSeriesCategory): Boolean {
        val value = normalizeHomeCategory(group)
        val isTurkish = listOf("turca", "turcas", "turco", "turcos", "turkish", "turk").any(value::contains)
        val isNovel = value.contains("novela")
        val isDorama = value.contains("dorama") || value.contains("k-drama") || value.contains("k drama")
        return when (category) {
            HomeSeriesCategory.DORAMAS -> isDorama && !isTurkish
            // O kind já está restrito a SERIES; portanto, basta o grupo ser
            // identificado como turco, mesmo que o provedor o nomeie apenas
            // como "Turcas" ou "Turkish" sem escrever "Novelas".
            HomeSeriesCategory.TURKISH_NOVELAS -> isTurkish
            HomeSeriesCategory.NOVELAS -> isNovel && !isTurkish && !isDorama
            HomeSeriesCategory.REELSHORTS -> value.contains("reelshort") || value == "shorts" || value.contains("shorts")
            // O pedido é que ANIMES sempre entre pela aba Crunchyroll; não
            // aceitar outra categoria de anime evita abrir uma pasta errada.
            HomeSeriesCategory.ANIMES -> value.contains("crunchyroll")
        }
    }

    private fun openFeaturedEntry(entry: CatalogEntry) {
        suppressAutoSelectFirst = true
        switchSection(entry.kind, autoSelectFirst = false)
        handleEntryClick(entry)
        mainHandler.postDelayed({ suppressAutoSelectFirst = false }, 4_000L)
    }

    private fun clearPreviewForSection(kind: MediaKind) {
        heroImage.setImageResource(R.drawable.future_main_background_3d)
        previewLogo.visibility = View.GONE
        liveBadge.visibility = View.GONE
        videoPreviewText.text = when (kind) {
            MediaKind.LIVE -> "Selecione um canal para ver o preview"
            MediaKind.MOVIE -> "Selecione um filme para ver o trailer"
            MediaKind.SERIES -> "Selecione uma série para ver as temporadas"
        }
        detailEyebrow.text = when (kind) {
            MediaKind.LIVE -> "CANAIS AO VIVO"
            MediaKind.MOVIE -> "FILMES"
            MediaKind.SERIES -> "SÉRIES"
        }
        detailChannelName.text = when (kind) {
            MediaKind.LIVE -> "Nenhum canal selecionado"
            MediaKind.MOVIE -> "Nenhum filme selecionado"
            MediaKind.SERIES -> "Nenhuma série selecionada"
        }
        detailTags.text = ""
        nowCard.visibility = if (kind == MediaKind.LIVE) View.VISIBLE else View.GONE
        detailDescription.text = when (kind) {
            MediaKind.LIVE -> "Selecione um canal para visualizar os detalhes."
            MediaKind.MOVIE -> "Selecione um filme para visualizar arte, metadados e sinopse."
            MediaKind.SERIES -> "Nenhuma série foi encontrada nesta lista do painel."
        }
        nowLabel.text = "DETALHES"
        currentProgram.text = ""
        currentProgramDescription.text = ""
        epgUpcoming.removeAllViews()
        epgUpcoming.visibility = View.GONE
        programTime.text = ""
        nextProgram.text = ""
        nextProgram.visibility = View.GONE
        actionRow.removeAllViews()
    }

    private fun switchSection(kind: MediaKind, autoSelectFirst: Boolean = true) {
        playConstellationTransition()
        parentalUnlocked = false
        radioMode = false
        voiceMode = false
        settingsMode = false
        settingsPanel?.let { (it.parent as? ViewGroup)?.removeView(it) }
        settingsPanel = null
        radioDialog?.dismiss()
        seriesEpisodesDialog?.dismiss()
        seriesSeasonsDialog?.dismiss()
        stopMiniPlayer()
        selectedEntry = null
        focusCatalogWhenReady = false
        clearPreviewForSection(kind)
        homeMode = false
        homePanel.visibility = View.GONE
        setSidebarBrandVisible(false)
        showInternalShell()
        setInternalCategoryVisibility(true)
        vodSection.visibility = View.GONE
        vodCards.removeAllViews()
        favoritesOnly = false
        currentKind = kind
        categoryRequestId++
        liveHeader.text = when (kind) {
            MediaKind.LIVE -> "◉  Live TV"
            MediaKind.MOVIE -> "◉  Filmes"
            MediaKind.SERIES -> "◉  Séries"
        }
        selectedCategory = "Todos"
        query = ""
        searchHint.text = "⌕"
        channelHeading.text = when (kind) {
            MediaKind.LIVE -> "CANAIS AO VIVO"
            MediaKind.MOVIE -> "FILMES"
            MediaKind.SERIES -> "SÉRIES"
        }
        renderNavigation()
        renderCategories()
        renderCatalog()
        if (autoSelectFirst) selectFirstVisible()
        if (!databaseBackedCatalog) restoreCachedCatalogIfNeeded()
        categoryList.post { focusFirstCategory() }
    }

    private fun switchFavorites() {
        playConstellationTransition()
        parentalUnlocked = false
        radioMode = false
        voiceMode = false
        settingsMode = false
        settingsPanel?.let { (it.parent as? ViewGroup)?.removeView(it) }
        settingsPanel = null
        radioDialog?.dismiss()
        seriesEpisodesDialog?.dismiss()
        seriesSeasonsDialog?.dismiss()
        stopMiniPlayer()
        selectedEntry = null
        focusCatalogWhenReady = false
        clearPreviewForSection(MediaKind.LIVE)
        vodSection.visibility = View.GONE
        vodCards.removeAllViews()
        homeMode = false
        homePanel.visibility = View.GONE
        setSidebarBrandVisible(false)
        showInternalShell()
        setInternalCategoryVisibility(true)
        favoritesOnly = true
        categoryRequestId++
        liveHeader.text = "◉  Favoritos"
        selectedCategory = "Todos"
        query = ""
        searchHint.text = "⌕"
        channelHeading.text = "FAVORITOS"
        renderNavigation()
        renderCategories()
        renderCatalog()
        selectFirstVisible()
        if (!databaseBackedCatalog) restoreCachedCatalogIfNeeded()
        categoryList.post { focusFirstCategory() }
    }

    private fun renderCategories() {
        if (radioMode) {
            renderCategoryButtons(listOf("Todos") + radioRepository.categories())
            return
        }
        val favoritesPill = if (favoritesOnly) emptyList() else listOf(FAVORITES_CATEGORY_LABEL)
        if (databaseBackedCatalog) {
            val requestKind = currentKind
            val requestId = ++categoryRequestId
            val cachedGroups = categoryCache[requestKind].orEmpty()
            categoryList.removeAllViews()
            renderCategoryButtons(favoritesPill + listOf("Todos") + cachedGroups)
            repository.queryGroups(requestKind, hiddenGroups(), includeAdult = true) { groups ->
                runOnUiThread {
                    if (!databaseBackedCatalog || currentKind != requestKind || requestId != categoryRequestId) return@runOnUiThread
                    val normalizedGroups = groups.map { it.ifBlank { "Sem categoria" } }.distinct()
                    val freshGroups = normalizedGroups.filterNot { it == ContentSafety.LOCKED_CATEGORY }.sorted() +
                        normalizedGroups.filter { it == ContentSafety.LOCKED_CATEGORY }
                    if (freshGroups == cachedGroups) return@runOnUiThread
                    categoryCache[requestKind] = freshGroups
                    renderCategoryButtons(favoritesPill + listOf("Todos") + freshGroups)
                    if (currentFocus == null || isWithin(currentFocus, navItems)) categoryList.post { focusFirstCategory() }
                }
            }
            return
        }
        renderCategoryButtons(favoritesPill + listOf("Todos") + currentItems().map { it.groupTitle.ifBlank { "Sem categoria" } }.distinct().sorted())
    }

    private fun repaintCategorySelection() {
        for (i in 0 until categoryList.childCount) {
            val view = categoryList.getChildAt(i) as? TextView ?: continue
            val category = view.text.toString()
            val active = category == selectedCategory
            view.setTextColor(if (active) Color.rgb(122, 255, 224) else Color.rgb(178, 207, 237))
            view.background = categoryChipDrawable(active, view.hasFocus())
        }
    }

    private fun categoryChipDrawable(active: Boolean, focused: Boolean): GradientDrawable = GradientDrawable().apply {
        val fill = when {
            active -> 0x4A1F6F86
            focused -> 0x3A2A5875
            else -> 0x2615223D
        }
        setColor(Color.argb((fill shr 24 and 0xFF).toInt(), (fill shr 16 and 0xFF).toInt(), (fill shr 8 and 0xFF).toInt(), (fill and 0xFF).toInt()))
        cornerRadius = 20f
        when {
            active -> setStroke(2, Color.rgb(68, 238, 255))
            focused -> setStroke(1, Color.rgb(119, 210, 255))
        }
    }

    private fun paintSortButtons() {
        listOf(sortRecentButton to SortMode.RECENT, sortAlphaButton to SortMode.ALPHABETICAL, sortRatingButton to SortMode.RATING).forEach { (button, mode) ->
            val active = sortMode == mode
            button.setTextColor(if (active) Color.rgb(232, 250, 255) else Color.rgb(170, 196, 228))
            button.background = categoryChipDrawable(active, button.hasFocus())
        }
    }

    private fun applySortMode(mode: SortMode) {
        if (sortMode == mode) return
        sortMode = mode
        getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).edit().putString(PREF_SORT_ALPHA, mode.name).apply()
        paintSortButtons()
        if (mode == SortMode.RATING) {
            applyRatingSort()
        } else if (!radioMode) {
            renderCatalog()
            selectFirstVisible()
        }
    }

    // "Nota" não dá pra ordenar o catálogo inteiro (o TMDB é uma API externa
    // buscada por nome, um item de cada vez -- inviável para catálogos com
    // dezenas/centenas de milhares de itens). Em vez disso, busca um lote já
    // carregável de itens, pega a nota de cada um, e mostra ordenado por nota
    // -- fora do sistema normal de paginação incremental (que já sofreu bugs
    // de posição de rolagem antes; essa ação é um resultado fixo/único, não
    // uma lista que cresce ao rolar).
    private fun applyRatingSort() {
        if (!databaseBackedCatalog) { Toast.makeText(this, "Ordenar por nota não disponível para esta lista.", Toast.LENGTH_SHORT).show(); return }
        Toast.makeText(this, "Buscando notas no TMDB...", Toast.LENGTH_SHORT).show()
        val hidden = hiddenGroups()
        val requestId = ++ratingSortRequestId
        repository.queryPage(currentKind, selectedCategory, query, hidden, emptySet(), SortMode.RECENT, 60, 0, seriesOnly = currentKind == MediaKind.SERIES, includeAdult = parentalUnlocked) { batch ->
            runOnUiThread {
                if (requestId != ratingSortRequestId || batch.isEmpty()) return@runOnUiThread
                val rated = arrayOfNulls<Pair<CatalogEntry, Double>>(batch.size)
                var pending = batch.size
                batch.forEachIndexed { index, item ->
                    repository.fetchTmdbRating(item) { rating ->
                        runOnUiThread {
                            if (requestId != ratingSortRequestId) return@runOnUiThread
                            rated[index] = item to (rating?.score ?: -1.0)
                            pending--
                            if (pending == 0) {
                                val sorted = rated.filterNotNull().sortedByDescending { it.second }.map { it.first }
                                pagedItems.clear()
                                pagedItems.addAll(sorted)
                                pageFinished = true
                                catalogAdapter.submit(sorted, selectedEntry?.key)
                                channelList.scrollToPosition(0)
                                if (sorted.isNotEmpty()) selectEntry(sorted.first(), false)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderCategoryButtons(categories: List<String>) {
        categoryList.removeAllViews()
        categories.forEach { category ->
            val item = TextView(this).apply {
                id = View.generateViewId()
                text = category
                gravity = Gravity.CENTER
                textSize = 12f
                isFocusable = true
                isClickable = true
                setPadding(dp(14), dp(8), dp(14), dp(8))
                layoutParams = LinearLayout.LayoutParams(-2, dp(46)).apply { setMargins(dp(4), dp(7), dp(4), dp(7)) }
                setOnClickListener {
                    val applyCategory = {
                        if (selectedCategory == ContentSafety.LOCKED_CATEGORY && category != ContentSafety.LOCKED_CATEGORY) {
                            parentalUnlocked = false
                            selectedEntry = null
                            clearPreviewForSection(currentKind)
                        }
                        selectedCategory = category
                        repaintCategorySelection()
                        renderCatalog()
                        selectFirstVisible()
                    }
                    if (category == ContentSafety.LOCKED_CATEGORY && !parentalUnlocked) {
                        requestAdultAccess(applyCategory)
                    } else {
                        applyCategory()
                    }
                }
                setOnFocusChangeListener { view, hasFocus ->
                    view.background = categoryChipDrawable(category == selectedCategory, hasFocus)
                }
            }
            item.setTextColor(if (category == selectedCategory) Color.rgb(122, 255, 224) else Color.rgb(178, 207, 237))
            item.background = categoryChipDrawable(category == selectedCategory, item.hasFocus())
            categoryList.addView(item)
        }
        configureExplicitFocusGraph()
        if (focusCategoryWhenReady) {
            focusCategoryWhenReady = false
            categoryList.post { focusFirstCategory() }
        }
    }

    private fun renderCatalog() {
        if (radioMode || !databaseBackedCatalog) {
            catalogAdapter.submit(visibleItems(), selectedEntry?.key)
            channelList.post { configureExplicitFocusGraph() }
            return
        }
        pageRequestId++
        pagedItems.clear()
        pageLoading = false
        pageFinished = false
        // Não chama catalogAdapter.submit(emptyList()) aqui: isso deixava a
        // lista visivelmente vazia por um instante até a primeira página do
        // banco responder, e era a causa real do "piscar". A lista antiga
        // fica na tela até loadNextPage() trazer os dados novos e trocar de
        // uma vez só.
        loadNextPage()
    }

    private fun loadNextPage() {
        if (!databaseBackedCatalog || pageLoading || pageFinished) return
        pageLoading = true
        val requestId = pageRequestId
        val offset = pagedItems.size
        val favoritesPillActive = selectedCategory == FAVORITES_CATEGORY_LABEL
        repository.queryPage(
            kind = if (favoritesOnly) null else currentKind,
            group = if (favoritesPillActive) "Todos" else selectedCategory,
            search = query,
            hidden = hiddenGroups(),
            favorites = if (favoritesOnly || favoritesPillActive) favorites() else emptySet(),
            sortMode = sortMode,
            limit = pageSize,
            offset = offset,
            seriesOnly = currentKind == MediaKind.SERIES && !favoritesOnly && !favoritesPillActive,
            includeAdult = parentalUnlocked,
        ) { page ->
            runOnUiThread {
                if (requestId != pageRequestId) return@runOnUiThread
                pageLoading = false
                if (page.isEmpty()) {
                    // Durante a importação o SQLite pode estar entre dois
                    // commits. Não transformar esse intervalo transitório em
                    // uma tela permanentemente vazia; tenta novamente quando
                    // o próximo lote estiver disponível.
                    if (catalogImportStillRunning()) {
                        pageFinished = false
                        mainHandler.postDelayed({
                            if (requestId == pageRequestId && !pageLoading) loadNextPage()
                        }, 1_200L)
                        return@runOnUiThread
                    }
                    pageFinished = true
                    if (offset == 0) {
                        selectedEntry = null
                        clearPreviewForSection(currentKind)
                        catalogAdapter.submit(emptyList(), null)
                    }
                    return@runOnUiThread
                }
                pagedItems.addAll(page)
                if (offset == 0) {
                    val layoutManager = channelList.layoutManager as? LinearLayoutManager
                    val anchorPosition = layoutManager?.findFirstVisibleItemPosition()?.takeIf { it != RecyclerView.NO_POSITION }
                    val anchorOffset = anchorPosition?.let { layoutManager.findViewByPosition(it)?.top }
                    catalogAdapter.submit(pagedItems.toList(), selectedEntry?.key)
                    channelList.post {
                        configureExplicitFocusGraph()
                        // Restaura a posição de rolagem em vez de deixar a lista pular
                        // pro início toda vez que essa página inicial é resubmetida.
                        if (anchorPosition != null && anchorPosition < pagedItems.size) {
                            layoutManager?.scrollToPositionWithOffset(anchorPosition, anchorOffset ?: 0)
                        }
                    }
                val browsingCatalog = isWithin(currentFocus, channelList) || isWithin(currentFocus, categoryList)
                if (!suppressAutoSelectFirst && !browsingCatalog && (selectedEntry == null || pagedItems.none { it.key == selectedEntry?.key })) selectEntry(page.first(), false)
                if (focusCatalogWhenReady) {
                        focusCatalogWhenReady = false
                        channelList.post { focusFirstCatalogItem() }
                    }
                } else {
                    catalogAdapter.append(page)
                    channelList.post { configureExplicitFocusGraph() }
                }
                if (page.size < pageSize) pageFinished = true
            }
        }
    }

    private fun currentItems(): List<CatalogEntry> {
        if (radioMode) {
            val safeRadios = radioEntries.filter { parentalUnlocked || !ContentSafety.isAdult(it) }
            if (selectedCategory == "Todos") return safeRadios
            return safeRadios.filter { it.groupTitle.ifBlank { "Rádios" } == selectedCategory }
        }
        if (favoritesOnly) return catalog.entries.filter { it.key in favorites() && !isHidden(it.groupTitle) && (parentalUnlocked || !ContentSafety.isAdult(it)) }
        return catalog.entries.filter { it.kind == currentKind && !isHidden(it.groupTitle) && (parentalUnlocked || !ContentSafety.isAdult(it)) }
    }

    private fun visibleItems(): List<CatalogEntry> {
        var result = currentItems()
        if (selectedCategory == FAVORITES_CATEGORY_LABEL) {
            val favKeys = favorites()
            result = result.filter { it.key in favKeys }
        } else if (selectedCategory != "Todos") {
            result = if (selectedCategory == ContentSafety.LOCKED_CATEGORY) {
                result.filter { parentalUnlocked && ContentSafety.isAdult(it) }
            } else {
                result.filter { it.groupTitle.ifBlank { "Sem categoria" } == selectedCategory }
            }
        }
        if (query.isNotBlank()) {
            val normalized = query.trim().lowercase()
            result = result.filter { item ->
                item.name.lowercase().contains(normalized) ||
                    item.groupTitle.lowercase().contains(normalized) ||
                    item.tvgId.lowercase().contains(normalized)
            }
        }
        return if (sortMode == SortMode.ALPHABETICAL) result.sortedBy { it.name.lowercase() } else result
    }

    private fun remoteSyncEnabled(): Boolean = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).getBoolean(PREF_REMOTE_SYNC, true)

    private fun hasParentalPin(): Boolean = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE)
        .getString(PREF_PARENTAL_PIN_HASH, "").orEmpty().isNotBlank()

    private fun hashPin(pin: String): String = MessageDigest.getInstance("SHA-256")
        .digest(pin.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun requestAdultAccess(onGranted: () -> Unit) {
        if (parentalUnlocked) {
            onGranted()
            return
        }
        val prefs = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE)
        val storedHash = prefs.getString(PREF_PARENTAL_PIN_HASH, "").orEmpty()
        if (storedHash.isBlank()) {
            showParentalPinEditor(onCreated = onGranted)
            return
        }
        val input = EditText(this).apply {
            hint = "PIN de 4 dígitos"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setSingleLine(true)
        }
        AlertDialog.Builder(this)
            .setTitle("Conteúdo protegido")
            .setMessage("Digite o PIN parental para exibir esta categoria.")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Desbloquear") { _, _ ->
                if (hashPin(input.text.toString()) != storedHash) {
                    Toast.makeText(this, "PIN parental incorreto", Toast.LENGTH_LONG).show()
                } else {
                    parentalUnlocked = true
                    onGranted()
                }
            }
            .show()
    }

    private fun showParentalPinEditor(onCreated: (() -> Unit)? = null) {
        val input = EditText(this).apply {
            hint = "Crie um PIN de 4 dígitos"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setSingleLine(true)
        }
        AlertDialog.Builder(this)
            .setTitle("Configurar PIN parental")
            .setMessage("O conteúdo adulto ficará na última categoria e só será exibido após este PIN. Não use 0000.")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar PIN") { _, _ ->
                val pin = input.text.toString()
                if (!pin.matches(Regex("\\d{4}")) || pin == "0000") {
                    Toast.makeText(this, "Use exatamente 4 números diferentes de 0000", Toast.LENGTH_LONG).show()
                } else {
                    getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).edit()
                        .putString(PREF_PARENTAL_PIN_HASH, hashPin(pin))
                        .apply()
                    parentalUnlocked = true
                    Toast.makeText(this, "PIN parental configurado", Toast.LENGTH_SHORT).show()
                    onCreated?.invoke()
                }
            }
            .show()
    }

    private fun showParentalControlDialog() {
        val configured = hasParentalPin()
        val state = if (configured) {
            "PIN configurado. O conteúdo protegido permanece oculto até o desbloqueio."
        } else {
            "Nenhum PIN configurado. O conteúdo protegido continuará bloqueado até você criar um PIN."
        }
        AlertDialog.Builder(this)
            .setTitle("Controle parental")
            .setMessage(state)
            .setNegativeButton("Fechar", null)
            .setNeutralButton(if (configured) "Bloquear agora" else "Criar PIN") { _, _ ->
                if (configured) {
                    parentalUnlocked = false
                    selectedCategory = "Todos"
                    categoryCache.clear()
                    renderCategories()
                    renderCatalog()
                    Toast.makeText(this, "Conteúdo protegido bloqueado", Toast.LENGTH_SHORT).show()
                } else {
                    showParentalPinEditor()
                }
            }
            .setPositiveButton(if (configured) "Alterar PIN" else "Configurar PIN") { _, _ ->
                if (configured) requestAdultAccess { showParentalPinEditor() } else showParentalPinEditor()
            }
            .show()
    }

    private fun selectFirstVisible() {
        if (radioMode) {
            visibleItems().firstOrNull()?.let { selectEntry(it, false) }
            return
        }
        if (databaseBackedCatalog) {
            val requestId = pageRequestId
            val favoritesPillActive = selectedCategory == FAVORITES_CATEGORY_LABEL
            repository.queryPage(
                kind = if (favoritesOnly) null else currentKind,
                group = if (favoritesPillActive) "Todos" else selectedCategory,
                search = query,
                hidden = hiddenGroups(),
                favorites = if (favoritesOnly || favoritesPillActive) favorites() else emptySet(),
                sortMode = sortMode,
                limit = 1,
                offset = 0,
                seriesOnly = currentKind == MediaKind.SERIES && !favoritesOnly && !favoritesPillActive,
                includeAdult = parentalUnlocked,
                ) { page ->
                runOnUiThread { if (requestId == pageRequestId) page.firstOrNull()?.let { selectEntry(it, false) } }
            }
            return
        }
        visibleItems().firstOrNull()?.let { selectEntry(it, false) }
    }

    private fun handleEntryClick(entry: CatalogEntry) {
        if (ContentSafety.isAdult(entry) && !parentalUnlocked) {
            requestAdultAccess { handleEntryClick(entry) }
            return
        }
        recordWatch(entry)
        val sameEntry = miniPlayerEntryKey == entry.key
        if (isSeriesRootEntry(entry)) {
            selectEntry(entry, true)
            return
        }
        if (entry.kind == MediaKind.LIVE && sameEntry && previewMode == PreviewMode.CONTENT) {
            expandMiniPlayer()
            return
        }
        selectEntry(entry, true)
        if (entry.kind == MediaKind.LIVE) {
            recordChannelWatch(entry)
            startMiniPlayer(entry)
        }
        // Filmes e séries permanecem sem mini player: o card apenas seleciona
        // o conteúdo e o botão principal abre a reprodução em tela cheia.
    }

    private fun startContentPreview(entry: CatalogEntry) {
        if (entry.kind != MediaKind.LIVE) {
            openEntry(entry)
            return
        }
        if (entry.streamUrl.isBlank()) {
            Toast.makeText(this, "Este item não possui o filme/episódio disponível na lista do painel", Toast.LENGTH_SHORT).show()
            return
        }
        startMiniPlayer(entry, entry.streamUrl, entry.name)
        // O segundo clique é o comando de reprodução: abrir imediatamente em tela cheia.
        videoPreview.post {
            if (miniPlayerEntryKey == entry.key && previewMode == PreviewMode.CONTENT) expandMiniPlayer()
        }
    }


    // Moldura de TV antiga desativada a pedido do usuário (ficava pequena e
    // "flutuando"). Função mantida (sem uso) caso seja reativada no futuro;
    // por enquanto o conteúdo sempre preenche o preview inteiro.
    private object TvFrameScreen {
        const val LEFT = 0.1477f
        const val TOP = 0.1678f
        const val WIDTH = 0.5890f
        const val HEIGHT = 0.6085f
    }

    private fun fitContentToTvFrame(content: View) {
        val params = (content.layoutParams as? FrameLayout.LayoutParams) ?: FrameLayout.LayoutParams(-1, -1)
        params.width = -1
        params.height = -1
        params.gravity = Gravity.NO_GRAVITY
        params.leftMargin = 0
        params.topMargin = 0
        content.layoutParams = params
    }

    private fun startMiniPlayer(entry: CatalogEntry, sourceUrl: String = entry.streamUrl, previewTitle: String = entry.name, mode: PreviewMode = PreviewMode.CONTENT) {
        if (entry.kind != MediaKind.LIVE) {
            openEntry(entry)
            return
        }
        if (sourceUrl.isBlank()) {
            Toast.makeText(this, "Este item não possui uma transmissão válida", Toast.LENGTH_SHORT).show()
            return
        }
        stopMiniPlayer()
        previewScale = PreviewScale.STRETCH
        val playerView = PlayerView(this).apply {
            useController = false
            controllerShowTimeoutMs = 0
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            isClickable = true
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
        }
        videoPreview.addView(playerView, 1)
        fitContentToTvFrame(playerView)
        if (radioMode && entry.kind == MediaKind.LIVE) {
            radioVisualizer = RadioWaveView(this).apply {
                layoutParams = FrameLayout.LayoutParams(-1, -1)
            }
            videoPreview.addView(radioVisualizer, minOf(4, videoPreview.childCount))
        }
        playerView.setOnClickListener { handleEntryClick(entry) }
        val player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player.setMediaItem(MediaItem.fromUri(sourceUrl))
        player.prepare()
        val prefs = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE)
        player.playWhenReady = prefs.getBoolean(PREF_AUTOPLAY, true)
        miniPlayer = player
        miniPlayerView = playerView
        miniPlayerEntryKey = entry.key
        previewMode = mode
        showPreviewScaleControl()
        heroImage.visibility = View.GONE
        previewLogo.visibility = View.GONE
        liveBadge.visibility = if (entry.kind == MediaKind.LIVE) View.VISIBLE else View.GONE
        videoPreviewText.text = "Mini player • $previewTitle"
    }

    private fun scheduleTrailerPreview(entry: CatalogEntry) {
        // Se já é o mesmo item que estava agendado (e ainda dentro da janela
        // de espera, ou já revelado), não reinicia o cronômetro do zero --
        // isso é o que fazia o trailer nunca aparecer: o foco "piscava" de
        // novo no mesmo item com frequência (comum enquanto a importação em
        // segundo plano ainda está rodando e reconstrói linhas da lista),
        // cancelando o cronômetro de 5s repetidamente antes dele completar.
        if (trailerFocusEntryKey == entry.key && (miniTrailerView == null || miniPlayerEntryKey == entry.key)) return
        trailerFocusToken++
        val token = trailerFocusToken
        trailerFocusEntryKey = entry.key
        if (entry.kind != MediaKind.MOVIE && entry.kind != MediaKind.SERIES) {
            if (miniTrailerView != null) stopMiniPlayer()
            return
        }
        if (miniTrailerView != null && miniPlayerEntryKey != entry.key) stopMiniPlayer()
        if (miniTrailerView != null && miniPlayerEntryKey == entry.key && previewMode == PreviewMode.TRAILER) return
        // Começa a preparar o YouTube imediatamente, mas deixa a área invisível
        // durante a janela de cinco segundos do foco.
        startTrailerPreview(entry, revealImmediately = false)
        mainHandler.postDelayed({
            if (token != trailerFocusToken || trailerFocusEntryKey != entry.key || selectedEntry?.key != entry.key) return@postDelayed
            if (miniPlayerEntryKey == entry.key && previewMode == PreviewMode.TRAILER) {
                revealTrailerPreview(entry)
            } else {
                startTrailerPreview(entry, revealImmediately = true)
            }
        }, TRAILER_FOCUS_DELAY_MS)
    }

    private fun startTrailerPreview(entry: CatalogEntry, revealImmediately: Boolean = true) {
        if (entry.kind != MediaKind.MOVIE && entry.kind != MediaKind.SERIES) return
        val knownTrailer = entry.trailerUrl.trim().ifBlank { enrichedMetadata[entry.key]?.trailer.orEmpty().trim() }
        videoPreview.visibility = if (revealImmediately) View.VISIBLE else View.INVISIBLE
        val trailer = knownTrailer
        if (trailer.isBlank()) {
            startYoutubeTrailerSearchPreview(entry)
        } else if (trailer.contains("youtube.com", true) || trailer.contains("youtu.be", true)) {
            startYoutubeTrailerPreview(entry, trailer)
        } else {
            // URL de vídeo direto também pode ser usado como trailer, mas sem
            // transformar filmes/séries em mini player de canal.
            startVODTrailerWebView(entry, trailer)
        }
        if (!revealImmediately) {
            miniTrailerView?.visibility = View.INVISIBLE
            videoPreview.visibility = View.INVISIBLE
        }
    }

    private fun revealTrailerPreview(entry: CatalogEntry) {
        if (miniPlayerEntryKey != entry.key || previewMode != PreviewMode.TRAILER) return
        videoPreview.visibility = View.VISIBLE
        miniTrailerView?.visibility = View.VISIBLE
        miniTrailerView?.alpha = 1f
        heroImage.visibility = View.GONE
        liveBadge.visibility = View.VISIBLE
        liveBadge.text = "TRAILER"
        showPreviewScaleControl()
    }

    private fun startVODTrailerWebView(entry: CatalogEntry, url: String) {
        stopMiniPlayer()
        val webView = createYoutubeWebView()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                super.onPageFinished(view, pageUrl)
                view?.alpha = 1f
            }
        }
        registerTrailerView(entry, webView)
        webView.loadUrl(url)
    }


    private fun startYoutubeTrailerSearchPreview(entry: CatalogEntry) {
        stopMiniPlayer()
        val query = Uri.encode("${entry.name} trailer oficial")
        val webView = createYoutubeWebView()
        var resolved = false
        fun resolveFirstResult(view: WebView, attempt: Int) {
            if (resolved) return
            view.evaluateJavascript(
                """(function(){var a=[...document.querySelectorAll('a')].map(function(x){return x.href||''}).find(function(h){return h.indexOf('/watch?v=')>=0});if(a)return a;var m=document.documentElement.innerHTML.match(/"videoId":"([A-Za-z0-9_-]{11})/);return m?m[1]:'';})()""",
            ) { rawValue ->
                val candidate = rawValue.trim().trim('"').replace("\\/", "/").replace("\\u0026", "&")
                val videoId = youtubeVideoId(candidate) ?: candidate.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
                if (!resolved && videoId != null) {
                    resolved = true
                    view.alpha = 1f
                    loadYoutubeVideoPage(view, videoId)
                } else if (attempt < 10) {
                    view.postDelayed({ resolveFirstResult(view, attempt + 1) }, 700L)
                }
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view == null || url?.contains("youtube", true) != true) return
                if (resolved) {
                    view.postDelayed({ hideYoutubeChrome(view); if (trailerAudioEnabled()) enableYoutubeAudio(view) }, 1_200L)
                } else {
                    view.postDelayed({ resolveFirstResult(view, 0) }, 1_200L)
                }
            }
        }
        webView.alpha = 0f
        registerTrailerView(entry, webView)
        webView.loadUrl("https://m.youtube.com/results?search_query=$query")
    }

    private fun createYoutubeWebView(): WebView = WebView(this).apply {
        setBackgroundColor(Color.BLACK)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        webChromeClient = WebChromeClient()
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.setSupportMultipleWindows(false)
        settings.allowContentAccess = true
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 12; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        isFocusable = false
        overScrollMode = View.OVER_SCROLL_NEVER
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        layoutParams = FrameLayout.LayoutParams(-1, -1)
    }

    private fun youtubeVideoPageUrl(videoId: String): String {
        val safeId = videoId.replace(Regex("[^A-Za-z0-9_-]"), "")
        // Usa a pagina "watch" normal em vez do endpoint /embed/: o embed falha
        // (tela de "video indisponivel") em qualquer video que o dono tenha
        // desativado para incorporacao em outros sites -- bem comum em
        // trailers oficiais de estudio. A pagina watch sempre funciona; a
        // interface extra do YouTube e escondida via CSS (hideYoutubeChrome).
        return "https://www.youtube.com/watch?v=$safeId&autoplay=1&playsinline=1&mute=0&rel=0"
    }

    private fun loadYoutubeVideoPage(webView: WebView, videoId: String) {
        webView.loadUrl(youtubeVideoPageUrl(videoId), mapOf("Referer" to "https://www.youtube.com/"))
    }

    private fun enableYoutubeAudio(webView: WebView, attempt: Int = 0) {
        if (webView.parent == null || attempt > 12) return
        webView.evaluateJavascript(
            """(function(){
                var v=document.querySelector('video');
                if(v){v.muted=false;v.defaultMuted=false;v.volume=1.0;var p=v.play();if(p&&p.catch)p.catch(function(){});}
                var b=document.querySelector("button[aria-label*='Unmute'],button[aria-label*='Ativar som'],button.ytp-mute-button");
                if(b&&/unmute|ativar som|sound/i.test(b.getAttribute('aria-label')||''))b.click();
                return !!v;
            })()""".trimIndent(),
        ) {
            hideYoutubeSoundOverlay(webView)
            val objectFit = when (previewScale) {
                PreviewScale.NORMAL -> "contain"
                PreviewScale.STRETCH -> "fill"
                PreviewScale.ZOOM -> "cover"
            }
            applyYoutubeVideoScale(webView, objectFit, if (previewScale == PreviewScale.ZOOM) 1.18f else 1f)
            webView.postDelayed({ enableYoutubeAudio(webView, attempt + 1) }, if (attempt < 3) 700L else 1_500L)
        }
    }

    private fun applyYoutubeVideoScale(webView: WebView, objectFit: String, scale: Float) {
        webView.evaluateJavascript(
            """(function(){var s=document.getElementById('future-video-scale');if(!s){s=document.createElement('style');s.id='future-video-scale';document.head.appendChild(s);}s.textContent='video{object-fit:$objectFit !important;transform:scale($scale);transform-origin:center center;}';})()""".trimIndent(),
            null,
        )
    }

    private fun hideYoutubeSoundOverlay(webView: WebView) {
        webView.evaluateJavascript(
            """(function(){
                var selectors='[aria-label*="Unmute" i],[aria-label*="Ativar som" i],[aria-label*="ativar o som" i],.ytp-unmute,.ytp-mute-button,.ytp-volume-panel';
                document.querySelectorAll(selectors).forEach(function(e){e.style.display='none';e.style.visibility='hidden';});
                document.querySelectorAll('body *').forEach(function(e){var t=(e.innerText||'').trim().toLowerCase();if(t&&t.length<42&&(t.indexOf('toque para ativar som')>=0||t.indexOf('tap to unmute')>=0||t==='ativar som'||t==='unmute')){e.style.display='none';e.style.visibility='hidden';}});
            })()""".trimIndent(),
            null,
        )
        hideYoutubeChrome(webView)
    }

    // Esconde tudo da pagina do YouTube que nao seja o video em si (cabecalho,
    // nome do canal, inscrever-se, curtidas, banner "Abrir app", comentarios,
    // sugestoes) e faz o player ocupar o espaco inteiro.
    private fun hideYoutubeChrome(webView: WebView) {
        webView.evaluateJavascript(
            """(function(){
                var s=document.getElementById('future-hide-chrome');
                if(!s){s=document.createElement('style');s.id='future-hide-chrome';document.head.appendChild(s);}
                s.textContent='ytm-mobile-topbar-renderer,#masthead-container,.mobile-topbar-header,'+
                    'ytm-app-promo-renderer,.ytm-mealbar-promo-renderer,ytm-mealbar-promo-renderer,'+
                    '#below,ytm-watch-below-the-player-renderer,.slim-video-metadata-renderer,'+
                    'ytm-slim-owner-renderer,.watch-below-the-player,#comments,ytm-comments-entry-point-header-renderer,'+
                    'ytm-item-section-renderer,#related,.ytm-video-metadata-renderer,.miniplayer-toggle-button-container,'+
                    'ytm-video-action-bar-renderer,.video-ads,.ytp-ce-element,.ytp-pause-overlay,.ytp-endscreen-content,'+
                    'ytm-single-column-watch-next-results-renderer,ytm-watch-metadata'+
                    '{display:none !important;}'+
                    'html,body{background:#000 !important;overflow:hidden !important;margin:0 !important;padding:0 !important;}';
                // Em vez de depender de nomes de classe/ID do YouTube (que mudam com
                // frequencia), sobe a partir do proprio elemento <video> -- que sempre
                // existe -- forcando ele e alguns niveis de pai a ocupar a tela toda.
                // Isso e o que faz o video realmente preencher o espaco, nao so
                // esconder o resto da pagina.
                var v=document.querySelector('video');
                if(v){
                    var el=v;
                    for(var i=0;i<6&&el&&el!==document.body;i++){
                        el.style.setProperty('position','fixed','important');
                        el.style.setProperty('top','0','important');
                        el.style.setProperty('left','0','important');
                        el.style.setProperty('width','100vw','important');
                        el.style.setProperty('height','100vh','important');
                        el.style.setProperty('max-width','none','important');
                        el.style.setProperty('max-height','none','important');
                        el.style.setProperty('margin','0','important');
                        el.style.setProperty('z-index','999999','important');
                        el=el.parentElement;
                    }
                    v.style.setProperty('object-fit','contain','important');
                }
            })()""".trimIndent(),
            null,
        )
    }

    private fun registerTrailerView(entry: CatalogEntry, webView: WebView) {
        webView.isClickable = true
        webView.setOnClickListener { handleEntryClick(entry) }
        videoPreview.addView(webView, 1)
        fitContentToTvFrame(webView)
        miniTrailerView = webView
        miniPlayerEntryKey = entry.key
        previewMode = PreviewMode.TRAILER
        previewScale = PreviewScale.STRETCH
        showPreviewScaleControl()
        heroImage.visibility = View.GONE
        previewLogo.visibility = View.GONE
        liveBadge.visibility = View.VISIBLE
        liveBadge.text = "TRAILER"
        videoPreviewText.text = if (entry.kind == MediaKind.SERIES) {
            "▶  Abrir série • ${seriesTitle(entry)}"
        } else {
            "▶  Assistir filme • ${entry.name}"
        }
        if (selectedEntry?.key == entry.key) renderActions(entry)
    }

    private fun startEmbeddedTrailerPreview(entry: CatalogEntry, url: String) {
        val videoId = youtubeVideoId(url)
        if (videoId.isNullOrBlank()) {
            startYoutubeTrailerSearchPreview(entry)
            return
        }
        stopMiniPlayer()
        val webView = createYoutubeWebView()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view != null && url?.contains("youtube", true) == true) view.postDelayed({ hideYoutubeChrome(view); if (trailerAudioEnabled()) enableYoutubeAudio(view) }, 1_000L)
            }
        }
        loadYoutubeVideoPage(webView, videoId)
        registerTrailerView(entry, webView)
    }

    private fun youtubeVideoId(value: String): String? {
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
        val pathSegments = uri.pathSegments
        val rawId = when {
            uri.host?.contains("youtu.be", true) == true -> pathSegments.firstOrNull()
            uri.getQueryParameter("v").orEmpty().isNotBlank() -> uri.getQueryParameter("v")
            pathSegments.indexOfFirst { it.equals("shorts", true) } >= 0 -> pathSegments.getOrNull(pathSegments.indexOfFirst { it.equals("shorts", true) } + 1)
            pathSegments.indexOfFirst { it.equals("embed", true) } >= 0 -> pathSegments.getOrNull(pathSegments.indexOfFirst { it.equals("embed", true) } + 1)
            value.matches(Regex("[A-Za-z0-9_-]{11}")) -> value
            else -> null
        }
        return rawId?.replace(Regex("[^A-Za-z0-9_-]"), "")?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
    }

    private fun startYoutubeTrailerPreview(entry: CatalogEntry, trailerUrl: String) {
        val videoId = youtubeVideoId(trailerUrl)
        if (videoId.isNullOrBlank()) {
            startYoutubeTrailerSearchPreview(entry)
            return
        }
        startEmbeddedTrailerPreview(entry, "https://www.youtube-nocookie.com/embed/$videoId")
    }

    private fun expandMiniPlayer() {
        val content = (miniPlayerView ?: miniTrailerView) ?: return
        val player = miniPlayer
        val playerView = content as? PlayerView
        val expandedEntry = selectedEntry
        (content.parent as? ViewGroup)?.removeView(content)
        lateinit var dialog: Dialog
        val fullScreen = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(content, FrameLayout.LayoutParams(-1, -1))
        }
        val backButton = TextView(this).apply {
            text = "VOLTAR"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 12f
            isFocusable = true
            isClickable = true
            setPadding(dp(16), 0, dp(16), 0)
            elevation = dp(8).toFloat()
            background = rounded(0xCC101827, 10f)
        }
        fullScreen.addView(backButton, FrameLayout.LayoutParams(-2, dp(48), Gravity.TOP or Gravity.END).apply {
            setMargins(dp(24), dp(24), dp(24), 0)
        })
        val hideBackButton = Runnable { if (dialog.isShowing) backButton.visibility = View.GONE }
        fun revealBackButton() {
            backButton.visibility = View.VISIBLE
            backButton.removeCallbacks(hideBackButton)
            backButton.postDelayed(hideBackButton, 3_000L)
        }
        backButton.setOnClickListener { dialog.dismiss() }
        backButton.setOnFocusChangeListener { view, hasFocus ->
            view.background = rounded(if (hasFocus) 0xFF2BFFB0 else 0xCC101827, 10f)
            (view as TextView).setTextColor(if (hasFocus) Color.rgb(5, 6, 10) else Color.WHITE)
        }
        if (playerView != null) {
            playerView.useController = true
            playerView.controllerShowTimeoutMs = 4_000
            playerView.isFocusable = true
            playerView.setOnClickListener {
                playerView.showController()
                revealBackButton()
            }
            playerView.setOnKeyListener { _, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER,
                    -> {
                        if (player?.isPlaying == true) player.pause() else player?.play()
                        playerView.showController()
                        revealBackButton()
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        dialog.dismiss()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_DPAD_UP,
                    -> backButton.requestFocus()
                    else -> false
                }
            }
        } else {
            content.setOnTouchListener { _, _ ->
                revealBackButton()
                false
            }
        }
        revealBackButton()
        dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            setContentView(fullScreen)
            setOnDismissListener {
                (content.parent as? ViewGroup)?.removeView(content)
                if (playerView != null) {
                    playerView.useController = false
                    playerView.setOnKeyListener(null)
                    playerView.setOnClickListener { expandedEntry?.let { handleEntryClick(it) } }
                }
                videoPreview.addView(content, 1)
                fitContentToTvFrame(content)
                miniPlayerDialog = null
            }
        }
        miniPlayerDialog = dialog
        dialog.show()
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        dialog.window?.decorView?.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        if (playerView != null) {
            playerView.showController()
            playerView.requestFocus()
        } else {
            backButton.requestFocus()
        }
        player?.playWhenReady = true
    }

    private fun stopMiniPlayer() {
        miniPlayerDialog?.setOnDismissListener(null)
        miniPlayerDialog?.dismiss()
        miniPlayerDialog = null
        miniPlayerView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        miniPlayerView = null
        miniTrailerView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.stopLoading()
            it.loadUrl("about:blank")
            it.destroy()
        }
        miniTrailerView = null
        radioVisualizer?.let { visualizer ->
            (visualizer.parent as? ViewGroup)?.removeView(visualizer)
            visualizer.stopAnimation()
        }
        radioVisualizer = null
        previewScaleButton.visibility = View.GONE
        previewScale = PreviewScale.STRETCH
        miniPlayer?.release()
        miniPlayer = null
        miniPlayerEntryKey = null
        previewMode = PreviewMode.NONE
        if (::heroImage.isInitialized) heroImage.visibility = View.VISIBLE
        if (::previewLogo.isInitialized) previewLogo.visibility = View.GONE
    }

    private fun selectEntry(entry: CatalogEntry, requestFocus: Boolean) {
        if (ContentSafety.isAdult(entry) && !parentalUnlocked) {
            if (requestFocus) requestAdultAccess { selectEntry(entry, true) }
            return
        }
        if (requestFocus && selectedEntry?.key != entry.key) stopMiniPlayer()
        selectedEntry = entry
        if (::catalogAdapter.isInitialized) catalogAdapter.setSelectedKey(entry.key)
        val editorial = editorialFor(entry)
        val epgPrograms = epgProgramsFor(entry)
        val epgProgram = currentEpgProgram(epgPrograms)
        val isLive = entry.kind == MediaKind.LIVE
        val isSeriesRoot = isSeriesRootEntry(entry)
        videoPreviewText.text = when {
            isLive -> "AO VIVO  •  ${entry.name}"
            isSeriesRoot -> "SÉRIE  •  ${seriesTitle(entry)}"
            entry.kind == MediaKind.MOVIE -> "FILME  •  ${entry.name}"
            else -> "CONTEÚDO  •  ${entry.name}"
        }
        val metadata = enrichedMetadata[entry.key]
        val heroSource = metadata?.backdrop?.takeIf { it.isNotBlank() } ?: entry.backdropUrl.ifBlank { entry.logoUrl }
        heroImage.setImageResource(fallbackHero(entry))
        if (heroSource.isBlank()) {
            heroImage.setImageResource(fallbackHero(entry))
        } else {
            imageLoader.load(heroSource, heroImage, fallbackHero(entry))
        }
        previewLogo.visibility = View.GONE
        liveBadge.visibility = if (isLive) View.VISIBLE else View.GONE
        liveBadge.text = "AO VIVO"
        if (!isLive) {
            stopMiniPlayer()
            videoPreview.visibility = View.VISIBLE
            previewScaleButton.visibility = View.GONE
        } else {
            videoPreview.visibility = View.VISIBLE
        }
        detailEyebrow.text = if (isLive) editorial.eyebrow.uppercase() else kindLabel(entry.kind)
        detailChannelName.text = if (isSeriesRoot) seriesTitle(entry) else entry.name
        detailTags.text = listOf(entry.groupTitle, metadata?.year?.ifBlank { entry.year } ?: entry.year, entry.quality, kindLabel(entry.kind), entry.runtime)
            .filter { it.isNotBlank() }.joinToString("   •   ")
        nowCard.visibility = if (isLive) View.VISIBLE else View.GONE
        val synopsis = metadata?.synopsis?.takeIf { it.isNotBlank() } ?: entry.synopsis
        detailDescription.text = if (isLive) editorial.description else displaySynopsis(synopsis).ifBlank {
            if (entry.kind == MediaKind.MOVIE || entry.kind == MediaKind.SERIES) "Buscando sinopse..." else "Sinopse não informada na lista do painel."
        }
        nowLabel.text = "AGORA"
        currentProgram.text = if (isLive) epgProgram?.title ?: editorial.currentProgram else ""
        currentProgramDescription.text = if (isLive) epgProgram?.description?.ifBlank { null } ?: editorial.currentDescription else ""
        programTime.text = if (isLive) epgProgram?.let { "${formatTime(it.start)} – ${formatTime(it.stop)}" } ?: editorial.time else ""
        renderUpcomingEpg(epgPrograms, epgProgram, isLive)
        nextProgram.visibility = if (isLive && epgPrograms.isEmpty()) View.VISIBLE else View.GONE
        nextProgram.text = if (isLive) {
            epgPrograms.dropWhile { it !== epgProgram }.drop(1).firstOrNull()?.let { "A seguir  •  ${it.title}  •  ${formatTime(it.start)}" } ?: editorial.nextProgram
        } else if (isSeriesRoot) "☷  Abrir temporadas" else "▶  Abrir em tela cheia"
        renderActions(entry)
        if (entry.kind == MediaKind.MOVIE || entry.kind == MediaKind.SERIES) enrichEntryMetadata(entry)
        // Do not submit/replace the adapter from a focus-change callback. The
        // CatalogAdapter invokes selectEntry when a row gains focus; rebuilding
        // the RecyclerView here destroys the focused ViewHolder and Android then
        // falls back to the sidebar. Data is submitted only by renderCatalog/loadNextPage.
        if (requestFocus) {
            channelList.post {
                configureExplicitFocusGraph()
                if (!focusSelectedCatalogItem()) {
                    // O item pode ainda estar sendo anexado por paginação. Tenta
                    // novamente o mesmo item, nunca o primeiro card.
                    val selectedKey = selectedEntry?.key
                    channelList.postDelayed({
                        if (selectedEntry?.key == selectedKey) focusSelectedCatalogItem()
                    }, 160L)
                }
            }
        } else {
            channelList.post { configureExplicitFocusGraph() }
        }
    }

    private fun enrichEntryMetadata(entry: CatalogEntry) {
        var answered = false
        if (entry.synopsis.isBlank() && (entry.kind == MediaKind.MOVIE || entry.kind == MediaKind.SERIES)) {
            mainHandler.postDelayed({
                if (!answered && selectedEntry?.key == entry.key && detailDescription.text.toString() == "Buscando sinopse...") {
                    detailDescription.text = "Sinopse não encontrada (${repository.lastMetadataDebug}). Buscando ainda em segundo plano..."
                }
            }, 15_000L)
        }
        repository.enrichMetadata(entry) { metadata ->
            runOnUiThread {
                answered = true
                if (metadata != null && (metadata.synopsis.isNotBlank() || metadata.year.isNotBlank() || metadata.backdrop.isNotBlank() || metadata.trailer.isNotBlank())) {
                    enrichedMetadata[entry.key] = metadata
                }
                if (selectedEntry?.key != entry.key) return@runOnUiThread
                if (metadata != null && metadata.synopsis.isNotBlank()) {
                    detailDescription.text = displaySynopsis(metadata.synopsis)
                } else if (entry.synopsis.isNotBlank()) {
                    detailDescription.text = displaySynopsis(entry.synopsis)
                } else if (entry.kind == MediaKind.MOVIE || entry.kind == MediaKind.SERIES) {
                    detailDescription.text = "Sinopse não encontrada (${repository.lastMetadataDebug})."
                }
                if (metadata != null && (metadata.year.isNotBlank() || metadata.backdrop.isNotBlank())) {
                    val parts = listOf(entry.groupTitle, metadata.year.ifBlank { entry.year }, entry.quality, kindLabel(entry.kind), entry.runtime)
                        .filter { it.isNotBlank() }
                    detailTags.text = parts.joinToString("   •   ")
                    if (metadata.backdrop.isNotBlank()) imageLoader.load(metadata.backdrop, heroImage, fallbackHero(entry))
                }
            }
        }
        if (entry.kind == MediaKind.MOVIE || entry.kind == MediaKind.SERIES) {
            repository.fetchTmdbRating(entry) { rating ->
                runOnUiThread {
                    if (rating == null || selectedEntry?.key != entry.key) return@runOnUiThread
                    val parts = listOf(entry.groupTitle, entry.year, entry.quality, kindLabel(entry.kind), entry.runtime, "⭐ %.1f".format(rating.score))
                        .filter { it.isNotBlank() }
                    detailTags.text = parts.joinToString("   •   ")
                }
            }
        }
    }

    private fun renderActions(entry: CatalogEntry) {
        actionRow.removeAllViews()
        val isFavorite = entry.key in favorites()
        val actions = mutableListOf<Pair<String, () -> Unit>>()
        val isSeriesRoot = isSeriesRootEntry(entry)
        val primaryLabel = when {
            isSeriesRoot -> "☷  TEMPORADAS"
            entry.kind == MediaKind.MOVIE -> "▶  ASSISTIR FILME"
            entry.kind == MediaKind.SERIES && entry.episode.isNotBlank() -> "▶  REPRODUZIR EPISÓDIO"
            entry.kind == MediaKind.LIVE -> "▶  ABRIR AO VIVO"
            else -> "▶  REPRODUZIR"
        }
        actions += primaryLabel to {
            when {
                isSeriesRoot -> showSeriesSeasonsDialog(entry)
                entry.kind == MediaKind.MOVIE || entry.kind == MediaKind.SERIES -> openEntry(entry)
                miniPlayerEntryKey == entry.key && previewMode == PreviewMode.CONTENT -> expandMiniPlayer()
                else -> startMiniPlayer(entry)
            }
        }
        if (entry.kind == MediaKind.SERIES && !isSeriesRoot) {
            actions += "☷  TEMPORADAS" to { showSeriesSeasonsDialog(entry) }
        }
        if (entry.kind == MediaKind.MOVIE) {
            actions += "ℹ  DETALHES" to { showMovieFullScreen(entry) }
        }
        actions += (if (isFavorite) "♥  FAVORITO" else "♡  FAVORITAR") to {
            toggleFavorite(entry)
            renderActions(entry)
            renderCatalog()
        }
        actions += "⌕  BUSCAR" to { showSearchDialog() }
        actions.forEachIndexed { index, (label, clickAction) ->
            val                 action = TextView(this).apply {
                    id = View.generateViewId()
                    text = label
                gravity = Gravity.CENTER
                textSize = 11f
                maxLines = 1
                isFocusable = true
                isClickable = true
                setPadding(dp(16), 0, dp(16), 0)
                setTextColor(Color.WHITE)
                elevation = dp(3).toFloat()
                background = actionButtonBackground(index == 0, false)
                setOnFocusChangeListener { view, hasFocus ->
                    view.background = actionButtonBackground(index == 0, hasFocus)
                    (view as TextView).setTextColor(Color.WHITE)
                }
                setOnClickListener { clickAction() }
                layoutParams = if (actionRow.orientation == LinearLayout.HORIZONTAL) {
                    LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(8), 0) }
                } else {
                    LinearLayout.LayoutParams(-1, dp(54)).apply { setMargins(0, 0, 0, dp(8)) }
                }
            }
            actionRow.addView(action)
        }
        configureExplicitFocusGraph()
    }


    private fun openEntry(entry: CatalogEntry) {
        if (ContentSafety.isAdult(entry) && !parentalUnlocked) {
            requestAdultAccess { openEntry(entry) }
            return
        }
        recordWatch(entry)
        if (entry.streamUrl.isBlank()) {
            Toast.makeText(this, "Este item não possui URL válida na lista do painel", Toast.LENGTH_SHORT).show()
            loadRemoteConfiguration()
            return
        }
        val prefs = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(PREF_EXTERNAL_PLAYER, false)) {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(entry.streamUrl), "video/*")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }.onSuccess { return }
                .onFailure { Toast.makeText(this, "Player externo indisponível; usando o player Future", Toast.LENGTH_SHORT).show() }
        }
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_TITLE, entry.name)
            putExtra(PlayerActivity.EXTRA_URL, entry.streamUrl)
            putExtra(PlayerActivity.EXTRA_MAC, getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).getString(PREF_MAC_ADDRESS, "").orEmpty())
        })
    }

    // Cabecalho comum (fundo com imagem, titulo, tags, sinopse, botao voltar)
    // usado tanto na tela cheia de filme quanto na de serie.
    private class DetailHeaderViews(
        val header: FrameLayout,
        val backdrop: ImageView,
        val title: TextView,
        val tags: TextView,
        val synopsis: TextView,
        val backButton: TextView,
    )

    private fun buildDetailHeader(heightDp: Int): DetailHeaderViews {
        val header = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(heightDp))
        }
        val backdrop = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        val shade = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x00000000, 0xE6070B15.toInt()))
        }
        val textBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { setMargins(dp(28), 0, dp(28), dp(20)) }
        }
        val title = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 26f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val tags = TextView(this).apply {
            setTextColor(Color.rgb(143, 155, 184))
            textSize = 12f
            setPadding(0, dp(4), 0, dp(10))
        }
        val synopsis = TextView(this).apply {
            setTextColor(Color.rgb(200, 208, 224))
            textSize = 13f
            maxLines = 5
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        textBlock.addView(title)
        textBlock.addView(tags)
        textBlock.addView(synopsis)
        val backButton = TextView(this).apply {
            text = "‹  VOLTAR"
            setTextColor(Color.WHITE)
            textSize = 12f
            isFocusable = true
            isClickable = true
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = rounded(0xCC101827, 10f)
            layoutParams = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply { setMargins(dp(20), dp(18), 0, 0) }
        }
        header.addView(backdrop)
        header.addView(shade)
        header.addView(textBlock)
        header.addView(backButton)
        return DetailHeaderViews(header, backdrop, title, tags, synopsis, backButton)
    }

    private fun fillDetailHeader(views: DetailHeaderViews, entry: CatalogEntry, displayTitle: String, fallbackImage: Int) {
        val cachedMeta = enrichedMetadata[entry.key]
        val initialSynopsis = cachedMeta?.synopsis?.takeIf { it.isNotBlank() } ?: entry.synopsis
        views.title.text = displayTitle
        views.synopsis.text = displaySynopsis(initialSynopsis).ifBlank { "Buscando sinopse..." }
        var currentYear = cachedMeta?.year?.ifBlank { entry.year } ?: entry.year
        var ratingSuffix = ""
        fun refreshTags() {
            views.tags.text = (listOf(entry.groupTitle, currentYear, kindLabel(entry.kind)).filter { it.isNotBlank() } + listOfNotNull(ratingSuffix.takeIf { it.isNotBlank() })).joinToString("   •   ")
        }
        refreshTags()
        val backdropSource = cachedMeta?.backdrop?.ifBlank { entry.backdropUrl }?.ifBlank { entry.logoUrl } ?: entry.backdropUrl.ifBlank { entry.logoUrl }
        imageLoader.load(backdropSource, views.backdrop, fallbackImage)
        var answered = initialSynopsis.isNotBlank()
        if (!answered) {
            mainHandler.postDelayed({
                if (!answered && views.synopsis.text.toString() == "Buscando sinopse...") {
                    views.synopsis.text = "Sinopse não encontrada. Tente novamente mais tarde."
                }
            }, 15_000L)
        }
        repository.enrichMetadata(entry) { metadata ->
            runOnUiThread {
                answered = true
                if (metadata != null && metadata.backdrop.isNotBlank()) imageLoader.load(metadata.backdrop, views.backdrop, fallbackImage)
                if (metadata != null && metadata.year.isNotBlank()) { currentYear = metadata.year; refreshTags() }
                when {
                    metadata != null && metadata.synopsis.isNotBlank() -> views.synopsis.text = displaySynopsis(metadata.synopsis)
                    entry.synopsis.isNotBlank() -> views.synopsis.text = displaySynopsis(entry.synopsis)
                    else -> views.synopsis.text = "Sinopse não encontrada (${repository.lastMetadataDebug})."
                }
            }
        }
        repository.fetchTmdbRating(entry) { rating ->
            runOnUiThread {
                if (rating != null) {
                    ratingSuffix = "⭐ %.1f".format(rating.score)
                    refreshTags()
                }
            }
        }
    }

    private fun showMovieFullScreen(entry: CatalogEntry) {
        if (ContentSafety.isAdult(entry) && !parentalUnlocked) {
            requestAdultAccess { showMovieFullScreen(entry) }
            return
        }
        val headerViews = buildDetailHeader(360)
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        val watchButton = TextView(this).apply {
            text = "▶  ASSISTIR"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            isFocusable = true
            isClickable = true
            background = actionButtonBackground(primary = true, focused = false)
            layoutParams = LinearLayout.LayoutParams(dp(200), dp(48)).apply { setMargins(dp(28), dp(18), dp(28), dp(8)) }
            setOnFocusChangeListener { _, hasFocus -> background = actionButtonBackground(primary = true, focused = hasFocus) }
        }
        val castLabel = TextView(this).apply {
            text = "ELENCO"
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(28), dp(14), dp(28), dp(4))
        }
        val castText = TextView(this).apply {
            text = entry.cast.ifBlank { "Elenco não informado na lista do painel." }
            setTextColor(Color.rgb(180, 188, 208))
            textSize = 12.5f
            setPadding(dp(28), 0, dp(28), dp(28))
        }
        content.addView(headerViews.header)
        content.addView(watchButton)
        content.addView(castLabel)
        content.addView(castText)
        scroll.addView(content)
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK); addView(scroll) }
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply { setContentView(root) }
        watchButton.setOnClickListener { dialog.dismiss(); openEntry(entry) }
        headerViews.backButton.setOnClickListener { dialog.dismiss() }
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) { dialog.dismiss(); true } else false
        }
        dialog.show()
        watchButton.post { watchButton.requestFocus() }
        fillDetailHeader(headerViews, entry, entry.name, fallbackHero(entry))
    }

    private fun showSeriesSeasonsDialog(entry: CatalogEntry) {
        if (ContentSafety.isAdult(entry) && !parentalUnlocked) {
            requestAdultAccess { showSeriesSeasonsDialog(entry) }
            return
        }
        seriesEpisodesDialog?.dismiss()
        seriesSeasonsDialog?.dismiss()
        val showTitle = seriesTitle(entry)
        val headerViews = buildDetailHeader(320)
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        val seasonRow = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(6) }
            isHorizontalScrollBarEnabled = false
        }
        val seasonList = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
        }
        seasonRow.addView(seasonList)
        val episodesHeading = TextView(this).apply {
            text = "EPISÓDIOS"
            setTextColor(Color.WHITE)
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(20), dp(14), dp(20), dp(6))
        }
        val episodeList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(24))
        }
        content.addView(headerViews.header)
        content.addView(seasonRow)
        content.addView(episodesHeading)
        content.addView(episodeList)
        scroll.addView(content)
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK); addView(scroll) }
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply { setContentView(root) }
        seriesSeasonsDialog = dialog
        dialog.setOnDismissListener { if (seriesSeasonsDialog === dialog) seriesSeasonsDialog = null }
        headerViews.backButton.setOnClickListener { dialog.dismiss() }
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) { dialog.dismiss(); true } else false
        }
        dialog.show()
        headerViews.backButton.post { headerViews.backButton.requestFocus() }
        fillDetailHeader(headerViews, entry, showTitle, fallbackHero(entry))

        var episodeDetails: Map<String, EpisodeDetail> = emptyMap()
        var currentSeason: String? = null

        fun episodeDetailFor(episode: CatalogEntry): EpisodeDetail? {
            val s = episode.season.ifBlank { "1" }.toIntOrNull()?.toString() ?: episode.season.ifBlank { "1" }
            val e = episode.episode.toIntOrNull()?.toString() ?: episode.episode
            return episodeDetails["$s:$e"]
        }

        fun renderEpisodes(episodes: List<CatalogEntry>) {
            episodeList.removeAllViews()
            if (episodes.isEmpty()) {
                episodeList.addView(dialogMessage("Nenhum episódio encontrado nesta temporada."))
                return
            }
            episodes.forEach { episode ->
                val detail = episodeDetailFor(episode)
                val code = episode.episode.takeIf { it.isNotBlank() }?.let { "E${it.padStart(2, '0')}" } ?: "EP"
                val episodeTitle = episode.name.removePrefix("$showTitle ").trim().ifBlank { episode.name }
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    isFocusable = true
                    isClickable = true
                    setPadding(dp(10), dp(10), dp(14), dp(10))
                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, dp(4)) }
                    background = rounded(0x14FFFFFF, 12f)
                    setOnFocusChangeListener { view, hasFocus -> view.background = rounded(if (hasFocus) 0x332BFFB0 else 0x14FFFFFF, 12f) }
                    setOnClickListener { dialog.dismiss(); openEntry(episode) }
                }
                val thumb = ImageView(this).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = LinearLayout.LayoutParams(dp(160), dp(90)).apply { marginEnd = dp(14) }
                    background = rounded(0xFF10192B, 8f)
                }
                val textCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                }
                val epTitleView = TextView(this).apply {
                    text = "$code  •  $episodeTitle"
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                val epSynopsisView = TextView(this).apply {
                    text = displaySynopsis(detail?.plot.orEmpty()).ifBlank { "Sinopse não informada." }
                    setTextColor(Color.rgb(160, 170, 192))
                    textSize = 11.5f
                    maxLines = 3
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setPadding(0, dp(4), 0, 0)
                }
                textCol.addView(epTitleView)
                textCol.addView(epSynopsisView)
                card.addView(thumb)
                card.addView(textCol)
                episodeList.addView(card)
                imageLoader.load(detail?.image.orEmpty().ifBlank { episode.logoUrl }, thumb, fallbackLogo(episode))
            }
        }

        fun loadSeason(season: String) {
            currentSeason = season
            episodeList.removeAllViews()
            episodeList.addView(dialogMessage("Carregando episódios..."))
            if (databaseBackedCatalog) {
                repository.querySeriesEpisodes(showTitle, season, selectedCategory, hiddenGroups(), includeAdult = parentalUnlocked) { episodes ->
                    runOnUiThread { if (currentSeason == season) renderEpisodes(episodes) }
                }
            } else {
                val episodes = currentItems().filter {
                    it.kind == MediaKind.SERIES && seriesTitle(it) == showTitle && (it.season.ifBlank { "1" } == season)
                }.sortedWith(compareBy({ it.episode.toIntOrNull() ?: Int.MAX_VALUE }, { it.name.lowercase() }))
                renderEpisodes(episodes)
            }
        }

        fun renderSeasons(seasons: List<String>) {
            seasonList.removeAllViews()
            if (seasons.isEmpty()) {
                episodeList.removeAllViews()
                episodeList.addView(dialogMessage("Nenhuma temporada identificada para esta série na lista do painel."))
                return
            }
            seasons.forEachIndexed { index, season ->
                val pill = TextView(this).apply {
                    text = "TEMPORADA ${season.padStart(2, '0')}"
                    isFocusable = true
                    isClickable = true
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setPadding(dp(16), dp(9), dp(16), dp(9))
                    layoutParams = LinearLayout.LayoutParams(-2, -1).apply { marginEnd = dp(8) }
                    setOnClickListener {
                        for (i in 0 until seasonList.childCount) {
                            val childView = seasonList.getChildAt(i)
                            val selected = i == seasons.indexOf(season)
                            childView.background = rounded(if (selected) 0x332BFFB0 else 0x14FFFFFF, 18f)
                            (childView as? TextView)?.setTextColor(if (selected) Color.rgb(43, 255, 176) else Color.rgb(170, 177, 199))
                        }
                        loadSeason(season)
                    }
                }
                val selected = index == 0
                pill.setTextColor(if (selected) Color.rgb(43, 255, 176) else Color.rgb(170, 177, 199))
                pill.background = rounded(if (selected) 0x332BFFB0 else 0x14FFFFFF, 18f)
                seasonList.addView(pill)
            }
            loadSeason(seasons.first())
        }

        if (databaseBackedCatalog) {
            repository.querySeriesSeasons(showTitle, selectedCategory, hiddenGroups(), includeAdult = parentalUnlocked) { seasons -> runOnUiThread { renderSeasons(seasons) } }
        } else {
            renderSeasons(currentItems().filter { it.kind == MediaKind.SERIES && seriesTitle(it) == showTitle }.map { it.season.ifBlank { "1" } }.distinct().sortedBy { it.toIntOrNull() ?: 1 })
        }

        repository.fetchSeriesEpisodeDetails(entry) { details ->
            runOnUiThread {
                if (seriesSeasonsDialog !== dialog || details.isEmpty()) return@runOnUiThread
                episodeDetails = details
                currentSeason?.let { season -> loadSeason(season) }
            }
        }
    }


    private fun createCatalogDialog(title: String, subtitle: String, onBack: (() -> Unit)?): Pair<Dialog, LinearLayout> {
        val dialog = Dialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(20))
            background = rounded(0xF00B1020, 18f)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        }
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        val titleView = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 20f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val subtitleView = TextView(this).apply {
            text = subtitle
            setTextColor(Color.rgb(143, 155, 184))
            textSize = 10f
            setPadding(0, dp(6), 0, 0)
        }
        heading.addView(titleView)
        heading.addView(subtitleView)
        header.addView(heading)
        val close = TextView(this).apply {
            text = if (onBack == null) "FECHAR" else "VOLTAR"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 10f
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(0xFF1B2036, 8f)
            isFocusable = true
            isClickable = true
            setOnClickListener {
                dialog.dismiss()
                onBack?.invoke()
            }
        }
        header.addView(close, LinearLayout.LayoutParams(-2, dp(42)).apply { setMargins(dp(10), 0, 0, 0) })
        root.addView(header)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16), 0, 0)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(list, FrameLayout.LayoutParams(-1, -2))
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        dialog.setContentView(root)
        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            val focused = dialog.window?.decorView?.findFocus()
            val buttons = (0 until list.childCount)
                .map { list.getChildAt(it) }
                .filter { it.isFocusable && it.isShown }
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    close.performClick()
                    true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    when {
                        focused === close -> buttons.firstOrNull()?.requestFocus() ?: true
                        else -> {
                            val index = buttons.indexOf(focused)
                            buttons.getOrNull(index + 1)?.requestFocus() ?: true
                        }
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    when {
                        focused === close -> true
                        else -> {
                            val index = buttons.indexOf(focused)
                            if (index <= 0) close.requestFocus() else buttons.getOrNull(index - 1)?.requestFocus() ?: true
                        }
                    }
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> close.requestFocus()
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                -> focused?.takeIf { it.isClickable && it.isEnabled }?.performClick() == true
                else -> false
            }
        }
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.72f).toInt(),
                (resources.displayMetrics.heightPixels * 0.78f).toInt(),
            )
            close.requestFocus()
        }
        return dialog to list
    }

    private fun dialogButton(label: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER_VERTICAL
        setTextColor(Color.WHITE)
        textSize = 14f
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(dp(18), dp(12), dp(18), dp(12))
        isFocusable = true
        isClickable = true
        background = rounded(0xFF161D33, 10f)
        setOnFocusChangeListener { view, hasFocus ->
            view.background = rounded(if (hasFocus) 0xFF286B7A else 0xFF161D33, 10f)
        }
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(56)).apply { setMargins(0, 0, 0, dp(8)) }
    }

    private fun dialogMessage(message: String): TextView = TextView(this).apply {
        text = message
        setTextColor(Color.rgb(170, 177, 199))
        textSize = 13f
        gravity = Gravity.CENTER
        setPadding(dp(18), dp(28), dp(18), dp(28))
        layoutParams = LinearLayout.LayoutParams(-1, -2)
    }

    private fun displaySynopsis(value: String): String {
        if (value.isBlank()) return ""
        val withBreaks = value.replace("\\n", "<br>")
        val decoded = android.text.Html.fromHtml(withBreaks, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
        return decoded
            .replace(Regex("[ \\t]{2,}"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun seriesTitle(entry: CatalogEntry): String = entry.seriesGroup.ifBlank { entry.name }

    private fun isSeriesRootEntry(entry: CatalogEntry): Boolean =
        entry.kind == MediaKind.SERIES && currentKind == MediaKind.SERIES && !favoritesOnly && seriesEpisodesDialog == null

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun showSearchDialog() {
        val input = EditText(this).apply {
            hint = "Nome do canal, filme ou série"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
            setText(query)
        }
        AlertDialog.Builder(this)
            .setTitle("Buscar no catálogo")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Buscar") { _, _ ->
                query = input.text.toString()
                searchHint.text = "⌕"
                renderCatalog()
                selectFirstVisible()
            }
            .show()
    }

    private fun loadRemoteConfiguration() {
        val prefs = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE)
        if (prefs.getString(ActivationActivity.PREF_SOURCE_MODE, ActivationActivity.SOURCE_PANEL) == ActivationActivity.SOURCE_MANUAL) {
            // No modo DNS/usuário/senha não existe cadastro por MAC no painel, então
            // pulamos appIntegration.fetchConfig (que sempre falharia e só geraria
            // um aviso sem sentido). Mas ainda é essencial configurar o estado do
            // catálogo aqui -- sem isso databaseBackedCatalog nunca vira true e a
            // tela principal nunca lê o SQLite, ficando sempre vazia mesmo com a
            // importação concluída com sucesso.
            loadManualCatalog()
            return
        }
        val mac = prefs.getString(PREF_MAC_ADDRESS, "").orEmpty()
        if (mac.isBlank()) return
        val catalogImportAlreadyStarted = intent.getBooleanExtra(EXTRA_CATALOG_IMPORT_IN_PROGRESS, false)
        appIntegration.fetchConfig(mac) { result ->
            runOnUiThread {
                result.onSuccess { config ->
                    if (!config.registered || !config.allowed) {
                        showAccessUnavailable(config)
                        return@onSuccess
                    }
                    applyRemoteConfig(config, catalogImportAlreadyStarted)
                    if (catalogImportAlreadyStarted) startCatalogImportWatcher()
                    if (remoteSyncEnabled()) appIntegration.startBackgroundSync(
                        mac = mac,
                        currentContent = { selectedEntry?.name },
                        onNotifications = { notifications -> showRemoteNotifications(mac, notifications) },
                        onCommands = { commands -> showRemoteCommands(mac, commands) },
                    )
                }.onFailure {
                    Toast.makeText(this, "Configuração remota indisponível; mantendo a última lista válida", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadManualCatalog() {
        val catalogImportAlreadyStarted = intent.getBooleanExtra(EXTRA_CATALOG_IMPORT_IN_PROGRESS, false)
        // Sem painel, não existe RemoteAppConfig real; um "vazio" basta porque
        // applyCatalogSnapshot só usa alguns campos (ex.: epgUrl) que aqui não
        // temos mesmo -- o catálogo em si já está no SQLite, importado pela
        // ActivationActivity antes de abrir esta tela.
        // Deriva a URL do EPG a partir das credenciais manuais (padrão Xtream:
        // mesma base do get.php, endpoint xmltv.php). Sem isso, epgUrl ficava
        // sempre em branco e a programação ao vivo nunca aparecia no modo DNS.
        val prefs = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE)
        val manualDns = prefs.getString(ActivationActivity.PREF_MANUAL_DNS, "").orEmpty()
        val manualUser = prefs.getString(ActivationActivity.PREF_MANUAL_USER, "").orEmpty()
        val manualPassword = prefs.getString(ActivationActivity.PREF_MANUAL_PASSWORD, "").orEmpty()
        val manualPlaylistUrl = if (manualDns.isNotBlank() && manualUser.isNotBlank() && manualPassword.isNotBlank()) {
            "${manualDns.trimEnd('/')}/get.php?username=${java.net.URLEncoder.encode(manualUser, "UTF-8")}&password=${java.net.URLEncoder.encode(manualPassword, "UTF-8")}&type=m3u_plus&output=ts"
        } else ""
        val manualEpgUrl = if (manualDns.isNotBlank() && manualUser.isNotBlank() && manualPassword.isNotBlank()) {
            "${manualDns.trimEnd('/')}/xmltv.php?username=${java.net.URLEncoder.encode(manualUser, "UTF-8")}&password=${java.net.URLEncoder.encode(manualPassword, "UTF-8")}"
        } else ""
        val emptyConfig = RemoteAppConfig(
            registered = true, allowed = true, mac = "", appId = "maximus", appName = "Future",
            status = "", expiration = "", logoUrl = "", bannerUrl = "", backgroundUrl = "",
            messageTitle = "", messageText = "", messageImageUrl = "", iconLiveTv = "", iconMovies = "",
            iconSeries = "", serverApiUrl = "", dnsUrl = "", testApiUrl = "", epgUrl = manualEpgUrl,
            playlistUrls = emptyList(), apkDownloadUrl = "", apkVersion = "",
        )
        repository.loadCached { cached ->
            runOnUiThread {
                if (cached != null && cached.totalCount > 0) {
                    applyCatalogSnapshot(cached, emptyConfig)
                    if (catalogImportAlreadyStarted && cached.totalCount < 4_000) {
                        greeting.text = "Olá, usuário  •  lista sendo atualizada..."
                    }
                } else if (catalogImportAlreadyStarted) {
                    // A Activity de ativação ainda está gravando o primeiro lote.
                    // O watcher reabre o cache assim que houver dados confirmados.
                    showCatalogUnavailable("Carregando canais, filmes e séries...")
                } else if (manualPlaylistUrl.isNotBlank()) {
                    // Instalações novas não possuem o cache da instalação anterior.
                    // Nesse caso o modo DNS não pode parar numa tela vazia: importa
                    // diretamente o get.php salvo nas preferências.
                    repository.loadIfChanged(listOf(manualPlaylistUrl)) { result ->
                        runOnUiThread {
                            result.onSuccess { loaded ->
                                applyCatalogSnapshot(loaded, emptyConfig.copy(playlistUrls = listOf(manualPlaylistUrl)))
                            }.onFailure {
                                showCatalogUnavailable("Não foi possível carregar a lista do DNS informado.")
                            }
                        }
                    }
                } else {
                    showCatalogUnavailable("Configure o DNS, usuário e senha para carregar o catálogo.")
                }
            }
        }
        if (catalogImportAlreadyStarted) startCatalogImportWatcher()
    }

    private fun applyRemoteConfig(config: RemoteAppConfig, catalogImportAlreadyStarted: Boolean = false) {
        remoteConfig = config
        brandMark.text = "FUTURE"
        brandSubtitle.text = "TV PLAYER"
        appLogo.setImageResource(R.drawable.future_logo)
        if (config.backgroundUrl.isNotBlank()) imageLoader.load(config.backgroundUrl, remoteBackground, R.drawable.future_logo)
        if (config.bannerUrl.isNotBlank()) remoteBannerUrl = config.bannerUrl
        if (config.epgUrl.isNotBlank()) remoteEpgUrl = config.epgUrl
        if (config.dnsUrl.isNotBlank() || config.serverApiUrl.isNotBlank() || config.testApiUrl.isNotBlank()) {
            getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).edit()
                .putString(PREF_SERVER_API_URL, config.dnsUrl.ifBlank { config.serverApiUrl })
                .putString(PREF_TEST_API_URL, config.testApiUrl)
                .apply()
        }
        if (config.messageTitle.isNotBlank() || config.messageText.isNotBlank()) {
            val messageKey = "${config.messageTitle}|${config.messageText}"
            val shownKey = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).getString(PREF_LAST_MESSAGE_KEY, "")
            if (messageKey != shownKey) {
                getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).edit().putString(PREF_LAST_MESSAGE_KEY, messageKey).apply()
                AlertDialog.Builder(this)
                    .setTitle(config.messageTitle.ifBlank { "Aviso" })
                    .setMessage(config.messageText)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
        if (config.playlistUrls.isNotEmpty()) {
            if (catalogImportAlreadyStarted) {
                // A ActivationActivity já iniciou a importação e confirmou o primeiro lote.
                // Ler o cache aqui evita um segundo HEAD/download/parser da mesma M3U.
                repository.loadCached { cached ->
                    runOnUiThread {
                        if (cached != null && cached.totalCount > 0) {
                            applyCatalogSnapshot(cached, config)
                            if (cached.totalCount < 4_000) {
                                greeting.text = "Olá, usuário  •  lista sendo atualizada..."
                            }
                        } else {
                            showCatalogUnavailable("O catálogo inicial está sendo preparado.")
                        }
                    }
                }
            } else {
                repository.loadIfChanged(config.playlistUrls) { result ->
                    runOnUiThread {
                        result.onSuccess { loaded -> applyCatalogSnapshot(loaded, config) }
                            .onFailure { showCatalogUnavailable("A lista do painel não está disponível nesta TV Box.") }
                    }
                }
            }
        } else {
            showCatalogUnavailable("O painel liberou o MAC, mas não enviou nenhuma lista.")
        }
        if (config.apkVersion.isNotBlank() && config.apkDownloadUrl.isNotBlank() && config.apkVersion != packageManager.getPackageInfo(packageName, 0).versionName) {
            Toast.makeText(this, "Há uma atualização disponível: ${config.apkVersion}", Toast.LENGTH_LONG).show()
        }
    }

    private fun growCatalogIfMore() {
        if (radioMode || !databaseBackedCatalog) {
            catalogAdapter.submit(visibleItems(), selectedEntry?.key)
            return
        }
        // Só acrescenta itens novos no fim da lista (usa notifyItemRangeInserted,
        // não mexe nas linhas já visíveis nem recarrega as imagens delas) --
        // por isso é seguro chamar mesmo com o usuário navegando ativamente.
        if (pageLoading) return
        pageFinished = false
        loadNextPage()
    }

    private fun applyPartialCatalogSnapshot(snapshot: CatalogSnapshot, stillLoading: Boolean) {
        if (snapshot.totalCount <= 0) return
        val wasHome = homeMode
        catalog = snapshot
        databaseBackedCatalog = snapshot.databaseBacked
        greeting.text = if (stillLoading) {
            "Olá, usuário  •  ${snapshot.totalCount} itens carregados • atualizando..."
        } else {
            "Olá, usuário  •  ${snapshot.totalCount} itens"
        }
        // Só montar a Home uma vez enquanto a importação parcial cresce. Antes,
        // homeFeaturedKey permanecia nulo até o primeiro tick e cada lote podia
        // recriar o centro, reiniciando a rotação de quatro segundos.
        if (wasHome && exactHomeHotspots.isEmpty()) {
            renderExactHomeHotspots()
        }
        if (!wasHome && !radioMode) {
            // As categorias (pílulas) ainda são reconstruídas inteiras sempre que
            // renderCategories() roda (mesmo quando o conteúdo final é igual),
            // então isso continua pausado enquanto o usuário navega nelas para
            // não derrubar o foco. Já a lista de itens agora só cresce por
            // inserção, então pode atualizar sempre, sem exceção.
            val browsingCategories = isWithin(currentFocus, categoryList)
            if (!browsingCategories) renderCategories()
            growCatalogIfMore()
            if (selectedEntry == null) selectFirstVisible()
            if (currentFocus == null) channelList.post { focusFirstCatalogItem() || focusNavigationForCurrentSection() }
        }
    }

    private fun startCatalogImportWatcher() {
        if (!catalogImportInProgress || catalogImportWatcherStarted) return
        catalogImportWatcherStarted = true
        mainHandler.post(catalogImportWatcher)
    }

    private fun applyCatalogSnapshot(snapshot: CatalogSnapshot, config: RemoteAppConfig) {
        if (snapshot.totalCount <= 0) {
            showCatalogUnavailable("A lista do painel foi recebida vazia.")
            return
        }
        catalog = snapshot
        databaseBackedCatalog = snapshot.databaseBacked
        categoryCache.clear()
        categoryRequestId++
        greeting.text = "Olá, usuário  •  ${snapshot.totalCount} itens"
        currentKind = MediaKind.LIVE
        favoritesOnly = false
        selectedCategory = "Todos"
        renderNavigation()
        renderCategories()
        renderCatalog()
        selectFirstVisible()
        if (homeMode) {
            homeFeaturedKey = null
            renderExactHomeHotspots()
        }
        loadConfiguredEpg(config.epgUrl.ifBlank { config.playlistUrls.firstOrNull().orEmpty() })
        if (currentFocus == null) channelList.post { focusFirstCatalogItem() || focusNavigationForCurrentSection() }
    }

    private fun renderVodStrip() {
        vodCards.removeAllViews()
        if (!databaseBackedCatalog) { vodSection.visibility = View.GONE; return }
        repository.queryPage(MediaKind.MOVIE, "Todos", "", hiddenGroups(), emptySet(), sortMode, 4, 0, includeAdult = parentalUnlocked) { movies ->
            runOnUiThread {
                // Não deixa a seção reservando espaço vazio (só o título, sem
                // nenhum card) enquanto os filmes ainda não carregaram -- isso
                // empurrava a lista de filmes pra baixo à toa.
                vodSection.visibility = if (movies.isEmpty()) View.GONE else View.VISIBLE
                movies.forEach { movie ->
                    val card = TextView(this).apply {
                        text = movie.name
                        gravity = Gravity.CENTER_VERTICAL
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        textSize = 10f
                        setTextColor(Color.WHITE)
                        setPadding(12, 6, 12, 6)
                        background = rounded(0x661B2036, 10f)
                        isFocusable = true
                        isClickable = true
                        layoutParams = LinearLayout.LayoutParams(150, 52).apply { setMargins(0, 0, 8, 0) }
                        setOnClickListener { selectEntry(movie, true) }
                    }
                    vodCards.addView(card)
                }
            }
        }
    }

    private fun showCatalogUnavailable(message: String) {
        if (databaseBackedCatalog || catalog.totalCount > 0) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            return
        }
        databaseBackedCatalog = false
        catalog = CatalogSnapshot(emptyList())
        categoryCache.clear()
        categoryRequestId++
        pagedItems.clear()
        selectedEntry = null
        renderCategories()
        renderCatalog()
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showAccessUnavailable(config: RemoteAppConfig) {
        AlertDialog.Builder(this)
            .setTitle("Acesso indisponível")
            .setMessage("Este dispositivo não está autorizado para Future. Verifique o MAC e o cadastro no painel.")
            .setPositiveButton("Configurar MAC") { _, _ -> showMacDialog() }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun showRemoteNotifications(mac: String, notifications: List<RemoteNotification>) {
        notifications.forEach { notification ->
            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle(notification.title.ifBlank { "Aviso" })
                    .setMessage(notification.message)
                    .setPositiveButton("OK") { _, _ -> appIntegration.ackNotification(mac, notification.id) }
                    .show()
            }
        }
    }

    private fun showRemoteCommands(mac: String, commands: List<RemoteCommand>) {
        commands.forEach { command ->
            runOnUiThread { executeRemoteCommand(mac, command) }
        }
    }

    private fun executeRemoteCommand(mac: String, command: RemoteCommand) {
        when (command.command.lowercase()) {
            "refresh_playlist" -> {
                loadRemoteConfiguration()
                appIntegration.ackCommand(mac, command.id, "executed", "Playlist atualizada pelo painel")
            }
            "switch_playlist" -> {
                val url = command.payload.optString("url")
                if (url.startsWith("http", true)) {
                    repository.load(listOf(url)) { result ->
                        result.onSuccess { loaded ->
                            runOnUiThread {
                                catalog = loaded
                                renderCategories()
                                renderCatalog()
                                selectFirstVisible()
                            }
                            appIntegration.ackCommand(mac, command.id, "executed", "Playlist alternada")
                        }.onFailure { appIntegration.ackCommand(mac, command.id, "failed", it.message.orEmpty()) }
                    }
                } else {
                    appIntegration.ackCommand(mac, command.id, "failed", "URL de playlist ausente")
                }
            }
            "update_dns" -> {
                val dns = command.payload.optString("dns", command.payload.optString("url"))
                if (dns.startsWith("http", true)) {
                    getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).edit().putString(PREF_SERVER_API_URL, dns).apply()
                    appIntegration.ackCommand(mac, command.id, "executed", "DNS aplicada")
                } else {
                    appIntegration.ackCommand(mac, command.id, "failed", "DNS ausente")
                }
            }
            "show_message" -> {
                AlertDialog.Builder(this)
                    .setTitle(command.payload.optString("title", "Aviso"))
                    .setMessage(command.payload.optString("message"))
                    .setPositiveButton("OK") { _, _ -> appIntegration.ackCommand(mac, command.id, "executed", "Mensagem exibida") }
                    .show()
            }
            "sync_access" -> {
                appIntegration.ackCommand(mac, command.id, "executed", "Acesso sincronizado")
                loadRemoteConfiguration()
            }
            "restart_player" -> appIntegration.ackCommand(mac, command.id, "executed", "Sessão pronta para reiniciar")
            else -> appIntegration.ackCommand(mac, command.id, "failed", "Comando não suportado")
        }
    }

    private fun focusFirstSettingsOption(): Boolean {
        val panel = settingsPanel as? ViewGroup ?: return false
        val scroll = panel.findViewWithTag<View>("settings_options_scroll") ?: return false
        val list = scroll as? ScrollView ?: return false
        val options = list.getChildAt(0) as? ViewGroup ?: return false
        val target = options.getChildAt(0) ?: return false
        target.isFocusable = true
        return target.requestFocus()
    }

    private fun showSettingsScreen() {
        parentalUnlocked = false
        homeMode = false
        radioMode = false
        voiceMode = false
        favoritesOnly = false
        settingsMode = true
        mainHandler.removeCallbacks(homeClockTicker)
        homePanel.visibility = View.GONE
        showInternalShell()
        setInternalCategoryVisibility(false)
        findViewById<View>(R.id.channelColumn).visibility = View.GONE
        findViewById<View>(R.id.previewScroll).visibility = View.GONE
        findViewById<View>(R.id.sideNavigation).visibility = View.VISIBLE
        setSidebarBrandVisible(false)
        renderNavigation()
        settingsPanel?.let { (it.parent as? ViewGroup)?.removeView(it) }

        val panel = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(5, 12, 30))
            elevation = dp(8).toFloat()
            layoutParams = FrameLayout.LayoutParams(-1, -1).apply { leftMargin = dp(92) }
        }
        panel.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            alpha = 0.24f
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.future_orbit_background)
            contentDescription = null
        })
        panel.addView(View(this).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            setBackgroundColor(0xC9071028.toInt())
        })

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(22), dp(30), dp(22))
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(68))
        }
        val headerText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        headerText.addView(TextView(this).apply {
            text = "CONFIGURAÇÕES"
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        headerText.addView(TextView(this).apply {
            text = "Personalize sua experiência no FUTURE"
            textSize = 12f
            setTextColor(Color.rgb(164, 198, 231))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) }
        })
        header.addView(headerText)
        header.addView(TextView(this).apply {
            text = "●  CONECTADO"
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(123, 255, 207))
            gravity = Gravity.CENTER
            background = settingsGlassDrawable(false, false)
            setPadding(dp(14), dp(9), dp(14), dp(9))
        })
        content.addView(header)

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(14) }
        }
        val optionsScroll = ScrollView(this).apply {
            tag = "settings_options_scroll"
            isFillViewport = true
            isFocusable = false
            clipToPadding = false
            layoutParams = LinearLayout.LayoutParams(0, -1, 0.46f).apply { marginEnd = dp(16) }
            setPadding(0, 0, dp(4), dp(4))
        }
        val optionsList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(-1, -2)
        }
        data class SettingOption(val icon: Int, val title: String, val subtitle: String, val action: () -> Unit)
        val options = listOf(
            SettingOption(R.drawable.ic_nav_settings, "Controle parental", "PIN e proteção de conteúdo") { showParentalControlDialog() },
            SettingOption(R.drawable.ic_nav_live, "Áudio e reprodução", "Player, trailers e tela cheia") { showPlaybackSettingsDialog() },
            SettingOption(R.drawable.ic_nav_movies, "Legendas e idioma", "Idioma e preferências de exibição") { showSubtitleLanguageDialog() },
            SettingOption(R.drawable.ic_nav_voice, "Comando de voz", "Ative a busca por voz") { startVoiceCommand() },
            SettingOption(R.drawable.ic_nav_radio, "Rádio", "Categorias e estações favoritas") { switchRadio() },
            SettingOption(R.drawable.ic_nav_live, "EPG e programação", "Agora, a seguir e próximos programas") { showEpgSettingsDialog() },
            SettingOption(R.drawable.ic_nav_favorites, "Estrelas da transição", if (transitionStarsEnabled()) "Estrelas brilhando ativadas" else "Estrelas brilhando desativadas") { },
            SettingOption(R.drawable.ic_nav_radio, "Som da transição", if (transitionSoundEnabled()) "Efeito sonoro ativado" else "Efeito sonoro desativado") { },
            SettingOption(R.drawable.ic_nav_settings, "DNS do painel", "Conexão com o servidor autorizado") { showDnsDialog() },
            SettingOption(R.drawable.ic_nav_movies, "Playlists e cache", "Listas recebidas e armazenamento local") { showPlaylistSettingsDialog() },
            SettingOption(R.drawable.ic_nav_series, "Categorias e ordem", "Categorias ocultas e organização") { showCatalogRulesDialog() },
            SettingOption(R.drawable.ic_nav_voice, "Sincronização", "Atualizações e notificações do painel") { showSyncSettingsDialog() },
            SettingOption(R.drawable.ic_nav_favorites, "Sobre o Future", "Versão e informações do aplicativo") { showAboutDialog() },
            SettingOption(R.drawable.ic_nav_settings, "Testar API do servidor", "Verifique a conexão com o painel") { showServerTestDialog() },
            SettingOption(R.drawable.ic_nav_settings, "Verificar atualização", "Procure uma nova versão do aplicativo") { checkForAppUpdate() },
            SettingOption(R.drawable.ic_nav_settings, "Sair do aplicativo", "Encerrar a sessão do Future") { showExitConfirmation() },
        )
        var firstOption: View? = null
        options.forEach { option ->
            val isStarsToggle = option.title == "Estrelas da transição"
            val isSoundToggle = option.title == "Som da transição"
            var toggle: CheckBox? = null
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isFocusable = true
                isClickable = true
                setPadding(dp(14), dp(10), dp(14), dp(10))
                layoutParams = LinearLayout.LayoutParams(-1, dp(76)).apply { setMargins(0, 0, 0, dp(8)) }
                background = settingsGlassDrawable(false, false)
                contentDescription = if (isStarsToggle) "Estrelas da transição: ${if (transitionStarsEnabled()) "ativadas" else "desativadas"}" else if (isSoundToggle) "Som da transição: ${if (transitionSoundEnabled()) "ativado" else "desativado"}" else option.title
                setOnFocusChangeListener { view, hasFocus -> view.background = settingsGlassDrawable(hasFocus, false) }
                setOnClickListener {
                    if (isStarsToggle || isSoundToggle) toggle?.isChecked = !(toggle?.isChecked ?: false) else option.action()
                }
            }
            if (firstOption == null) firstOption = row
            row.addView(ImageView(this).apply {
                setImageResource(option.icon)
                imageTintList = android.content.res.ColorStateList.valueOf(Color.rgb(208, 235, 255))
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42)).apply { marginEnd = dp(14) }
            })
            val labels = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            labels.addView(TextView(this).apply {
                text = option.title
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
            labels.addView(TextView(this).apply {
                text = option.subtitle
                textSize = 10f
                setTextColor(Color.rgb(163, 193, 225))
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) }
            })
            row.addView(labels)
            if (isStarsToggle || isSoundToggle) {
                toggle = CheckBox(this).apply {
                    isFocusable = false
                    isClickable = false
                    isChecked = if (isStarsToggle) transitionStarsEnabled() else transitionSoundEnabled()
                    buttonTintList = android.content.res.ColorStateList.valueOf(Color.rgb(125, 246, 224))
                    setOnCheckedChangeListener { _, checked ->
                        if (isStarsToggle) setTransitionStarsEnabled(checked) else setTransitionSoundEnabled(checked)
                        row.contentDescription = if (isStarsToggle) "Estrelas da transição: ${if (checked) "ativadas" else "desativadas"}" else "Som da transição: ${if (checked) "ativado" else "desativado"}"
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                }
                row.addView(toggle)
            } else row.addView(TextView(this).apply {
                text = "›"
                textSize = 27f
                setTextColor(Color.rgb(133, 232, 255))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(24), -1)
            })
            optionsList.addView(row)
        }
        optionsScroll.addView(optionsList)
        body.addView(optionsScroll)

        val summary = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = settingsGlassDrawable(false, false)
            layoutParams = LinearLayout.LayoutParams(0, -1, 0.54f)
        }
        summary.addView(TextView(this).apply {
            text = "CENTRAL DE CONTROLE"
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(122, 255, 224))
        })
        summary.addView(TextView(this).apply {
            text = "Tudo no seu ritmo"
            textSize = 28f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }
        })
        summary.addView(TextView(this).apply {
            text = "A mesma experiência Future em todas as áreas: Canais, Filmes, Séries, Favoritos e Rádios. Use as opções ao lado para ajustar sua conta e sua programação."
            textSize = 13f
            setTextColor(Color.rgb(172, 201, 231))
            setLineSpacing(dp(3).toFloat(), 1f)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
        })
        val infoRows = listOf(
            "CONTA ATIVA" to "Perfil conectado ao painel autorizado",
            "EPG" to "Programação atualizada automaticamente",
            "CONTROLE REMOTO" to "OK abre  •  Voltar retorna à Home",
            "PRIVACIDADE" to "Dados e preferências ficam neste dispositivo",
        )
        infoRows.forEach { (label, value) ->
            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(12), 0, dp(12))
                layoutParams = LinearLayout.LayoutParams(-1, -2)
            }
            info.addView(TextView(this).apply {
                text = label
                textSize = 10f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(Color.rgb(112, 221, 255))
            })
            info.addView(TextView(this).apply {
                text = value
                textSize = 12f
                setTextColor(Color.rgb(212, 229, 248))
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) }
            })
            summary.addView(info)
        }
        body.addView(summary)
        content.addView(body)
        panel.addView(content)
        settingsPanel = panel
        (findViewById<ViewGroup>(R.id.rootShell)).addView(panel)
        firstOption?.post { firstOption?.requestFocus() }
    }

    private fun settingsGlassDrawable(focused: Boolean, selected: Boolean): GradientDrawable = GradientDrawable().apply {
        val fill = when {
            selected -> 0x552D7894
            focused -> 0x4A2D6688
            else -> 0x32192C4B
        }
        setColor(Color.argb((fill shr 24 and 0xFF).toInt(), (fill shr 16 and 0xFF).toInt(), (fill shr 8 and 0xFF).toInt(), (fill and 0xFF).toInt()))
        cornerRadius = dp(16).toFloat()
        if (focused || selected) setStroke(dp(1), Color.rgb(113, 224, 255))
    }

    private fun showSettingsDialog() {
        val message = if (hasParentalPin()) "Controle parental: PIN configurado" else "Controle parental: não configurado"
        val options = listOf(
            "Controle parental (PIN)" to { showParentalControlDialog() },
            "Áudio e reprodução" to { showPlaybackSettingsDialog() },
            "Legendas e idioma" to { showSubtitleLanguageDialog() },
            "Comando de voz" to { startVoiceCommand() },
            "Rádio" to { showRadioDialog() },
            "EPG e programação" to { showEpgSettingsDialog() },
            "DNS do painel" to { showDnsDialog() },
            "Playlists e cache" to { showPlaylistSettingsDialog() },
            "Categorias ocultas e ordem" to { showCatalogRulesDialog() },
            "Sincronização e notificações" to { showSyncSettingsDialog() },
            "Testar API do servidor" to { showServerTestDialog() },
            "Verificar atualização" to { checkForAppUpdate() },
            "Sobre o Future" to { showAboutDialog() },
            "Sair do aplicativo" to { showExitConfirmation() },
        )
        val (dialog, list) = createCatalogDialog(
            title = "Configurações do Future",
            subtitle = "Use cima/baixo e pressione OK para abrir uma opção",
            onBack = null,
        )
        list.addView(dialogMessage(message))
        options.forEach { (label, action) ->
            list.addView(dialogButton(label) {
                dialog.dismiss()
                action()
            })
        }
        dialog.show()
    }

    private fun showPlaybackSettingsDialog() {
        val prefs = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE)
        val audio = CheckBox(this).apply {
            text = "Tentar áudio automático em trailers"
            isChecked = prefs.getBoolean(PREF_TRAILER_AUDIO, true)
        }
        val autoplay = CheckBox(this).apply {
            text = "Iniciar conteúdo automaticamente"
            isChecked = prefs.getBoolean(PREF_AUTOPLAY, true)
        }
        val keepScreen = CheckBox(this).apply {
            text = "Manter a tela ligada durante a reprodução"
            isChecked = prefs.getBoolean(PREF_KEEP_SCREEN, true)
        }
        val externalPlayer = CheckBox(this).apply {
            text = "Permitir abrir conteúdo em player externo"
            isChecked = prefs.getBoolean(PREF_EXTERNAL_PLAYER, false)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(4), dp(24), 0)
            addView(audio)
            addView(autoplay)
            addView(keepScreen)
            addView(externalPlayer)
        }
        AlertDialog.Builder(this)
            .setTitle("Áudio e reprodução")
            .setView(content)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                prefs.edit()
                    .putBoolean(PREF_TRAILER_AUDIO, audio.isChecked)
                    .putBoolean(PREF_AUTOPLAY, autoplay.isChecked)
                    .putBoolean(PREF_KEEP_SCREEN, keepScreen.isChecked)
                    .putBoolean(PREF_EXTERNAL_PLAYER, externalPlayer.isChecked)
                    .apply()
                Toast.makeText(this, "Preferências de reprodução salvas", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showSubtitleLanguageDialog() {
        val prefs = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE)
        val enabled = CheckBox(this).apply {
            text = "Ativar legendas quando a transmissão fornecer legenda"
            isChecked = prefs.getBoolean(PREF_SUBTITLE_ENABLE, false)
        }
        val languageLabel = TextView(this).apply {
            text = "Idioma preferido"
            setTextColor(Color.WHITE)
            setPadding(0, dp(12), 0, dp(6))
        }
        val languages = listOf("Português (Brasil)" to "pt-BR", "English" to "en", "Español" to "es")
        val languageGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }
        val selectedLanguage = prefs.getString(PREF_LANGUAGE_CODE, "pt-BR")
        languages.forEach { (label, code) ->
            languageGroup.addView(RadioButton(this).apply {
                text = label
                tag = code
                isChecked = code == selectedLanguage
                isFocusable = true
            })
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(4), dp(24), 0)
            addView(enabled)
            addView(languageLabel)
            addView(languageGroup)
        }
        AlertDialog.Builder(this)
            .setTitle("Legendas e idioma")
            .setView(content)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                val selected = languageGroup.findViewById<RadioButton>(languageGroup.checkedRadioButtonId)
                prefs.edit()
                    .putBoolean(PREF_SUBTITLE_ENABLE, enabled.isChecked)
                    .putString(PREF_LANGUAGE_CODE, selected?.tag?.toString() ?: "pt-BR")
                    .apply()
                Toast.makeText(this, "Preferências de idioma salvas", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showSyncSettingsDialog() {
        val prefs = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE)
        val enabled = CheckBox(this).apply {
            text = "Permitir sincronização automática, alertas e comandos do painel"
            isChecked = remoteSyncEnabled()
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(4), dp(24), 0)
            addView(enabled)
            addView(TextView(this@MainActivity).apply {
                text = "A lista M3U continua sendo recebida exclusivamente pelo painel e pelo MAC deste dispositivo."
                setTextColor(Color.rgb(170, 177, 199))
                setPadding(0, dp(12), 0, 0)
            })
        }
        AlertDialog.Builder(this)
            .setTitle("Sincronização e notificações")
            .setView(content)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                prefs.edit().putBoolean(PREF_REMOTE_SYNC, enabled.isChecked).apply()
                Toast.makeText(this, "Preferência de sincronização salva", Toast.LENGTH_SHORT).show()
                loadRemoteConfiguration()
            }
            .show()
    }

    private fun showEpgSettingsDialog() {
        val prefs = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE)
        val enabled = CheckBox(this).apply {
            text = "Mostrar Agora, A seguir e próximos programas"
            isChecked = prefs.getBoolean(PREF_SHOW_EPG, true)
        }
        AlertDialog.Builder(this)
            .setTitle("EPG e programação")
            .setView(enabled)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                prefs.edit().putBoolean(PREF_SHOW_EPG, enabled.isChecked).apply()
                selectedEntry?.let { selectEntry(it, false) }
            }
            .show()
    }

    private fun showPlaylistSettingsDialog() {
        val urls = remoteConfig?.playlistUrls.orEmpty()
        val message = buildString {
            append("Listas recebidas exclusivamente do painel pelo MAC.\\n\\n")
            if (urls.isEmpty()) append("Nenhuma URL de playlist foi enviada pelo painel.")
            else append("Selecione a playlist ativa abaixo. As demais permanecem como failover.\\n")
            append("\\nCache: ").append(if (catalog.databaseBacked) "SQLite paginado ativo" else "memória")
        }
        var selected = 0
        val builder = AlertDialog.Builder(this)
            .setTitle("Playlists e cache")
            .setMessage(message)
        if (urls.size > 1) {
            builder.setSingleChoiceItems(
                urls.mapIndexed { index, url -> "Lista ${index + 1}  •  ${maskUrl(url)}" }.toTypedArray(),
                0,
            ) { _, which -> selected = which }
        }
        builder
            .setPositiveButton(if (urls.size > 1) "Aplicar" else "Recarregar") { _, _ ->
                if (urls.size > 1) loadSelectedPlaylist(urls[selected]) else loadRemoteConfiguration()
            }
            .setNeutralButton("Limpar cache") { _, _ ->
                repository.clearCache()
                loadRemoteConfiguration()
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun loadSelectedPlaylist(url: String) {
        Toast.makeText(this, "Carregando playlist selecionada...", Toast.LENGTH_SHORT).show()
        repository.load(listOf(url)) { result ->
            runOnUiThread {
                result.onSuccess { snapshot ->
                    remoteConfig?.let { applyCatalogSnapshot(snapshot, it.copy(playlistUrls = listOf(url))) }
                    Toast.makeText(this, "Playlist ativa atualizada", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(this, "Não foi possível carregar a playlist selecionada", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Sair do Future?")
            .setMessage("A reprodução será encerrada e o aplicativo será fechado.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Sair") { _, _ -> finishAffinity() }
            .show()
    }

    private fun showDnsDialog() {
        Toast.makeText(this, "Carregando DNS disponíveis no painel...", Toast.LENGTH_SHORT).show()
        appIntegration.fetchDnsList { result ->
            runOnUiThread {
                result.onSuccess { dnsList ->
                    val options = dnsList.take(5).ifEmpty { listOf("DNS do painel não informado") }
                    val current = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).getString(PREF_SELECTED_DNS, "")
                    var selected = options.indexOf(current).takeIf { it >= 0 } ?: 0
                    AlertDialog.Builder(this)
                        .setTitle("DNS do painel")
                        .setSingleChoiceItems(options.toTypedArray(), selected) { _, which -> selected = which }
                        .setPositiveButton("Aplicar") { _, _ ->
                            val chosen = options.getOrNull(selected).orEmpty()
                            if (chosen.startsWith("http", true)) {
                                getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).edit().putString(PREF_SELECTED_DNS, chosen).apply()
                                Toast.makeText(this, "DNS selecionado", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }.onFailure { error ->
                    Toast.makeText(this, "DNS indisponível: ${error.message ?: "erro de conexão"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkForAppUpdate() {
        val mac = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).getString(PREF_MAC_ADDRESS, "").orEmpty()
        if (mac.isBlank()) {
            Toast.makeText(this, "MAC ainda não configurado", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Verificando atualização...", Toast.LENGTH_SHORT).show()
        appIntegration.checkUpdate(mac) { result ->
            runOnUiThread {
                result.onSuccess { update ->
                    val message = if (update.available) "Nova versão disponível: ${update.version}\\n${update.url}" else "O Future já está atualizado."
                    AlertDialog.Builder(this).setTitle("Atualização").setMessage(message).setPositiveButton("OK", null).show()
                }.onFailure { Toast.makeText(this, "Não foi possível verificar agora", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun showAboutDialog() {
        val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrDefault("1.0.0")
        AlertDialog.Builder(this)
            .setTitle("Sobre o Future")
            .setMessage("Future TV Player\\nVersão $version\\n\\nPlayer IPTV nativo para Android TV e TV Box, com ativação por MAC, playlists M3U Plus, filmes, séries, rádio, EPG e comandos de voz.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun maskUrl(url: String): String = url.replace(Regex("(username|password)=([^&]+)"), "$1=••••")

    private fun trailerAudioEnabled(): Boolean = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).getBoolean(PREF_TRAILER_AUDIO, true)

    private fun transitionPreferences() = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE)

    private fun transitionStarsEnabled(): Boolean = transitionPreferences().getBoolean(PREF_TRANSITION_STARS, true)

    private fun transitionSoundEnabled(): Boolean = transitionPreferences().getBoolean(PREF_TRANSITION_SOUND, true)

    private fun setTransitionStarsEnabled(enabled: Boolean) {
        transitionPreferences().edit().putBoolean(PREF_TRANSITION_STARS, enabled).apply()
        if (!enabled && ::constellationTransition.isInitialized) constellationTransition.stop()
    }

    private fun setTransitionSoundEnabled(enabled: Boolean) {
        transitionPreferences().edit().putBoolean(PREF_TRANSITION_SOUND, enabled).apply()
        if (!enabled) constellationSound?.let { sound -> if (sound.isPlaying) sound.pause(); sound.seekTo(0) }
    }

    private fun showRadioDialog() {
        switchRadio()
    }

    private fun switchRadio() {
        playConstellationTransition()
        parentalUnlocked = false
        pageRequestId++
        pagedItems.clear()
        pageLoading = false
        pageFinished = true
        radioDialog?.dismiss()
        settingsPanel?.let { (it.parent as? ViewGroup)?.removeView(it) }
        settingsPanel = null
        radioMode = true
        voiceMode = false
        settingsMode = false
        favoritesOnly = false
        homeMode = false
        seriesEpisodesDialog?.dismiss()
        seriesSeasonsDialog?.dismiss()
        stopMiniPlayer()
        selectedEntry = null
        currentKind = MediaKind.LIVE
        selectedCategory = "Todos"
        query = ""
        radioEntries = radioRepository.allStations().map { station ->
            CatalogEntry(
                key = "radio:${station.id}",
                name = station.name,
                groupTitle = station.category.ifBlank { "Rádios" },
                tvgId = station.id,
                logoUrl = station.logoUrl,
                streamUrl = station.streamUrl,
                kind = MediaKind.LIVE,
                quality = "RÁDIO",
            )
        }
        homePanel.visibility = View.GONE
        showInternalShell()
        setInternalCategoryVisibility(true)
        vodSection.visibility = View.GONE
        liveHeader.text = "◉  Rádios"
        channelHeading.text = "RÁDIO ONLINE"
        searchHint.text = "⌕"
                renderNavigation()
        renderCategories()
        renderCatalog()
        selectFirstVisible()
        categoryList.post { focusFirstCategory() }
    }
    private fun startVoiceCommand() {
        voiceMode = true
        renderNavigation()
        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_VOICE_PERMISSION)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga: abrir filmes, séries, canais, rádio ou buscar")
        }
        runCatching { startActivityForResult(intent, REQUEST_VOICE) }
            .onFailure { Toast.makeText(this, "Reconhecimento de voz indisponível neste dispositivo", Toast.LENGTH_LONG).show() }
    }

    private fun handleVoiceCommand(spoken: String) {
        voiceMode = false
        renderNavigation()
        val raw = spoken.trim()
        val command = raw.lowercase(Locale.ROOT)
        when {
            command.contains("rádio") || command.contains("radio") -> {
                showRadioDialog()
                Toast.makeText(this, "Comando: $spoken", Toast.LENGTH_SHORT).show()
            }
            command.contains("filme") -> {
                openVoiceSectionSearch(MediaKind.MOVIE, extractVoiceTitle(raw))
            }
            command.contains("série") || command.contains("serie") -> {
                openVoiceSectionSearch(MediaKind.SERIES, extractVoiceTitle(raw))
            }
            command.contains("canal") -> {
                switchSection(MediaKind.LIVE)
                Toast.makeText(this, "Comando: $spoken", Toast.LENGTH_SHORT).show()
            }
            command.contains("favorito") -> {
                switchFavorites()
                Toast.makeText(this, "Comando: $spoken", Toast.LENGTH_SHORT).show()
            }
            command.contains("início") || command.contains("inicio") || command.contains("home") -> {
                showHome()
                Toast.makeText(this, "Comando: $spoken", Toast.LENGTH_SHORT).show()
            }
            command.contains("buscar") || command.contains("pesquisar") -> {
                showSearchDialog()
                Toast.makeText(this, "Comando: $spoken", Toast.LENGTH_SHORT).show()
            }
            else -> openBySpokenName(raw)
        }
    }

    private fun extractVoiceTitle(spoken: String): String {
        val marker = Regex("(?i)\\b(filme|filmes|série|series|séries)\\b").find(spoken)
        if (marker == null) return spoken.trim()
        return spoken.substring(marker.range.last + 1)
            .trim()
            .replace(Regex("(?i)^(de|do|da|o|a)\\s+"), "")
            .trim()
    }

    private fun openVoiceSectionSearch(kind: MediaKind, title: String) {
        val term = title.trim()
        if (term.isBlank()) {
            switchSection(kind)
            return
        }
        switchSection(kind, autoSelectFirst = false)
        selectedCategory = "Todos"
        query = term
        searchHint.text = "⌕  $term"
        renderCategories()
        renderCatalog()
        categoryList.post { focusFirstCategory() }
        Toast.makeText(this, "Buscando ${kindLabel(kind).lowercase(Locale.ROOT)}: $term", Toast.LENGTH_SHORT).show()
    }

    // Diz um nome (canal, filme ou série) e o app abre direto, seja qual for a
    // tela em que o usuário estiver -- busca nos três tipos de conteúdo de
    // uma vez, não só na seção atual.
    private fun openBySpokenName(spokenQuery: String) {
        if (spokenQuery.isBlank()) return
        if (!databaseBackedCatalog) {
            query = spokenQuery
            searchHint.text = "⌕"
            if (radioMode) showRadioDialog() else { renderCatalog(); selectFirstVisible() }
            Toast.makeText(this, "Comando: $spokenQuery", Toast.LENGTH_SHORT).show()
            return
        }
        val hidden = hiddenGroups()
        val kinds = listOf(MediaKind.LIVE, MediaKind.MOVIE, MediaKind.SERIES)
        val found = arrayOfNulls<CatalogEntry>(kinds.size)
        var pending = kinds.size
        fun finish() {
            pending--
            if (pending > 0) return
            val match = found.firstNotNullOfOrNull { it }
            if (match != null) {
                suppressAutoSelectFirst = true
                switchSection(match.kind, autoSelectFirst = false)
                handleEntryClick(match)
                mainHandler.postDelayed({ suppressAutoSelectFirst = false }, 4_000L)
                Toast.makeText(this, "Abrindo \"${match.name}\"", Toast.LENGTH_SHORT).show()
            } else {
                query = spokenQuery
                searchHint.text = "⌕"
                if (radioMode) showRadioDialog() else { renderCatalog(); selectFirstVisible() }
                Toast.makeText(this, "Nenhum resultado para \"$spokenQuery\"", Toast.LENGTH_SHORT).show()
            }
        }
        kinds.forEachIndexed { index, kind ->
            repository.queryPage(kind, "Todos", spokenQuery, hidden, emptySet(), SortMode.RECENT, 1, 0, includeAdult = parentalUnlocked) { results ->
                runOnUiThread {
                    found[index] = results.firstOrNull()
                    finish()
                }
            }
        }
    }

    private fun showCatalogRulesDialog() {
        val groupInput = EditText(this).apply {
            hint = "Ex.: ADULTOS, RADIOS"
            setSingleLine(false)
            setText(hiddenGroups().joinToString(", "))
        }
        val sortGroup = RadioGroup(this).apply { orientation = LinearLayout.VERTICAL }
        val recentRadio = RadioButton(this).apply { text = "Mais recentes primeiro"; id = View.generateViewId() }
        val alphaRadio = RadioButton(this).apply { text = "Ordem alfabética (A-Z)"; id = View.generateViewId() }
        val ratingRadio = RadioButton(this).apply {
            text = "Por nota (em breve, precisa do TMDB)"
            id = View.generateViewId()
            isEnabled = false
        }
        sortGroup.addView(recentRadio)
        sortGroup.addView(alphaRadio)
        sortGroup.addView(ratingRadio)
        when (sortMode) {
            SortMode.RECENT -> recentRadio.isChecked = true
            SortMode.ALPHABETICAL -> alphaRadio.isChecked = true
            SortMode.RATING -> recentRadio.isChecked = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 0, 28, 0)
            addView(TextView(this@MainActivity).apply { text = "Grupos ocultos, separados por vírgula:" })
            addView(groupInput)
            addView(TextView(this@MainActivity).apply { text = "Ordenar por:"; setPadding(0, 24, 0, 0) })
            addView(sortGroup)
        }
        AlertDialog.Builder(this)
            .setTitle("Categorias e ordem")
            .setView(content)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                val groups = groupInput.text.toString().split(",", "\\n").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                val newSortMode = if (alphaRadio.isChecked) SortMode.ALPHABETICAL else SortMode.RECENT
                getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).edit()
                    .putStringSet(PREF_HIDDEN_GROUPS, groups)
                    .putString(PREF_SORT_ALPHA, newSortMode.name)
                    .apply()
                sortMode = newSortMode
                renderCategories()
                renderCatalog()
                selectFirstVisible()
            }
            .show()
    }

    private fun showServerTestDialog() {
        val mac = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).getString(PREF_MAC_ADDRESS, "").orEmpty()
        val apiUrl = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).getString(PREF_TEST_API_URL, "").orEmpty()
        if (apiUrl.isBlank()) {
            Toast.makeText(this, "A API do Servidor ainda não foi configurada no painel", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, "Testando API do Servidor...", Toast.LENGTH_SHORT).show()
        appIntegration.testExternalApi(apiUrl) { result ->
            runOnUiThread {
                result.onSuccess { test ->
                    val status = if (test.ok) "online" else "offline"
                    val payload = JSONObject().apply {
                        put("mac", mac)
                        put("name", brandMark.text.toString())
                        put("status", status)
                        put("source", "maximus")
                    }
                    appIntegration.reportMaximusTestResult(payload)
                    AlertDialog.Builder(this)
                        .setTitle("Teste da API do Servidor")
                        .setMessage("Status: $status\\nHTTP: ${test.httpCode}\\n${test.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }.onFailure {
                    AlertDialog.Builder(this)
                        .setTitle("Falha no teste")
                        .setMessage(it.message ?: "Não foi possível testar a API")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun showMacDialog() {
        val input = EditText(this).apply {
            hint = "AA:BB:CC:DD:EE:FF"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
            setText(getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).getString(PREF_MAC_ADDRESS, ""))
        }
        AlertDialog.Builder(this)
            .setTitle("Dispositivo / MAC")
            .setMessage("O MAC fica salvo somente neste dispositivo e pode ser usado pelo seu servidor autorizado.")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).edit().putString(PREF_MAC_ADDRESS, input.text.toString().trim()).apply()
                Toast.makeText(this, "MAC salvo", Toast.LENGTH_SHORT).show()
                loadRemoteConfiguration()
            }
            .show()
    }

    private fun loadDerivedEpg(m3uUrl: String?) {
        val epgUrl = m3uUrl?.takeIf { it.contains("get.php", true) }?.replace("get.php", "xmltv.php", ignoreCase = true) ?: return
        loadConfiguredEpg(epgUrl)
    }

    private fun loadConfiguredEpg(epgUrl: String?) {
        if (epgUrl.isNullOrBlank()) return
        val resolved = if (epgUrl.contains("get.php", true)) epgUrl.replace("get.php", "xmltv.php", ignoreCase = true) else epgUrl
        epgRepository.load(resolved) { result ->
            result.onSuccess { map ->
                runOnUiThread {
                    epgByChannel = map
                    exactHomeProgramEntry?.let { updateExactHomeProgram(it) }
                    selectedEntry?.let { selectEntry(it, false) }
                }
            }
        }
    }

    private fun epgProgramsFor(entry: CatalogEntry): List<EpgProgram> = epgByChannel[entry.tvgId].orEmpty().sortedBy { it.start }

    private fun currentEpgProgram(programs: List<EpgProgram>): EpgProgram? {
        if (programs.isEmpty()) return null
        val now = System.currentTimeMillis()
        return programs.firstOrNull { now in it.start..it.stop } ?: programs.firstOrNull { it.start > now }
    }

    private fun renderUpcomingEpg(programs: List<EpgProgram>, current: EpgProgram?, isLive: Boolean) {
        epgUpcoming.removeAllViews()
        if (!isLive || programs.isEmpty()) {
            epgUpcoming.visibility = View.GONE
            return
        }
        val anchorIndex = current?.let { programs.indexOf(it) } ?: -1
        val upcoming = if (anchorIndex >= 0) programs.drop(anchorIndex + 1) else programs.filter { it.start > System.currentTimeMillis() }.drop(1)
        upcoming.take(5).forEachIndexed { index, program ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(7), dp(8), dp(7))
                background = rounded(if (index == 0) 0x223FE7EF else 0x14111629, 8f)
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(3), 0, 0) }
            }
            val label = TextView(this).apply {
                text = if (index == 0) "A SEGUIR" else "DEPOIS"
                setTextColor(if (index == 0) Color.rgb(43, 255, 176) else Color.rgb(170, 177, 199))
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(dp(88), -2)
            }
            val title = TextView(this).apply {
                text = "${formatTime(program.start)}  •  ${program.title}"
                setTextColor(Color.WHITE)
                textSize = 15f
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            row.addView(label)
            row.addView(title)
            epgUpcoming.addView(row)
        }
        epgUpcoming.visibility = if (epgUpcoming.childCount > 0) View.VISIBLE else View.GONE
    }

    private fun formatTime(timestamp: Long): String = if (timestamp <= 0L) "--:--" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

    private fun catalogImportStillRunning(): Boolean {
        val prefs = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE)
        return catalogImportInProgress || prefs.getBoolean(ActivationActivity.PREF_IMPORT_IN_PROGRESS, false)
    }

    private fun hiddenGroups(): Set<String> = getSharedPreferences(ActivationActivity.PREFS_NAME, MODE_PRIVATE).getStringSet(PREF_HIDDEN_GROUPS, emptySet())?.map { it.uppercase() }?.toSet() ?: emptySet()

    private fun isHidden(group: String): Boolean = group.uppercase() in hiddenGroups()

    private fun profileScopedKey(base: String): String = "${base}_profile_${activeProfileId}"

    private fun favorites(): MutableSet<String> {
        val prefs = profilePreferences()
        val scopedKey = profileScopedKey(PREF_FAVORITES)
        if (prefs.contains(scopedKey)) return prefs.getStringSet(scopedKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        // Migração transparente: dados antigos ficam apenas no perfil padrão
        // quando ele é usado pela primeira vez.
        val legacy = if (activeProfileId == "default") prefs.getStringSet(PREF_FAVORITES, emptySet()).orEmpty() else emptySet()
        if (activeProfileId == "default" && legacy.isNotEmpty()) prefs.edit().putStringSet(scopedKey, legacy).apply()
        return legacy.toMutableSet()
    }

    private fun toggleFavorite(entry: CatalogEntry) {
        val current = favorites()
        if (!current.add(entry.key)) current.remove(entry.key)
        profilePreferences().edit().putStringSet(profileScopedKey(PREF_FAVORITES), current).apply()
    }

    private fun watchedKeys(): MutableSet<String> {
        return profilePreferences().getStringSet(profileScopedKey(PREF_WATCHED_KEYS), emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun recordWatch(entry: CatalogEntry) {
        if (entry.key.isBlank()) return
        val watched = watchedKeys()
        if (watched.add(entry.key)) profilePreferences().edit().putStringSet(profileScopedKey(PREF_WATCHED_KEYS), watched).apply()
    }

    // Atalho: segurar OK em cima de um item na lista favorita/desfavorita na
    // hora, sem precisar navegar até o botão de favoritar no painel de
    // detalhes.
    private fun quickToggleFavorite(entry: CatalogEntry) {
        val wasFavorite = entry.key in favorites()
        toggleFavorite(entry)
        Toast.makeText(this, if (wasFavorite) "Removido dos favoritos" else "★ Adicionado aos favoritos", Toast.LENGTH_SHORT).show()
        if (selectedEntry?.key == entry.key) renderActions(entry)
        if (selectedCategory == FAVORITES_CATEGORY_LABEL || favoritesOnly) renderCatalog()
    }

    // Contagem de "assistido" por canal, guardada separada do catalogo (que e
    // apagado/reimportado periodicamente) para sobreviver a reimportacoes.
    // Chave estavel: CatalogEntry.key (tvg-id + streamUrl), que nao muda entre
    // reimportacoes do mesmo provedor.
    private fun watchCounts(): MutableMap<String, Int> {
        val prefs = profilePreferences()
        val scopedKey = profileScopedKey(PREF_CHANNEL_WATCH_COUNTS)
        val raw = prefs.getString(scopedKey, null) ?: if (activeProfileId == "default") prefs.getString(PREF_CHANNEL_WATCH_COUNTS, null).orEmpty() else ""
        if (raw.isBlank()) return mutableMapOf()
        return runCatching {
            val json = org.json.JSONObject(raw)
            val map = mutableMapOf<String, Int>()
            json.keys().forEach { key -> map[key] = json.optInt(key, 0) }
            map
        }.getOrDefault(mutableMapOf())
    }

    private fun recordChannelWatch(entry: CatalogEntry) {
        if (entry.kind != MediaKind.LIVE || entry.key.isBlank()) return
        val counts = watchCounts()
        counts[entry.key] = (counts[entry.key] ?: 0) + 1
        val json = org.json.JSONObject()
        counts.forEach { (key, count) -> json.put(key, count) }
        profilePreferences().edit()
            .putString(profileScopedKey(PREF_CHANNEL_WATCH_COUNTS), json.toString())
            .apply()
    }

    private fun mostWatchedChannelKey(): String? = watchCounts().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key

    private fun editorialFor(entry: CatalogEntry): ChannelEditorial {
        val known = editorials.entries.firstOrNull { entry.name.lowercase().contains(it.key) }?.value
        return known ?: ChannelEditorial(
            eyebrow = kindLabel(entry.kind),
            description = "${entry.name} está disponível na categoria ${entry.groupTitle}. Selecione assistir agora para abrir o conteúdo.",
            tags = "${entry.groupTitle}   •   ${kindLabel(entry.kind)}",
            currentProgram = if (entry.kind == MediaKind.LIVE) "Programação ao vivo" else entry.name,
            currentDescription = "Informações detalhadas do programa serão exibidas quando o provedor disponibilizar EPG para este item.",
            time = if (entry.kind == MediaKind.LIVE) "Ao vivo" else "Disponível para assistir",
            nextProgram = "A seguir  •  Consulte a programação do provedor",
        )
    }

    private fun kindLabel(kind: MediaKind): String = when (kind) {
        MediaKind.LIVE -> "AO VIVO"
        MediaKind.MOVIE -> "FILME"
        MediaKind.SERIES -> "SÉRIE"
    }

    private fun fallbackLogo(entry: CatalogEntry): Int = when {
        entry.name.contains("Animal Planet", true) -> R.drawable.animal_planet_logo
        entry.name.contains("Cartoon Network", true) -> R.drawable.cartoon_network_logo
        entry.name.contains("Discovery", true) -> R.drawable.discovery_logo
        entry.name.contains("National Geographic", true) -> R.drawable.national_geo_logo
        entry.name.contains("ESPN", true) -> R.drawable.espn_logo
        else -> R.drawable.tv_banner
    }

    private fun fallbackHero(entry: CatalogEntry): Int = when {
        entry.name.contains("Animal Planet", true) -> R.drawable.animal_planet_hero
        entry.name.contains("Cartoon Network", true) -> R.drawable.cartoon_network_hero
        entry.name.contains("Discovery", true) -> R.drawable.discovery_hero
        entry.name.contains("National Geographic", true) -> R.drawable.national_geo_hero
        entry.name.contains("ESPN", true) -> R.drawable.espn_hero
        else -> R.drawable.future_home_hero
    }

    private fun searchPlaceholder(): String = when {
        favoritesOnly -> "Buscar favoritos..."
        currentKind == MediaKind.MOVIE -> "Buscar filme..."
        currentKind == MediaKind.SERIES -> "Buscar série..."
        else -> "Buscar canal..."
    }

    private fun actionButtonBackground(primary: Boolean, focused: Boolean): GradientDrawable {
        val colors = if (primary) {
            if (focused) intArrayOf(0xFFFFD166.toInt(), 0xFFE58A17.toInt()) else intArrayOf(0xFF39D5E0.toInt(), 0xFF0A7084.toInt())
        } else {
            if (focused) intArrayOf(0xFF3FE7EF.toInt(), 0xFF1F7792.toInt()) else intArrayOf(0xFF30496E.toInt(), 0xFF131B2F.toInt())
        }
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), if (focused || primary) 0xFF7DF8FF.toInt() else 0xFF52698F.toInt())
        }
    }

    private fun ovalBackground(color: Long): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.argb((color shr 24 and 0xFF).toInt(), (color shr 16 and 0xFF).toInt(), (color shr 8 and 0xFF).toInt(), (color and 0xFF).toInt()))
        setStroke(dp(1), Color.argb(170, 120, 205, 255))
    }

    private fun rounded(color: Long, radius: Float): GradientDrawable = GradientDrawable().apply {
        setColor(Color.argb((color shr 24 and 0xFF).toInt(), (color shr 16 and 0xFF).toInt(), (color shr 8 and 0xFF).toInt(), (color and 0xFF).toInt()))
        cornerRadius = radius
    }

    private fun ovalDrawable(color: Long): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.argb((color shr 24 and 0xFF).toInt(), (color shr 16 and 0xFF).toInt(), (color shr 8 and 0xFF).toInt(), (color and 0xFF).toInt()))
    }

    private fun orbitalBubbleDrawable(color: Long): GradientDrawable {
        val source = color.toInt()
        val red = Color.red(source)
        val green = Color.green(source)
        val blue = Color.blue(source)
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.argb(142, red, green, blue),
                Color.argb(70, red / 2, green / 2, blue / 2),
            ),
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(dp(1), 0xB8FFFFFF.toInt())
        }
    }

    private fun orbitalSatelliteDrawable(color: Long): GradientDrawable {
        val source = color.toInt()
        val red = Color.red(source)
        val green = Color.green(source)
        val blue = Color.blue(source)
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.argb(100, red, green, blue),
                Color.argb(34, red, green, blue),
            ),
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(dp(1), 0x72EAF2FF.toInt())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_VOICE) return
        voiceMode = false
        renderNavigation()
        if (resultCode != RESULT_OK) return
        val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (spoken.isNotBlank()) handleVoiceCommand(spoken)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_VOICE_PERMISSION && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startVoiceCommand()
        else if (requestCode == REQUEST_VOICE_PERMISSION) {
            voiceMode = false
            renderNavigation()
            Toast.makeText(this, "Permissão de microfone recusada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        val hadParentalAccess = parentalUnlocked
        parentalUnlocked = false
        constellationSound?.let { sound -> if (sound.isPlaying) sound.pause(); sound.seekTo(0) }
        if (hadParentalAccess && ::catalogAdapter.isInitialized && !homeMode) {
            selectedCategory = "Todos"
            selectedEntry = null
            categoryCache.clear()
            clearPreviewForSection(currentKind)
            renderCategories()
            renderCatalog()
        }
        super.onStop()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        constellationSound?.release()
        constellationSound = null
        stopMiniPlayer()
        repository.shutdown()
        imageLoader.shutdown()
        epgRepository.shutdown()
        appIntegration.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val PREF_FAVORITES = "favorite_catalog_keys"
        private const val PREF_PROFILES = "future_profiles"
        private const val PREF_ACTIVE_PROFILE_ID = "future_active_profile_id"
        private const val PREF_WATCHED_KEYS = "watched_catalog_keys"
        private const val PREF_HIDDEN_GROUPS = "hidden_catalog_groups"
        private const val PREF_CHANNEL_WATCH_COUNTS = "channel_watch_counts"
        private const val FAVORITES_CATEGORY_LABEL = "★ Favoritos"
        private const val PREF_SORT_ALPHA = "catalog_sort_alpha"
        private const val PREF_MAC_ADDRESS = "mac_address"
        private const val PREF_SERVER_API_URL = "server_api_url"
        private const val PREF_TEST_API_URL = "test_api_url"
        private const val PREF_LAST_MESSAGE_KEY = "last_remote_message_key"
        private const val PREF_TRAILER_AUDIO = "trailer_audio_enabled"
        private const val PREF_TRANSITION_STARS = "future_transition_stars"
        private const val PREF_TRANSITION_SOUND = "future_transition_sound"
        private const val TRAILER_FOCUS_DELAY_MS = 5_000L
        private const val PREF_AUTOPLAY = "autoplay_enabled"
        const val EXTRA_CATALOG_IMPORT_IN_PROGRESS = "catalog_import_in_progress"
        private const val PREF_KEEP_SCREEN = "keep_screen_on"
        private const val PREF_SHOW_EPG = "show_epg"
        private const val PREF_PARENTAL_PIN_HASH = "parental_pin_hash"
        private const val PREF_SUBTITLE_ENABLE = "subtitle_enable"
        private const val PREF_LANGUAGE_CODE = "language_code"
        private const val PREF_REMOTE_SYNC = "remote_sync_enabled"
        private const val PREF_EXTERNAL_PLAYER = "external_player"
        private const val PREF_SELECTED_DNS = "selected_dns"
        private const val REQUEST_VOICE = 7101
        private const val REQUEST_VOICE_PERMISSION = 7102
    }
}
