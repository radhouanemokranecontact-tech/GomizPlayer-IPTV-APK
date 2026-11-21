plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.gomizplayer.iptv"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    defaultConfig {
        applicationId = "com.gomizplayer.iptv"
        minSdk = 24
        targetSdk = 37
        versionCode = 104
        versionName = "1.0.4"
    }

    signingConfigs {
        create("release") {
            val ksPath = (System.getenv("GOMIZ_KEYSTORE") ?: (project.findProperty("gomiz.keystore") as String?))?.trim()
            if (!ksPath.isNullOrEmpty() && file(ksPath).isFile) {
                storeFile = file(ksPath)
                storePassword = System.getenv("GOMIZ_KEYSTORE_PASSWORD") ?: project.findProperty("gomiz.keystore.password") as String?
                keyAlias = System.getenv("GOMIZ_KEY_ALIAS") ?: project.findProperty("gomiz.key.alias") as String? ?: "gomiz"
                keyPassword = System.getenv("GOMIZ_KEY_PASSWORD") ?: project.findProperty("gomiz.key.password") as String?
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")?.takeIf { it.storeFile != null } ?: signingConfigs.getByName("debug")
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(bom)

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    // Android TV (Leanback) Compose
    implementation("androidx.tv:tv-foundation:1.0.0")
    implementation("androidx.tv:tv-material:1.1.0")

    // Media3 ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.11.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.11.1")

    // Network + JSON
    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0")

    // Images
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")
    implementation("io.coil-kt.coil3:coil-svg:3.6.3")

    // ffmpeg (maintained fork, free tier) — used ONLY for local MP4 remux (network goes through OkHttp/Media3 which have TLS)
    implementation("dev.ffmpegkit-maintained:ffmpeg:8.1.9")

    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
