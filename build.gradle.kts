// Build multi-modulo:
//   :core    — il motore, Kotlin puro, nessuna UI. Lo usano sia il desktop sia Android.
//   :desktop — i due front-end di oggi (CLI + JavaFX), che girano sul PC.
//   :android — l'app per il telefono (entra nella build solo se l'SDK c'e', vedi settings).
// I plugin si dichiarano qui una volta sola, con "apply false": i moduli li applicano senza
// ripeterne la versione.
// ⚠️ AGP resta sulla serie **8.x** e non passa alla 9: il plugin Android incluso in IntelliJ IDEA
// 2025.3 e' di febbraio 2026, precedente ad AGP 9, e sincronizzando un progetto 9.x muore con
// "Unsupported method: AndroidArtifact.getPrivacySandboxSdkInfo()" — un metodo che l'IDE chiede al
// modello e che AGP 9 ha tolto. A cadere non e' solo il modulo Android: il sync fallisce **tutto**,
// quindi dall'IDE non parte piu' nemmeno la GUI desktop. Alzare la versione solo dopo l'IDE.
// Conseguenza: serve di nuovo `kotlin("android")`, perche' il Kotlin integrato e' una novita' di AGP 9.
// ⚠️ Nemmeno tutta la serie 8 va bene: **8.12.0 e' il massimo che quell'IDE accetta** ("The project is
// using an incompatible version (AGP 8.13.2)... Latest supported version is AGP 8.12.0"). Il tetto lo
// alza l'IDE, non noi: si sale solo dopo averlo aggiornato.
plugins {
    kotlin("jvm") version "2.4.10" apply false
    kotlin("android") version "2.4.10" apply false
    kotlin("plugin.compose") version "2.4.10" apply false
    id("com.android.application") version "8.12.0" apply false
    id("org.openjfx.javafxplugin") version "0.1.0" apply false
}

group = "com.cutcalculator"
version = "0.4.0"

// Scorciatoie per non dover ricordare il modulo: `./gradlew run` continua a fare quel che faceva.
tasks.register("run") {
    group = "application"
    description = "Avvia il client testuale (alias di :desktop:run)"
    dependsOn(":desktop:run")
}

tasks.register("runGui") {
    group = "application"
    description = "Avvia il client grafico JavaFX (alias di :desktop:runGui)"
    dependsOn(":desktop:runGui")
}
