import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "dev.mrbean.laundryledger"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.mrbean.laundryledger"
        minSdk = 26
        targetSdk = 35
        // CI passes the git tag (v1.2.3 -> 1.2.3). versionCode = major*10000 + minor*100 + patch.
        val vName = (System.getenv("VERSION_NAME") ?: "1.1.1").removePrefix("v")
        val (maj, min, pat) = (vName.split(".").map { it.toIntOrNull() ?: 0 } + listOf(0, 0, 0)).take(3)
        versionCode = maj * 10000 + min * 100 + pat
        versionName = vName
    }
    signingConfigs {
        val localDebug = rootProject.file("debug.keystore")
        if (localDebug.exists()) {
            getByName("debug") {
                storeFile = localDebug
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        // Release key: from keystore/keystore.properties locally, or env vars in CI.
        val props = Properties().apply {
            rootProject.file("keystore/keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        val storePath = System.getenv("SIGNING_STORE_FILE") ?: props.getProperty("storeFile")
        if (storePath != null) {
            create("release") {
                storeFile = file(storePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: props.getProperty("storePassword")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: props.getProperty("keyAlias")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: props.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp { arg("room.generateKotlin", "true") }

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    testImplementation("junit:junit:4.13.2")
}
