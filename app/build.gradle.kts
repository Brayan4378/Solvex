import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val solvexLocalProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val solvexDebugApiBaseUrl = providers.gradleProperty("SOLVEX_API_BASE_URL")
    .orElse(providers.environmentVariable("SOLVEX_API_BASE_URL"))
    .orElse(solvexLocalProperties.getProperty("SOLVEX_API_BASE_URL") ?: "http://10.0.2.2:8080/")
val solvexReleaseApiBaseUrl = providers.gradleProperty("SOLVEX_API_BASE_URL")
    .orElse(providers.environmentVariable("SOLVEX_API_BASE_URL"))
    .orElse("https://backend.example.com/")

android {
    namespace = "com.solvex.solvex"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.solvex.solvex"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"${solvexDebugApiBaseUrl.get()}\"")
            buildConfigField("boolean", "DEMO_MODE", "true")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"${solvexReleaseApiBaseUrl.get()}\"")
            buildConfigField("boolean", "DEMO_MODE", "false")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // PDFBox para extracción de texto de PDFs (extractos bancarios)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
