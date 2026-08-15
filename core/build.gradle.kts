import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Il motore: dominio, formule, ottimizzatore, preventivo, pianificazione, persistenza, catalogo e
// Controller. Kotlin puro, nessuna dipendenza da JavaFX o da Android: lo usano tanto il client
// desktop quanto l'app per il telefono.
plugins {
    kotlin("jvm")
}

// ⚠️ jvmTarget 17 e non 25: Android non digerisce bytecode piu' recente di Java 17 (D8 lo
// rifiuterebbe). Il desktop puo' restare su 25 perche' un modulo puo' sempre usare classi compilate
// per una versione precedente, mai il contrario.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.test {
    useJUnitPlatform()
}
