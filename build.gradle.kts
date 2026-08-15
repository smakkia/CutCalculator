// Build multi-modulo:
//   :core    — il motore, Kotlin puro, nessuna UI. Lo usano sia il desktop sia Android.
//   :desktop — i due front-end di oggi (CLI + JavaFX), che girano sul PC.
//   :android — l'app per il telefono (entra nella build solo se l'SDK c'e', vedi settings).
// I plugin si dichiarano qui una volta sola, con "apply false": i moduli li applicano senza
// ripeterne la versione.
plugins {
    kotlin("jvm") version "2.4.10" apply false
    kotlin("plugin.compose") version "2.4.10" apply false
    id("com.android.application") version "9.3.1" apply false
    id("org.openjfx.javafxplugin") version "0.1.0" apply false
}

group = "com.cutcalculator"
version = "0.1.0-SNAPSHOT"

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
