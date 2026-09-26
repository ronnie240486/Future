plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}


android {
    namespace = "com.futuretv.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.futuretv.player"
        minSdk = 23
        targetSdk = 35
        versionCode = 16
        versionName = "1.0.15"
    }

    // BUG CRÍTICO corrigido: sem esse bloco, o build debug usava a chave de
    // assinatura PADRÃO do Gradle (~/.android/debug.keystore) -- que num
    // runner do GitHub Actions não existe de fábrica e é GERADA DO ZERO, com
    // uma chave nova e aleatória, a CADA execução do workflow. Resultado:
    // toda vez que o CI compilava um novo APK, ele saía assinado com uma
    // chave diferente da versão anterior -- e o Android RECUSA instalar
    // "por cima" (update install) quando a assinatura muda (erro
    // INSTALL_FAILED_UPDATE_INCOMPATIBLE), obrigando a desinstalar antes.
    // Isso não só derrubava o SharedPreferences (apagando o MAC salvo), como
    // também trocava o ANDROID_ID em si (documentado pelo Android: o
    // ANDROID_ID muda quando a chave de assinatura do app muda) -- ou seja,
    // o "MAC muda toda hora" acontecia mesmo com a correção de persistência
    // em ActivationActivity.onCreate(), porque nunca dava pra fazer o
    // "instalar por cima" de verdade entre builds diferentes do CI.
    // A correção: gerar uma keystore de debug FIXA uma única vez (commitada
    // no repo -- prática recomendada pelo próprio Google pra times/CI, já
    // que é só uma chave de debug, sem valor de segurança real) e usar ela
    // em todo build debug, pra sempre sair com a MESMA assinatura -- assim
    // "instalar por cima" funciona entre qualquer build novo saído do CI, o
    // ANDROID_ID para de mudar, e o MAC persistido continua sendo o mesmo.
    signingConfigs {
        getByName("debug") {
            storeFile = file("future-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

