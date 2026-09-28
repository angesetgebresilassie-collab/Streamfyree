plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.streamfyree.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.streamfyree.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0"

        // No longer tied to Chaquopy's supported ABI set now that the
        // Python/yt-dlp runtime is gone, so widen device coverage.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        }

        // Spotify Web API (Client Credentials flow — app-only, no user login)
        // used for playlist search/browsing. Get a free Client ID/Secret at
        // https://developer.spotify.com/dashboard and set them below via
        // gradle.properties (SPOTIFY_CLIENT_ID / SPOTIFY_CLIENT_SECRET) or as
        // environment variables of the same name. Left blank, the Spotify
        // feed simply stays empty — playback of everything else is unaffected.
        val spotifyClientId = (project.findProperty("SPOTIFY_CLIENT_ID") as? String)
            ?: System.getenv("SPOTIFY_CLIENT_ID") ?: ""
        val spotifyClientSecret = (project.findProperty("SPOTIFY_CLIENT_SECRET") as? String)
            ?: System.getenv("SPOTIFY_CLIENT_SECRET") ?: ""
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$spotifyClientId\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"$spotifyClientSecret\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.03.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-session:1.6.1")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("com.github.teamnewpipe:newpipeextractor:0.26.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
