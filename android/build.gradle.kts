// L'app per il telefono. Non contiene logica: tutto il calcolo sta in :core, esattamente lo stesso
// modulo che usa il client desktop. Qui dentro ci sono solo le schermate Compose e il ponte verso
// il Controller.
// ⚠️ `kotlin("android")` serve perche' l'AGP e' fermo alla serie 8 (il perche' e' nel build.gradle.kts
// di radice): il supporto Kotlin integrato, che renderebbe superfluo questo plugin, arriva con AGP 9.
// Se un domani si passera' alla 9, questa riga va **tolta**, altrimenti la configurazione fallisce con
// un errore illeggibile ("java.lang.Throwable (no error message)").

import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
}

// L'etichetta della versione, scritta una volta sola: la usano il `versionName` che si legge nelle
// impostazioni del telefono e il nome del file APK.
val versioneApp = "0.6.0"

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
        // versionCode e' il numero che Android confronta per decidere se un APK e' un aggiornamento:
        // deve **crescere** a ogni rilascio, altrimenti l'installazione viene rifiutata come downgrade.
        // versionName e' solo l'etichetta che si legge nelle impostazioni del telefono.
        versionCode = 6
        versionName = versioneApp
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // La firma di release sta in keystore.properties, che e' fuori dal versionamento. Senza quel file
    // il blocco si salta: altrimenti la build **debug** morirebbe in configurazione su una macchina
    // che il keystore non ce l'ha (i cast a String su valori assenti lanciano subito).
    if (keystorePropertiesFile.exists()) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

// Il nome dell'APK segue lo stesso schema del pacchetto desktop
// (`CutCalculator-0.6.0-windows-x64.zip`): `CutCalculator-<versione>-android.apk`. Il predefinito
// era `android-release.apk`, che fuori dalla cartella di build non dice ne' che app sia ne' quale
// versione. La **debug** tiene il suo suffisso: ha una firma diversa e installarla sopra la release
// fallisce, quindi le due non devono somigliarsi al punto da confonderle.
// ⚠️ Passa dalla vecchia `applicationVariants` perche' la Variant API nuova (`androidComponents`) in
// AGP 8 **non espone** il nome del file: `VariantOutput.outputFileName` non esiste, ce l'ha solo
// l'implementazione interna. Con AGP 9 `applicationVariants` sparisce e questo blocco va rifatto.
android.applicationVariants.all {
    val suffisso = if (buildType.name == "release") "" else "-${buildType.name}"
    outputs.all {
        (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
            "CutCalculator-$versioneApp-android$suffisso.apk"
    }
}

// Con AGP 8 il plugin Kotlin e' esterno, quindi la sua estensione sta **fuori** dal blocco android.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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
