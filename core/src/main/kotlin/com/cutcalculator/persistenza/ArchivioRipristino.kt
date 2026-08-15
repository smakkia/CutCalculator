package com.cutcalculator.persistenza

import com.cutcalculator.catalogo.Catalogo
import com.cutcalculator.dominio.Avanzo
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.Optional

/**
 * La fotografia dell'**ultimo calcolo globale**, tenuta da parte per poterlo annullare: quali ordini
 * vi hanno partecipato e **com'era il magazzino prima**.
 *
 * Serve al "ripristina ordine". Il calcolo consuma gli avanzi usati e fa rientrare i ritagli sopra
 * soglia: senza sapere da dove si era partiti, quell'effetto non è ricostruibile — i ritagli nascono
 * dal piano **complessivo**, non da un singolo ordine, e tornare indietro "a occhio" lascerebbe il
 * magazzino diverso dal vero. Qui invece il ripristino è esatto: si riscrive lo stato di prima.
 *
 * Tre scelte, tutte volute:
 * - **Solo l'ultimo calcolo.** Ogni calcolo sovrascrive la fotografia precedente. Annullare un
 *   calcolo vecchio non avrebbe senso: il magazzino nel frattempo è stato mosso da quelli dopo, e
 *   riscriverci sopra uno stato antico cancellerebbe il loro effetto.
 * - **Tutto il gruppo.** Si salvano *tutti* gli ordini del calcolo, perché le barre erano condivise:
 *   non esiste "la parte di magazzino" di un ordine solo, e ripristinarne uno solo sarebbe una stima,
 *   non un annullamento.
 * - **Il magazzino nel formato di [ArchivioMagazzino]**, di cui questa classe si serve davvero invece
 *   di ricopiarne il parsing: stesso CSV, stesse regole, stessa gestione delle righe malformate.
 *
 * L'elenco degli ordini sta in un file a parte, un nome per riga. È lui a dire se c'è qualcosa da
 * annullare: un calcolo ha sempre almeno un ordine, mentre il magazzino di prima può essere
 * legittimamente vuoto.
 *
 * @param cartella dove mettere i due file (la stessa degli altri dati)
 */
class ArchivioRipristino(cartella: Path, catalogo: Catalogo) {

    private val fileOrdini: Path = cartella.resolve(FILE_ORDINI)
    private val magazzinoPrima = ArchivioMagazzino(cartella.resolve(FILE_MAGAZZINO), catalogo)

    /**
     * Registra un calcolo appena fatto, **sostituendo** la fotografia precedente: da qui in avanti è
     * questo il calcolo annullabile.
     *
     * @param ordini    i nomi degli ordini che vi hanno partecipato
     * @param magazzino il magazzino **com'era prima** che il calcolo lo consumasse
     */
    fun salva(ordini: List<String>, magazzino: List<Avanzo>) {
        try {
            fileOrdini.parent?.let { Files.createDirectories(it) }
            Files.write(fileOrdini, ordini, StandardCharsets.UTF_8)
        } catch (scrittura: IOException) {
            throw UncheckedIOException(
                "Impossibile salvare il punto di ripristino su $fileOrdini", scrittura
            )
        }
        magazzinoPrima.salva(magazzino)
    }

    /** Il calcolo annullabile, o vuoto se non ce n'è nessuno (mai calcolato, o già ripristinato). */
    fun carica(): Optional<Ripristino> {
        if (!Files.exists(fileOrdini)) {
            return Optional.empty()
        }
        val ordini = ArrayList<String>()
        try {
            for (riga in Files.readAllLines(fileOrdini, StandardCharsets.UTF_8)) {
                val nome = togliBom(riga).trim()
                if (nome.isNotEmpty() && !nome.startsWith("#")) {
                    ordini.add(nome)
                }
            }
        } catch (lettura: IOException) {
            throw UncheckedIOException(
                "Impossibile leggere il punto di ripristino da $fileOrdini", lettura
            )
        }
        return if (ordini.isEmpty()) Optional.empty()
        else Optional.of(Ripristino(ordini, magazzinoPrima.carica()))
    }

    /**
     * Dimentica il calcolo annullabile: lo si chiama **dopo** averlo ripristinato, perché a quel punto
     * non c'è più niente da annullare e lasciare i file inviterebbe a farlo due volte.
     */
    fun cancella() {
        try {
            Files.deleteIfExists(fileOrdini)
            Files.deleteIfExists(fileOrdini.resolveSibling(FILE_MAGAZZINO))
        } catch (cancellazione: IOException) {
            throw UncheckedIOException(
                "Impossibile cancellare il punto di ripristino $fileOrdini", cancellazione
            )
        }
    }

    /**
     * Un calcolo che si può annullare: chi c'era dentro e a cosa va riportato il magazzino.
     *
     * @param ordini    i nomi degli ordini del calcolo (tornano tutti "da calcolare")
     * @param magazzino lo stato a cui riportare il magazzino
     */
    class Ripristino(ordini: List<String>, magazzino: List<Avanzo>) {

        private val ordini: List<String> = Collections.unmodifiableList(ArrayList(ordini))
        private val magazzino: List<Avanzo> = Collections.unmodifiableList(ArrayList(magazzino))

        fun ordini(): List<String> = ordini

        fun magazzino(): List<Avanzo> = magazzino

        /** `true` se quest'ordine ha partecipato al calcolo annullabile. */
        fun contiene(ordine: String): Boolean = ordini.contains(ordine)

        override fun equals(other: Any?): Boolean = this === other ||
                (other is Ripristino && ordini == other.ordini && magazzino == other.magazzino)

        override fun hashCode(): Int = 31 * ordini.hashCode() + magazzino.hashCode()

        override fun toString(): String = "Ripristino[ordini=$ordini, magazzino=$magazzino]"
    }

    private companion object {
        const val FILE_ORDINI = "ripristino-ordini.csv"
        const val FILE_MAGAZZINO = "ripristino-magazzino.csv"
        const val BOM = '﻿'

        fun togliBom(riga: String): String =
            if (riga.isNotEmpty() && riga[0] == BOM) riga.substring(1) else riga
    }
}
