package com.cutcalculator.persistenza

import com.cutcalculator.catalogo.Catalogo
import com.cutcalculator.catalogo.Sistema
import com.cutcalculator.dominio.Avanzo
import com.cutcalculator.dominio.Colore
import com.cutcalculator.dominio.Profilo
import com.cutcalculator.dominio.RegolaTaglio
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Archivio su disco del magazzino: carica e salva la lista di [Avanzo] in un CSV semplice, una riga
 * per avanzo nel formato `codiceProfilo;colore;lunghezza;quantita`.
 *
 * Del profilo si memorizza solo il **codice**: al caricamento viene ri-risolto contro il [Catalogo]
 * (i profili "veri" vengono già di lì), così il file resta corto e i profili restano canonici. Il
 * colore è testo libero (viene normalizzato dal [Colore]). Una riga con codice non nel catalogo,
 * colore vuoto, malformata, vuota o che inizia con `#` (commento) viene semplicemente ignorata: un
 * file corretto a mano non fa crashare l'app (le vecchie righe a 3 campi, senza colore, ricadono
 * qui). Il file è UTF-8 e un eventuale BOM in testa viene tolto.
 */
class ArchivioMagazzino(private val file: Path, catalogo: Catalogo) {

    private val profiliPerCodice: Map<String, Profilo> = indicizzaProfili(catalogo)

    /** Carica gli avanzi dal file; lista vuota se il file non esiste ancora (primo avvio). */
    fun carica(): List<Avanzo> {
        if (!Files.exists(file)) {
            return ArrayList()
        }
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8).mapNotNull { leggiRiga(it) }
        } catch (lettura: IOException) {
            throw UncheckedIOException("Impossibile leggere il magazzino da $file", lettura)
        }
    }

    /** Salva l'intero magazzino sovrascrivendo il file (crea le cartelle mancanti). */
    fun salva(magazzino: List<Avanzo>) {
        val righe = magazzino.map {
            it.profilo.codice + SEP + it.colore.nome() + SEP + it.lunghezza + SEP + it.quantita
        }
        try {
            file.parent?.let { Files.createDirectories(it) }
            Files.write(file, righe, StandardCharsets.UTF_8)
        } catch (scrittura: IOException) {
            throw UncheckedIOException("Impossibile salvare il magazzino su $file", scrittura)
        }
    }

    /** Interpreta una riga; `null` se vuota, commento, malformata, profilo o colore mancante. */
    private fun leggiRiga(riga: String): Avanzo? {
        val pulita = togliBom(riga).trim()
        if (pulita.isEmpty() || pulita.startsWith("#")) {
            return null
        }
        // Lo split di Kotlin tiene i campi vuoti finali, come il limite -1 di Java: senza, un campo
        // finale vuoto sparirebbe e la riga sembrerebbe averne tre (cioe' una vecchia riga senza
        // colore) invece che una malformata.
        val campi = pulita.split(SEP)
        if (campi.size != 4) {
            return null
        }
        val profilo = profiliPerCodice[campi[0].trim()]
        if (profilo == null || campi[1].isBlank()) {
            return null
        }
        return try {
            val colore = Colore(campi[1].trim())
            val lunghezza = campi[2].trim().toDouble()
            val quantita = campi[3].trim().toInt()
            if (lunghezza > 0 && quantita > 0) Avanzo(profilo, colore, lunghezza, quantita) else null
        } catch (malformata: NumberFormatException) {
            null
        }
    }

    private companion object {
        const val SEP = ";"
        const val BOM = '﻿'

        fun togliBom(riga: String): String =
            if (riga.isNotEmpty() && riga[0] == BOM) riga.substring(1) else riga

        /**
         * Mappa codice → profilo con tutti i profili distinti del catalogo (primo che vince).
         *
         * Si passa da [Sistema.profili], che è la **stessa** lista da cui le UI fanno scegliere il
         * profilo di un avanzo: comprende quindi anche i profili delle **varianti** (telaio
         * maggiorato, anta maggiorata), che non compaiono in nessuna [RegolaTaglio]. Indicizzando le
         * sole regole, un avanzo di telaio maggiorato si poteva dichiarare e salvare ma non si
         * sarebbe più ricaricato: la riga veniva scartata in silenzio e il primo salvataggio
         * successivo la cancellava.
         */
        fun indicizzaProfili(catalogo: Catalogo): Map<String, Profilo> {
            val perCodice = LinkedHashMap<String, Profilo>()
            for (sistema in catalogo.sistemi()) {
                for (profilo in sistema.profili()) {
                    perCodice.putIfAbsent(profilo.codice, profilo)
                }
            }
            return perCodice
        }
    }
}
