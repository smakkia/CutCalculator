// L'app per il telefono. Non contiene logica: tutto il calcolo sta in :core, esattamente lo stesso
// modulo che usa il client desktop. Qui dentro ci sono solo le schermate Compose e il ponte verso
// il Controller.
// ⚠️ Niente `kotlin("android")`: da AGP 9.0 il supporto Kotlin e' **dentro** il plugin Android, e
// applicarlo di nuovo fa fallire la configurazione (vedi kotl.in/gradle/agp-built-in-kotlin).

import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.cutcalculator.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cutcalculator.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(
                keystoreProperties["storeFile"] as String
            )
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation(project(":core"))

    // Anche qui: le versioni piu' recenti pretendono compileSdk 37 (preview). Queste stanno su 36.
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    // Il BOM tiene allineate fra loro tutte le librerie Compose: le singole non portano versione.
    // ⚠️ Non l'ultimo (2026.08.00): pretende `compileSdk 37`, che e' ancora una preview e non si
    // scarica dal canale stabile. Questo e' l'ultimo che sta su API 36.
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
