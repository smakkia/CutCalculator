package com.cutcalculator.persistenza

import com.cutcalculator.dominio.Pezzo
import com.cutcalculator.dominio.Vetro
import com.cutcalculator.pianificazione.DistintaOrdine
import com.cutcalculator.pianificazione.EvasioneOrdini
import com.cutcalculator.pianificazione.QuotaOrdine
import com.cutcalculator.preventivo.RigaVetro
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/**
 * Archivio dei **risultati dei calcoli**: quando un ordine viene calcolato, la sua distinta, il suo
 * preventivo e i suoi vetri finiscono su disco e restano consultabili anche dopo la chiusura.
 *
 * Tre file CSV nella stessa cartella degli altri dati, con il **nome dell'ordine come prima
 * colonna** — come `ordini.csv`:
 * - `calcoli-distinta.csv` — `ordine;profilo;colore;lunghezza;taglio;quantita`
 * - `calcoli-preventivo.csv` — `ordine;profilo;colore;lunghezza;peso;costo`
 * - `calcoli-vetri.csv` — `ordine;altezza;larghezza;quantita;areaMq;costo`
 *
 * Il preventivo per ordine è una **quota** (le barre sono condivise, vedi [QuotaOrdine]), quindi qui
 * non ci sono barre né sfrido: quelli hanno senso solo sul piano complessivo.
 *
 * Ogni salvataggio **aggiorna** invece di sovrascrivere: le righe degli ordini appena calcolati
 * sostituiscono le loro precedenti, quelle degli altri ordini restano dov'erano. Così l'archivio
 * cresce una sessione alla volta e ricalcolare una commessa non ne duplica i documenti.
 *
 * I file si possono aprire anche fuori dall'app (Excel, stampa), ma [carica] li rilegge per mostrare
 * i documenti di un ordine già calcolato senza doverlo ricalcolare.
 *
 * @param cartella dove mettere i tre CSV (la stessa di magazzino e ordini)
 */
class ArchivioCalcoli(private val cartella: Path) {

    /** Scrive distinta, preventivo e vetri di ogni ordine appena calcolato. */
    fun salva(evasione: EvasioneOrdini) {
        val ordini = evasione.ordini().map { it.nome() }.toSet()
        aggiorna(FILE_DISTINTA, ordini, righeDistinta(evasione.distinte()))
        aggiorna(FILE_PREVENTIVO, ordini, righePreventivo(evasione.quote()))
        aggiorna(FILE_VETRI, ordini, righeVetri(evasione.distinte()))
    }

    /**
     * Cancella dai tre file i documenti di un ordine: lo si chiama quando l'ordine viene **rimosso o
     * rinominato**.
     *
     * Senza, le sue righe resterebbero lì per sempre — i file crescerebbero a ogni commessa chiusa, e
     * soprattutto un **futuro ordine con lo stesso nome** si vedrebbe mostrare la distinta e il
     * preventivo di quello vecchio, che con lui non c'entrano nulla. I file che non esistono ancora
     * non vengono creati apposta per svuotarli.
     */
    fun dimentica(ordine: String) {
        for (nomeFile in listOf(FILE_DISTINTA, FILE_PREVENTIVO, FILE_VETRI)) {
            if (Files.exists(cartella.resolve(nomeFile))) {
                aggiorna(nomeFile, setOf(ordine), emptyList())
            }
        }
    }

    /**
     * Rilegge i documenti di un ordine calcolato. Se non è mai stato calcolato (o i file non ci sono)
     * torna un [CalcoloOrdine.vuoto] calcolo vuoto, non un errore.
     */
    fun carica(ordine: String): CalcoloOrdine {
        val distinta = righeDi(FILE_DISTINTA, ordine, 6).map { campi ->
            CalcoloOrdine.VoceDistinta(
                campi[1], campi[2], valore(campi[3]), campi[4], valore(campi[5]).toInt()
            )
        }
        val preventivo = righeDi(FILE_PREVENTIVO, ordine, 6).map { campi ->
            CalcoloOrdine.VocePreventivo(
                campi[1], campi[2], valore(campi[3]), valore(campi[4]), valore(campi[5])
            )
        }
        val vetri = righeDi(FILE_VETRI, ordine, 6).map { campi ->
            CalcoloOrdine.VoceVetro(
                valore(campi[1]), valore(campi[2]), valore(campi[3]).toInt(),
                valore(campi[4]), valore(campi[5])
            )
        }
        return CalcoloOrdine(ordine, distinta, preventivo, vetri)
    }

