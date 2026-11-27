import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Proxy URL + app token live in android/local.properties (git-ignored), never in source.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun localProp(key: String) = (localProps.getProperty(key) ?: "").replace("\"", "")

android {
    namespace = "com.movieradar.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.movieradar.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 16
        versionName = "0.9.1"
        buildConfigField("String", "PROXY_BASE_URL", "\"${localProp("movieradar.proxyUrl")}\"")
        buildConfigField("String", "APP_TOKEN", "\"${localProp("movieradar.appToken")}\"")
    }

    // Two apps from one codebase, installable side by side.
    flavorDimensions += "edition"
    productFlavors {
        create("standard") {
            dimension = "edition"
            applicationId = "com.movieradar.app"
            resValue("string", "app_name", "MoviesRadar")
            buildConfigField("boolean", "ADULT_ENABLED", "false")
            buildConfigField("String", "LOGO_TEXT", "\"Movies\"")
            buildConfigField("long", "LOGO_ACCENT", "0xFFE50914L")
        }
        create("plus") {
            dimension = "edition"
            applicationId = "com.movieradar.plus"
            resValue("string", "app_name", "MovieRadar")
            buildConfigField("boolean", "ADULT_ENABLED", "true")
            buildConfigField("String", "LOGO_TEXT", "\"Movie\"")
            buildConfigField("long", "LOGO_ACCENT", "0xFFFF7A00L")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
