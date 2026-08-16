pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "cutcalculator"

include(":core")
include(":desktop")

// Il modulo Android entra nella build solo quando l'SDK c'e' davvero: senza, il plugin Android
// fallirebbe la configurazione e nemmeno "./gradlew :core:test" partirebbe piu'.
val sdkAndroid = System.getenv("ANDROID_HOME")
    ?: System.getenv("ANDROID_SDK_ROOT")
    ?: file("local.properties").takeIf { it.exists() }
        ?.readLines()
        ?.firstOrNull { it.startsWith("sdk.dir=") }
        ?.substringAfter("=")
        // In un .properties il backslash e' un escape: su Windows il percorso arriva come
        // "C\:\\Users\\..." e va ripulito, altrimenti non corrisponde a nessuna cartella.
        ?.replace("\\\\", "\\")
        ?.replace("\\:", ":")
if (sdkAndroid != null && file(sdkAndroid).exists()) {
    include(":android")
} else {
    logger.lifecycle(
        "Android SDK non trovato: il modulo :android resta fuori dalla build. " +
                "Apri il progetto in Android Studio (crea local.properties) oppure imposta ANDROID_HOME."
    )
}