    /** Le righe di un file che appartengono a quell'ordine e hanno il numero giusto di campi. */
    private fun righeDi(nomeFile: String, ordine: String, campiAttesi: Int): List<List<String>> {
        val file = cartella.resolve(nomeFile)
        if (!Files.exists(file)) {
            return emptyList()
        }
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8)
                .map { it.split(SEP) }
                .filter { it.size == campiAttesi && it[0].trim() == ordine }
        } catch (lettura: IOException) {
            throw UncheckedIOException("Impossibile leggere i calcoli da $file", lettura)
        }
    }

    /**
     * Riscrive il file tenendo le righe degli **altri** ordini e sostituendo quelle degli ordini
     * appena calcolati. Delle righe già presenti guarda solo il primo campo (il nome dell'ordine):
     * non le interpreta, quindi un file ritoccato a mano non si perde per strada.
     */
    private fun aggiorna(nomeFile: String, ordiniRicalcolati: Set<String>, nuove: List<String>) {
        val file = cartella.resolve(nomeFile)
        val righe = ArrayList<String>()
        try {
            if (Files.exists(file)) {
                for (riga in Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    val ordine = riga.split(SEP)[0].trim()
                    if (riga.isNotBlank() && !ordiniRicalcolati.contains(ordine)) {
                        righe.add(riga)
                    }
                }
            }
            righe.addAll(nuove)
            Files.createDirectories(cartella)
            Files.write(file, righe, StandardCharsets.UTF_8)
        } catch (errore: IOException) {
            throw UncheckedIOException("Impossibile salvare i calcoli su $file", errore)
        }
    }

    private companion object {
        const val SEP = ";"
        const val FILE_DISTINTA = "calcoli-distinta.csv"
        const val FILE_PREVENTIVO = "calcoli-preventivo.csv"
        const val FILE_VETRI = "calcoli-vetri.csv"

        /** Un numero del file; una riga scritta male vale 0 invece di far saltare tutta la lettura. */
        fun valore(campo: String): Double = try {
            campo.trim().toDouble()
        } catch (nonUnNumero: NumberFormatException) {
            0.0
        }

        /** Una riga per gruppo di pezzi uguali: chi taglia non vuole 8 righe identiche. */
        fun righeDistinta(distinte: List<DistintaOrdine>): List<String> {
            val righe = ArrayList<String>()
            for (voce in distinte) {
                // Pezzi uguali (materiale + lunghezza + taglio) contati insieme, in ordine di comparsa.
                val conteggio = LinkedHashMap<String, Int>()
                val esempio = LinkedHashMap<String, Pezzo>()
                for (pezzo in voce.distinta.pezzi()) {
                    val chiave = pezzo.profilo.codice + "|" + pezzo.colore.nome() +
                            "|" + pezzo.lunghezza + "|" + pezzo.tipoTaglio
                    conteggio.merge(chiave, 1, Int::plus)
                    esempio.putIfAbsent(chiave, pezzo)
                }
                conteggio.forEach { (chiave, quantita) ->
                    val p = esempio.getValue(chiave)
                    righe.add(
                        listOf(
                            voce.ordine, p.profilo.codice, p.colore.nome(),
                            numero(p.lunghezza), p.tipoTaglio.name, quantita.toString()
                        ).joinToString(SEP)
                    )
                }
            }
            return righe
        }

        /** Una riga per materiale, con la quota di peso e costo dell'ordine. */
        fun righePreventivo(quote: List<QuotaOrdine>): List<String> {
            val righe = ArrayList<String>()
            for (quota in quote) {
                for (riga in quota.righe()) {
                    righe.add(
                        listOf(
                            quota.ordine(), riga.profilo.codice, riga.colore.nome(),
                            numero(riga.lunghezza), numero(riga.peso), numero(riga.costo)
                        ).joinToString(SEP)
                    )
                }
            }
            return righe
        }

        /** Una riga per misura di lastra: il vetro non si condivide, quindi sono numeri esatti. */
        fun righeVetri(distinte: List<DistintaOrdine>): List<String> {
            val righe = ArrayList<String>()
            for (voce in distinte) {
                for (riga in aggregaPerMisura(voce.distinta.vetri())) {
                    righe.add(
                        listOf(
                            voce.ordine, numero(riga.lunghezza), numero(riga.larghezza),
                            riga.quantita.toString(), numero(riga.areaTotaleMq()), numero(riga.costo)
                        ).joinToString(SEP)
                    )
                }
            }
            return righe
        }

        /**
         * Lastre della stessa misura (e stesso prezzo) in una riga sola, con quantità e costo
         * sommati.
         */
        fun aggregaPerMisura(vetri: List<Vetro>): List<RigaVetro> {
            val perMisura = LinkedHashMap<String, RigaVetro>()
            for (vetro in vetri) {
                perMisura.merge(
                    "${vetro.lunghezza}x${vetro.larghezza}@${vetro.prezzi.alMqVetro}",
                    RigaVetro(vetro.lunghezza, vetro.larghezza, vetro.quantita, vetro.costo())
                ) { vecchia, nuova ->
                    RigaVetro(
                        vecchia.lunghezza, vecchia.larghezza,
                        vecchia.quantita + nuova.quantita, vecchia.costo + nuova.costo
                    )
                }
            }
            return ArrayList(perMisura.values)
        }

        /** Numeri senza notazione scientifica e con il punto decimale, come negli altri CSV. */
        fun numero(valore: Double): String = String.format(Locale.ROOT, "%.3f", valore)
    }
}
