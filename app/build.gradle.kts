import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// Load keystore.properties for signing config (never commit this file)
val keystoreProperties = Properties().apply {
    val keystoreFile = rootProject.file("keystore.properties")
    if (keystoreFile.exists()) {
        load(keystoreFile.inputStream())
    }
}

fun signingProp(name: String): String? {
    val fromFile = keystoreProperties.getProperty(name)?.trim().orEmpty()
    if (fromFile.isNotEmpty()) return fromFile
    val fromEnv = System.getenv(name)?.trim().orEmpty()
    return fromEnv.ifEmpty { null }
}

val requiredReleaseSigningProps = listOf(
    "RELEASE_STORE_FILE",
    "RELEASE_STORE_PASSWORD",
    "RELEASE_KEY_ALIAS",
    "RELEASE_KEY_PASSWORD"
)

fun quotedBuildConfigValue(value: String): String {
    return "\"" + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"") + "\""
}

val googleWebClientId = providers.gradleProperty("TERMEX_GOOGLE_WEB_CLIENT_ID").orNull
    ?: System.getenv("TERMEX_GOOGLE_WEB_CLIENT_ID")
    ?: ""
val enableGoogleSync = (providers.gradleProperty("TERMEX_ENABLE_GOOGLE_SYNC").orNull
    ?: System.getenv("TERMEX_ENABLE_GOOGLE_SYNC")
    ?: "false").equals("true", ignoreCase = true)

android {
    namespace = "com.termex.app"
    compileSdk = 35
    testBuildType = "releaseProof"

    defaultConfig {
        applicationId = "com.termex.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", quotedBuildConfigValue(googleWebClientId))
        buildConfigField("boolean", "ENABLE_GOOGLE_SYNC", enableGoogleSync.toString())

        testInstrumentationRunner = "com.termex.app.testing.TermexHiltTestRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            signingProp("RELEASE_STORE_FILE")?.let { storeFile = file(it) }
            signingProp("RELEASE_STORE_PASSWORD")?.let { storePassword = it }
            signingProp("RELEASE_KEY_ALIAS")?.let { keyAlias = it }
            signingProp("RELEASE_KEY_PASSWORD")?.let { keyPassword = it }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "BYPASS_PAYWALL", "false")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "BYPASS_PAYWALL", "false")
        }

        create("dev") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("boolean", "BYPASS_PAYWALL", "true")
            matchingFallbacks += listOf("debug")
        }

        create("releaseProof") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".proof"
            versionNameSuffix = "-proof"
            buildConfigField("boolean", "BYPASS_PAYWALL", "false")
            matchingFallbacks += listOf("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        animationsDisabled = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        managedDevices {
            localDevices {
                create("pixel2api29") {
                    device = "Pixel 2"
                    apiLevel = 29
                    systemImageSource = "aosp"
                }
                create("pixel8api35") {
                    device = "Pixel 8"
                    apiLevel = 35
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,DEPENDENCIES,LICENSE.md,LICENSE-notice.md}"
        }
    }
}

val validateReleaseSigning by tasks.registering {
    group = "verification"
    description = "Fails early when release signing inputs are missing or invalid."

    doLast {
        val missing = mutableListOf<String>()
        val storeFilePath = signingProp("RELEASE_STORE_FILE")

        requiredReleaseSigningProps.forEach { name ->
            if (signingProp(name).isNullOrBlank()) {
                missing += name
            }
        }

        if (!storeFilePath.isNullOrBlank() && !file(storeFilePath).isFile) {
            missing += "RELEASE_STORE_FILE (file not found: $storeFilePath)"
        }

        if (missing.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Release signing is not configured.")
                    appendLine("Provide keystore.properties or these environment variables:")
                    requiredReleaseSigningProps.forEach { appendLine("- $it") }
                    appendLine("Missing or invalid:")
                    missing.forEach { appendLine("- $it") }
                }
            )
        }
    }
}

listOf(
    "assembleRelease",
    "bundleRelease",
    "packageReleaseBundle",
    "signReleaseBundle",
    "validateSigningRelease"
).forEach { taskName ->
    tasks.matching { it.name == taskName }.configureEach {
        dependsOn(validateReleaseSigning)
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Room with KSP
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Security & Biometrics
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.1.0")

    // SSH - Apache MINA SSHD
    implementation("org.apache.sshd:sshd-core:2.16.0")
    implementation("net.i2p.crypto:eddsa:0.3.0") // ed25519 support
    implementation("org.slf4j:slf4j-simple:2.0.13")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

    // Billing
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("com.google.android.gms:play-services-auth:21.5.1")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Leak detection (debug only)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("app.cash.turbine:turbine:1.2.0")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    androidTestImplementation(files("$buildDir/intermediates/hilt/component_classes/debug"))
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("io.mockk:mockk-android:1.13.13")
    androidTestUtil("androidx.test:orchestrator:1.5.1")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
