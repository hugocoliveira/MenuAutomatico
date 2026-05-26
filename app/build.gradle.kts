import java.util.Properties // lê o local.properties para tokens e configurações de keystore

// ─────────────────────────────────────────────────────────────────────────────
// PLUGINS
// Cada alias é resolvido no gradle/libs.versions.toml — versões centralizadas lá.
// ─────────────────────────────────────────────────────────────────────────────
plugins {
    alias(libs.plugins.android.application) // plugin principal do Android — gera APK/AAB
    alias(libs.plugins.kotlin.compose)      // habilita o compilador do Jetpack Compose para Kotlin
    alias(libs.plugins.hilt.android)        // plugin Hilt — gera código de injeção de dependência
    alias(libs.plugins.ksp)                 // KSP (Kotlin Symbol Processing) — processa anotações Room e Hilt
}

// ─────────────────────────────────────────────────────────────────────────────
// LEITURA DO local.properties
// Arquivo não commitado no git — contém segredos do ambiente local:
//  - github.token   → token GitHub para OTA (evita rate limit anônimo)
//  - keystore.path  → caminho absoluto do arquivo .jks de assinatura
//  - keystore.password / key.alias / key.password → credenciais do keystore
// ─────────────────────────────────────────────────────────────────────────────
val localProps = Properties().apply {
    val f = rootProject.file("local.properties") // arquivo na raiz do projeto
    if (f.exists()) load(f.inputStream())         // carrega apenas se existir — não falha em CI sem ele
}

