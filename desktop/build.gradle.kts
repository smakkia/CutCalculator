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

// --- Pacchetto distribuibile ------------------------------------------------------------

/**
 * Cancella una cartella davvero, togliendo l'attributo di sola lettura e riprovando: su Windows la
 * prima passata può lasciarsi dietro un file ancora agganciato da chi l'ha appena scritto o letto.
 * Fallisce rumorosamente se non ci riesce, invece di lasciare che sia jpackage a lamentarsene dopo.
 */
fun svuota(cartella: File) {
    repeat(3) { tentativo ->
        if (!cartella.exists()) return
        cartella.walkBottomUp().forEach { file ->
            file.setWritable(true)
            file.delete()
        }
        if (cartella.exists() && tentativo < 2) {
            Thread.sleep(500)
        }
    }
    if (cartella.exists()) {
        throw GradleException(
            "Non riesco a svuotare $cartella: qualcosa la tiene aperta (l'app in esecuzione?)."
        )
    }
}

/**
 * Il pacchetto da allegare a una release: una cartella con l'eseguibile grafico e **la sua JRE
 * dentro**, così chi lo riceve non deve avere Java installato.
 *
 * Contiene la sola **GUI**: il client testuale resta uno strumento da sviluppo, che si lancia con
 * `./gradlew run` da chi ha il progetto.
 *
 * ⚠️ È **solo per Windows**, e non per pigrizia: il plugin JavaFX sceglie il classifier della
 * piattaforma al momento della build, quindi in `lib/` finiscono i `.jar` con dentro le `.dll` di
 * Windows. `jpackage` per giunta non fa cross-compilazione: un pacchetto per Linux o per Mac va
 * costruito *su* Linux o su Mac.
 */
val pacchettoWindows by tasks.registering {
    group = "distribution"
    description = "Costruisce il pacchetto Windows autonomo (eseguibili + JRE inclusa)"
    dependsOn(tasks.named("installDist"))

    val distribuzione = layout.buildDirectory.dir("install/desktop/lib")
    val destinazione = layout.buildDirectory.dir("jpackage")
    // ⚠️ `rootProject.version` e non `project.version`: la versione e' dichiarata **solo** sulla
    // radice, quindi qui dentro varrebbe "unspecified" — e jpackage rifiuta di partire.
    val versione = rootProject.version.toString()
    // Il jpackage della toolchain, non quello che capita nel PATH: devono essere lo stesso JDK con
    // cui il codice e' stato compilato.
    val jdk = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }

    inputs.dir(distribuzione)
    outputs.dir(destinazione)

    doLast {
        val fuori = destinazione.get().asFile
        // jpackage rifiuta di scrivere su una cartella che esiste gia', quindi si riparte pulito.
        // `deleteRecursively()` da solo non basta: su Windows un eseguibile appena creato resta
        // agganciato per qualche istante (antivirus, indicizzazione) e la cancellazione fallisce
        // **in silenzio**, restituendo false invece di lanciare — al secondo lancio il task moriva
        // con "destination directory already exists" senza dire perche'.
        svuota(fuori)
        fuori.mkdirs()
        val esito = providers.exec {
            isIgnoreExitValue = true   // altrimenti Gradle muore prima di far vedere il perche'
            executable = jdk.get().metadata.installationPath.file("bin/jpackage").asFile.absolutePath
            args(
                "--type", "app-image",
                "--name", "CutCalculator",
                "--app-version", versione,
                "--vendor", "Simone Macchia",
                "--description", "Ottimizzatore di tagli per serramenti in alluminio",
                "--input", distribuzione.get().asFile.absolutePath,
                "--main-jar", "desktop.jar",
                "--main-class", "com.cutcalculator.gui.GuiApp",
                "--dest", fuori.absolutePath,
                // Senza, JavaFX avvisa a ogni avvio che carica librerie native — e dalle prossime
                // versioni del JDK quelle chiamate verranno **bloccate**, non solo segnalate.
                "--java-options", "--enable-native-access=ALL-UNNAMED",
                // La JRE si costruisce a mano: jpackage da solo deduce i moduli dai jar e non vede
                // quelli che JavaFX carica per riflessione, producendo un pacchetto che non parte.
                "--add-modules", listOf(
                    "java.base", "java.datatransfer", "java.desktop", "java.logging",
                    "java.management", "java.naming", "java.prefs", "java.scripting",
                    "java.sql", "java.xml", "jdk.unsupported", "jdk.xml.dom"
                ).joinToString(",")
            )
        }
        val uscita = esito.result.get().exitValue
        if (uscita != 0) {
            throw GradleException(
                "jpackage e' fallito (codice $uscita):\n" +
                        esito.standardError.asText.get() + esito.standardOutput.asText.get()
            )
        }
        logger.lifecycle("Pacchetto pronto in ${fuori.resolve("CutCalculator")}")
    }
}

/** Lo zip da caricare come asset della release. */
tasks.register<Zip>("zipWindows") {
    group = "distribution"
    description = "Comprime il pacchetto Windows nello zip da allegare alla release"
    dependsOn(pacchettoWindows)
    from(layout.buildDirectory.dir("jpackage/CutCalculator"))
    into("CutCalculator")
    archiveFileName = "CutCalculator-${rootProject.version}-windows-x64.zip"
    destinationDirectory = layout.buildDirectory.dir("distribuzione")
}
