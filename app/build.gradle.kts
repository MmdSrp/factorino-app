plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "ir.shaerpour.factorino"
    compileSdk = 35
    defaultConfig {
        applicationId = "ir.shaerpour.factorino"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.0." + (System.getenv("VERSION_CODE") ?: "1")
    }
    // امضای نسخه‌ی نهایی: فایل keystore.properties را بسازید (راهنما در README)
    val ksFile = rootProject.file("keystore.properties")
    if (ksFile.exists()) {
        val p = java.util.Properties().apply { load(ksFile.inputStream()) }
        signingConfigs.create("release") {
            storeFile = file(p["storeFile"] as String)
            storePassword = p["storePassword"] as String
            keyAlias = p["keyAlias"] as String
            keyPassword = p["keyPassword"] as String
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
}