// ═════════════════════════════════════════════════════════════════════════════
// CONFIGURAÇÃO ANDROID
// ═════════════════════════════════════════════════════════════════════════════
android {
    namespace  = "com.lit.aplicacaomenuautomatico" // package base do app — usado no R e no BuildConfig
    compileSdk {
        // compileSdk 36.1 — API mais recente disponível; permite usar APIs novas sem quebrar minSdk
        version = release(36) {
            minorApiLevel = 1 // sub-versão da API 36 (36.1)
        }
    }

    // ─── CONFIGURAÇÃO PADRÃO ──────────────────────────────────────────────────
    defaultConfig {
        applicationId = "com.lit.aplicacaomenuautomatico" // identificador único do app na Play Store e no dispositivo
        minSdk        = 24   // Android 7.0 (Nougat) — versão mínima dos coletores Zebra MC3300
        targetSdk     = 36   // API alvo — define o comportamento em dispositivos com Android 36
        versionCode   = 23   // número inteiro incremental — usado pelo sistema para detectar atualização
        versionName   = "1.23" // versão legível exibida na tela de login e nas notificações OTA

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" // runner de testes instrumentados

        // Injeta o token do GitHub no BuildConfig em tempo de compilação.
        // Usado pelo AppUpdateChecker para requests autenticados (rate limit maior).
        // Se o token não estiver no local.properties, injeta string vazia (request anônimo).
        buildConfigField("String", "GITHUB_TOKEN", "\"${localProps.getProperty("github.token", "")}\"")
    }

    // ─── CONFIGURAÇÃO DE ASSINATURA ───────────────────────────────────────────
    // Todos os valores vêm do local.properties — nunca commitados no git.
    // Sem os valores corretos, o build release falha na etapa de assinatura.
    signingConfigs {
        create("release") {
            storeFile     = file(localProps.getProperty("keystore.path", ""))     // caminho do arquivo .jks
            storePassword = localProps.getProperty("keystore.password", "")        // senha do keystore
            keyAlias      = localProps.getProperty("key.alias", "")               // alias da chave dentro do keystore
            keyPassword   = localProps.getProperty("key.password", "")            // senha da chave privada
        }
    }

    // ─── TIPOS DE BUILD ───────────────────────────────────────────────────────
    buildTypes {
        release {
            // isMinifyEnabled = false: ProGuard/R8 desabilitado.
            // Habilitado no futuro reduzirá o APK, mas exige mapeamento de classes
            // para que o Hilt e o Room funcionem corretamente (regras em proguard-rules.pro).
            isMinifyEnabled = false

            signingConfig = signingConfigs.getByName("release") // assina com as credenciais do local.properties

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), // regras base do Android
                "proguard-rules.pro"                                      // regras customizadas do projeto
            )
        }
        // O buildType "debug" usa assinatura de debug automática — sem configuração necessária
    }

    // ─── COMPATIBILIDADE JAVA ─────────────────────────────────────────────────
    // Java 17 é o mínimo exigido pelo AGP 9+ e pelo Kotlin 2.x.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 // versão do código-fonte Java compilado
        targetCompatibility = JavaVersion.VERSION_17 // versão do bytecode gerado (.class files)
    }

    // ─── FEATURES ─────────────────────────────────────────────────────────────
    buildFeatures {
        compose     = true // habilita o compilador do Jetpack Compose — obrigatório para UI declarativa
        buildConfig = true // gera a classe BuildConfig com campos customizados (GITHUB_TOKEN, VERSION_NAME, etc.)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONFIGURAÇÃO KOTLIN
// ─────────────────────────────────────────────────────────────────────────────
kotlin {
    compilerOptions {
        // JVM 17 alinhado com compileOptions acima — garante consistência entre Java e Kotlin
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONFIGURAÇÃO KSP (Kotlin Symbol Processing)
// room.schemaLocation: diretório onde o Room gera os arquivos JSON de schema
// (ex.: app/schemas/com.lit.../AppDatabase/2.json).
// Esses arquivos devem ser commitados no git para rastrear mudanças de schema
// e auxiliar na criação de migrations futuras.
// Nível raiz (não dentro de android{}) — compatível com KSP 2.x + AGP 9.
// ─────────────────────────────────────────────────────────────────────────────
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// ═════════════════════════════════════════════════════════════════════════════
// DEPENDÊNCIAS
// Versões gerenciadas centralmente em gradle/libs.versions.toml.
// Nunca adicionar versões hardcoded aqui — usar sempre o alias do TOML.
// ═════════════════════════════════════════════════════════════════════════════
dependencies {

    // ─── AndroidX Core ────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)             // extensões Kotlin para APIs Android (Context, View, etc.)
    implementation(libs.androidx.lifecycle.runtime.ktx) // lifecycle-aware corrotinas e extensões
    implementation(libs.androidx.activity.compose)     // integração Activity com Jetpack Compose (setContent, etc.)

    // ─── Jetpack Compose ──────────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom)) // BOM: alinha versões de todos os módulos Compose
    implementation(libs.androidx.compose.ui)            // runtime e tooling básico do Compose
    implementation(libs.androidx.compose.ui.graphics)   // primitivas gráficas (Color, Path, Canvas)
    implementation(libs.androidx.compose.ui.tooling.preview) // suporte ao @Preview no Android Studio
    implementation(libs.androidx.compose.material3)     // componentes Material3 (Button, Card, TextField, etc.)
    implementation(libs.androidx.compose.material.icons.extended) // ícones extras (Person, Lock, Download, etc.)

    // ─── Lifecycle + ViewModel ────────────────────────────────────────────────
    implementation(libs.lifecycle.viewmodel.compose)  // hiltViewModel() e viewModel() em Composables
    implementation(libs.lifecycle.runtime.compose)    // collectAsStateWithLifecycle() — coleta Flow respeitando lifecycle

    // ─── Hilt — injeção de dependência ───────────────────────────────────────
    implementation(libs.hilt.android)          // runtime do Hilt — contêiner de DI
    ksp(libs.hilt.compiler)                    // gerador de código Hilt em tempo de compilação (via KSP)
    implementation(libs.hilt.navigation.compose) // hiltViewModel() integrado ao Navigation Compose
    implementation(libs.hilt.work)             // integração Hilt + WorkManager (@HiltWorker)
    ksp(libs.hilt.work.compiler)               // gerador de código Hilt para Workers

    // ─── Room — banco de dados SQLite local ──────────────────────────────────
    implementation(libs.room.runtime) // runtime do Room (Database, Entity, DAO)
    implementation(libs.room.ktx)     // extensões Kotlin para Room (suspend queries, Flow)
    ksp(libs.room.compiler)           // gerador de código Room em tempo de compilação (via KSP)

    // ─── Retrofit + OkHttp — chamadas HTTP ao SAP OData ─────────────────────
    implementation(libs.retrofit.core)    // cliente HTTP de alto nível — gera implementação da interface ODataService
    implementation(libs.retrofit.gson)    // conversor JSON: Gson desserializa ODataEnvelope e MenuAppDto
    implementation(libs.okhttp.core)      // cliente HTTP de baixo nível (pooling de conexões, interceptors)
    implementation(libs.okhttp.logging)   // HttpLoggingInterceptor — loga chamadas HTTP no Logcat

    // ─── Navigation Compose ───────────────────────────────────────────────────
    implementation(libs.navigation.compose) // NavHost, NavController, composable() — roteamento entre telas

    // ─── WorkManager — sincronização em background ────────────────────────────
    implementation(libs.workmanager.ktx) // WorkManager com suporte a corrotinas (CoroutineWorker)

    // ─── Security Crypto — armazenamento seguro de credenciais ───────────────
    implementation(libs.security.crypto) // EncryptedSharedPreferences + MasterKey (AES256-GCM)

    // ─── Coroutines ───────────────────────────────────────────────────────────
    implementation(libs.coroutines.android) // Dispatchers.Main, Dispatchers.IO, Flow, StateFlow

    // ─── Testes ───────────────────────────────────────────────────────────────
    testImplementation(libs.junit)                              // JUnit 4 — testes unitários locais (JVM)
    androidTestImplementation(libs.androidx.junit)              // JUnit 4 para testes instrumentados (dispositivo)
    androidTestImplementation(libs.androidx.espresso.core)      // Espresso — testes de UI instrumentados
    androidTestImplementation(platform(libs.androidx.compose.bom)) // BOM Compose para testes instrumentados
    androidTestImplementation(libs.androidx.compose.ui.test.junit4) // ComposeTestRule para testes de Composables
    debugImplementation(libs.androidx.compose.ui.tooling)       // suporte ao @Preview em builds debug
    debugImplementation(libs.androidx.compose.ui.test.manifest) // manifesto de teste Compose (apenas debug)

    // ─── Módulo interno de atualização OTA ───────────────────────────────────
    // updater-lib: verifica versões no GitHub e faz download de APKs atualizados.
    // Módulo local — código em updater-lib/ na raiz do projeto.
    implementation(project(":updater-lib"))
}
