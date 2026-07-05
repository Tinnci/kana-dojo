plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.tinnci.kanadojo"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    compileSdkMinor = libs.versions.androidCompileSdkMinor.get().toInt()
    buildToolsVersion = libs.versions.androidBuildTools.get()

    defaultConfig {
        applicationId = "dev.tinnci.kanadojo"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
