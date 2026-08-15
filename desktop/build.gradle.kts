// I due front-end che girano sul PC: il client testuale (CLI) e quello grafico (JavaFX).
// Tutta la logica sta in :core — qui dentro ci sono solo menu, finestre e formattazione.
plugins {
    kotlin("jvm")
    application
    id("org.openjfx.javafxplugin")
}

dependencies {
    implementation(project(":core"))
    testImplementation(project(":core"))
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

javafx {
    version = "25.0.1"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass = "com.cutcalculator.cli.CliApp"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// ⚠️ workingDir sulla radice: CliApp e GuiFx cercano la cartella `dati/` **relativa alla cartella
// di lavoro**. Senza questa riga Gradle partirebbe da desktop/ e l'app si creerebbe un magazzino
// vuoto tutto suo, lasciando i dati veri dove sono.
tasks.named<JavaExec>("run") {
    standardInput = System.`in`   // la CLI e' interattiva: senza, Gradle le chiude subito lo stdin
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("runGui") {
    group = "application"
    description = "Avvia il client grafico JavaFX"
    mainClass = "com.cutcalculator.gui.GuiApp"
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir
}

tasks.test {
    useJUnitPlatform()
}
