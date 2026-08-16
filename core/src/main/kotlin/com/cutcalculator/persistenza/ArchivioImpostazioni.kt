package com.cutcalculator.persistenza

import com.cutcalculator.app.Unita
import com.cutcalculator.ottimizzatore.Strategia
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/**
 * Archivio su disco delle preferenze dell'utente — l'[Unita] con cui mostrare e leggere le lunghezze
 * e la [Strategia] euristica di taglio da usare nei calcoli; domani anche soglia di ritaglio e barra
 * standard. I **prezzi** qui non ci sono: non sono una preferenza, si dichiarano su ogni serramento.
 *
 * Formato `properties` (una riga `chiave=valore`), UTF-8: leggibile e correggibile a mano. File
 * assente, illeggibile o con un valore sconosciuto non sono un errore: si torna semplicemente al
 * valore predefinito, come per gli altri archivi.
 *
 * Ogni `salva*` riscrive il file **preservando le altre chiavi**: le impostazioni sono indipendenti
 * fra loro, cambiare l'unità non deve azzerare i prezzi.
 */
class ArchivioImpostazioni(private val file: Path) {

    /** L'unità salvata, o [Unita.PREDEFINITA] se non c'è o non si capisce. */
    fun caricaUnita(): Unita = Unita.daNome(leggi().getProperty(CHIAVE_UNITA))

    /** Salva l'unità scelta, creando le cartelle mancanti. */
    fun salvaUnita(unita: Unita) {
        salvaChiave(CHIAVE_UNITA, unita.name)
    }

    /** L'euristica di taglio salvata, o la [Strategia.PREDEFINITA] se non c'è o non si capisce. */
    fun caricaStrategia(): Strategia = Strategia.daNome(leggi().getProperty(CHIAVE_STRATEGIA))

    /** Salva l'euristica scelta. */
    fun salvaStrategia(strategia: Strategia) {
        salvaChiave(CHIAVE_STRATEGIA, strategia.name)
    }

    /** Scrive una preferenza **preservando le altre**: sono indipendenti fra loro. */
    private fun salvaChiave(chiave: String, valore: String) {
        val impostazioni = leggi()
        impostazioni.setProperty(chiave, valore)
        scrivi(impostazioni)
    }

    /** Le impostazioni su disco, o vuote se il file manca o è illeggibile. */
    private fun leggi(): Properties {
        val impostazioni = Properties()
        if (!Files.exists(file)) {
            return impostazioni
        }
        try {
            Files.newBufferedReader(file, StandardCharsets.UTF_8).use { impostazioni.load(it) }
        } catch (illeggibile: IOException) {
            return Properties()
        } catch (illeggibile: IllegalArgumentException) {
            return Properties()
        }
        return impostazioni
    }

    private fun scrivi(impostazioni: Properties) {
        try {
            file.parent?.let { Files.createDirectories(it) }
            Files.newBufferedWriter(file, StandardCharsets.UTF_8).use {
                impostazioni.store(it, "Impostazioni di CutCalculator")
            }
        } catch (scrittura: IOException) {
            throw UncheckedIOException("Impossibile salvare le impostazioni su $file", scrittura)
        }
    }

    private companion object {
        const val CHIAVE_UNITA = "unita"
        const val CHIAVE_STRATEGIA = "strategia"
    }
}
