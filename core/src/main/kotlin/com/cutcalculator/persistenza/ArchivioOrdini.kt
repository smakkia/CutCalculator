package com.cutcalculator.persistenza

import com.cutcalculator.catalogo.Catalogo
import com.cutcalculator.catalogo.Sistema
import com.cutcalculator.dominio.Categoria
import com.cutcalculator.dominio.Colore
import com.cutcalculator.dominio.Dimensione
import com.cutcalculator.dominio.Ordine
import com.cutcalculator.dominio.Prezzi
import com.cutcalculator.dominio.Serramento
import com.cutcalculator.dominio.Tipologia
import com.cutcalculator.dominio.Varianti
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Archivio su disco degli ordini: carica e salva la lista di [Ordine] in un CSV semplice, una riga
 * per [Serramento] nel formato
 * `ordine;sistema;tipologia;colore;L;H;HF;quantita;calcolato;prezzoKg;prezzoMq;varianti`.
 *
 * Le **varianti** stanno in un **campo solo**, `RUOLO=nome|RUOLO=nome` (vuoto = tutti i profili
 * base): i ruoli con alternative aumenteranno, e un campo per ruolo avrebbe voluto dire cambiare il
 * formato ogni volta. Anche qui si salva il **nome** e si ri-risolve dal catalogo; se il nome non c'è
 * più, la riga è scartata invece che caricata coi profili base, perché un serramento con le quote
 * sbagliate è peggio di un serramento mancante.
 *
 * Il 9° campo (`1`/`0`) dice se l'ordine è **già stato calcolato**: senza di lui, ricaricando il file
 * gli ordini evasi tornerebbero "da calcolare" e il calcolo globale scalerebbe il magazzino una
 * seconda volta. È ripetuto su ogni riga dell'ordine, che è ridondante ma tiene il formato a una riga
 * per serramento; le righe a **8 campi** (i file salvati prima) valgono "da calcolare". Fa eccezione
 * l'ordine **vuoto**, che si salva con la sola riga-nome e quindi torna sempre da calcolare: senza
 * serramenti non ha materiale da scalare, quindi non cambia nulla.
 *
 * Come per il magazzino, si memorizzano solo i **nomi** di sistema e tipologia: al caricamento la
 * [Tipologia] viene ri-risolta contro il [Catalogo] (le ricette "vere" vengono di lì). Un ordine
 * senza serramenti si salva con una riga **solo nome**, così non va perso. Gli ordini si raggruppano
 * per **nome**: due ordini con lo stesso nome si fondono al reload.
 *
 * Righe vuote, commenti (`#`), con sistema/tipologia sconosciuti, colore vuoto o numeri malformati
 * vengono ignorate: un file corretto a mano non fa crashare l'app. UTF-8, BOM tolto.
 */
class ArchivioOrdini(private val file: Path, private val catalogo: Catalogo) {

    private var righeScartate: Int = 0

    /** Carica gli ordini dal file; lista vuota se il file non esiste ancora. */
    fun carica(): List<Ordine> {
        righeScartate = 0
        if (!Files.exists(file)) {
            return ArrayList()
        }
        val perNome = LinkedHashMap<String, Ordine>()
        try {
            for (riga in Files.readAllLines(file, StandardCharsets.UTF_8)) {
                leggiRiga(riga, perNome)
            }
        } catch (lettura: IOException) {
            throw UncheckedIOException("Impossibile leggere gli ordini da $file", lettura)
        }
        return ArrayList(perNome.values)
    }

    /**
     * Quante righe con contenuto l'ultimo [carica] **non** ha saputo interpretare — di solito una
     * tipologia che nel catalogo non esiste più, o un numero scritto male.
     *
     * Scartarle è voluto (un file corretto a mano non deve far crashare l'app), ma da quando gli
     * ordini si **risalvano da soli** il silenzio non basta più: la prima modifica riscrive il file
     * senza quelle righe, e il dato è perso per sempre. Le UI lo dicono all'avvio, così c'è il tempo
     * di sistemare il file prima che venga riscritto.
     */
    fun righeScartate(): Int = righeScartate

    /** Salva tutti gli ordini sovrascrivendo il file (crea le cartelle mancanti). */
    fun salva(ordini: List<Ordine>) {
        val righe = ArrayList<String>()
        for (ordine in ordini) {
            val serramenti = ordine.serramenti()
            if (serramenti.isEmpty()) {
                righe.add(ordine.nome())   // riga-segnaposto: preserva l'ordine vuoto
                continue
            }
            for (serramento in serramenti) {
                val sistema = nomeSistema(serramento.tipologia)
                    ?: continue   // tipologia non nel catalogo: non salvabile
                val d = serramento.dimensione
                righe.add(
                    listOf(
                        ordine.nome(), sistema, serramento.tipologia.nome(),
                        serramento.colore.nome(), d.L.toString(), d.H.toString(),
                        d.HF.toString(), serramento.quantita.toString(),
                        if (ordine.calcolato()) CALCOLATO else DA_CALCOLARE,
                        serramento.prezzi.alChiloBarre.toString(),
                        serramento.prezzi.alMqVetro.toString(),
                        scriviVarianti(serramento.varianti)
                    ).joinToString(SEP)
                )
            }
        }
        try {
            file.parent?.let { Files.createDirectories(it) }
            Files.write(file, righe, StandardCharsets.UTF_8)
        } catch (scrittura: IOException) {
            throw UncheckedIOException("Impossibile salvare gli ordini su $file", scrittura)
        }
    }

    /** Interpreta una riga e la accumula nella mappa nome→ordine; ignora le righe non valide. */
    private fun leggiRiga(riga: String, perNome: MutableMap<String, Ordine>) {
        val pulita = togliBom(riga).trim()
        if (pulita.isEmpty() || pulita.startsWith("#")) {
            return
        }
        // Lo split di Kotlin tiene i campi vuoti finali, come il limite -1 di Java: il campo
        // varianti, che è l'ultimo ed è spesso vuoto, sparirebbe e la riga sembrerebbe di un
        // formato più vecchio.
        val campi = pulita.split(SEP)
        val nome = campi[0].trim()
        if (nome.isEmpty()) {
            return
        }
        if (campi.size == 1) {
            perNome.computeIfAbsent(nome) { Ordine(it) }   // ordine vuoto
            return
        }
        if (campi.size != 8 && campi.size != 9 && campi.size != 11 && campi.size != 12) {
            righeScartate++
            return
        }
        val serramento = leggiSerramento(campi)
        if (serramento == null) {
            righeScartate++
            return
        }
        val ordine = perNome.computeIfAbsent(nome) { Ordine(it) }
        ordine.aggiungi(serramento)   // rimette l'ordine tra quelli da calcolare...
        // ...quindi lo stato salvato si riapplica dopo. Il flag è il 9° campo sia nel formato a 9
        // sia in quello a 11 (con i prezzi in coda): guardarlo solo nel primo lo perdeva.
        if (campi.size >= 9 && CALCOLATO == campi[8].trim()) {
            ordine.segnaCalcolato()
        }
    }

    /** Ricostruisce un serramento dai campi; `null` se tipologia sconosciuta o dati invalidi. */
    private fun leggiSerramento(campi: List<String>): Serramento? {
        val sistema = catalogo.sistema(campi[1].trim()).orElse(null) ?: return null
        val tipologia = sistema.tipologia(campi[2].trim()).orElse(null) ?: return null
        if (campi[3].isBlank()) {
            return null
        }
        return try {
            val colore = Colore(campi[3].trim())
            val l = campi[4].trim().toDouble()
            val h = campi[5].trim().toDouble()
            val hf = campi[6].trim().toDouble()
            val quantita = campi[7].trim().toInt()
            if (l <= 0 || h <= 0 || hf < 0 || quantita <= 0) {
                return null
            }
            // I prezzi (campi 9-10) sono arrivati dopo: i file più vecchi non li hanno e valgono 0.
            val prezzi = if (campi.size >= 11) {
                Prezzi(prezzo(campi[9]), prezzo(campi[10]))
            } else {
                Prezzi.NESSUNO
            }
            // Le varianti (campo 11) sono arrivate dopo ancora: senza il campo si usano i profili base.
            val lette =
                if (campi.size == 12) leggiVarianti(campi[11].trim(), sistema) else Varianti.NESSUNA
            // Nome di variante non più nel catalogo: meglio scartare che sbagliare.
            val varianti = lette ?: return null
            Serramento(tipologia, colore, Dimensione(l, h, hf), quantita, prezzi, varianti)
        } catch (malformata: IllegalArgumentException) {
            null
        }
    }

    /** Il nome del sistema che contiene questa tipologia, o `null` se non è nel catalogo. */
    private fun nomeSistema(tipologia: Tipologia): String? =
        catalogo.sistemaDi(tipologia).map { it.nome() }.orElse(null)

    private companion object {
        const val SEP = ";"

        /** Dentro il campo varianti: separatore fra le scelte e fra ruolo e nome. */
        const val VARIANTI = "|"
        const val VALORE = "="

        /**
         * Il BOM scritto come escape, non come carattere letterale: un salvataggio in un altro
         * encoding lo trasformerebbe in silenzio in un carattere qualunque.
         */
        const val BOM = '﻿'

        /** 9° campo: l'ordine è già stato evaso da un calcolo globale. */
        const val CALCOLATO = "1"
        const val DA_CALCOLARE = "0"

        /** `RUOLO=nome|RUOLO=nome`, vuoto se non c'è nessuna variante scelta. */
        fun scriviVarianti(varianti: Varianti): String =
            varianti.scelte().entries.joinToString(VARIANTI) { (ruolo, variante) ->
                ruolo.name + VALORE + variante.nome
            }

        /** L'inverso, ri-risolto contro il sistema; `null` se un nome non esiste più. */
        fun leggiVarianti(campo: String, sistema: Sistema): Varianti? {
            if (campo.isEmpty()) {
                return Varianti.NESSUNA
            }
            var varianti = Varianti.NESSUNA
            for (scelta in campo.split(VARIANTI)) {
                val parti = scelta.split(VALORE, limit = 2)
                if (parti.size != 2) {
                    return null
                }
                val ruolo = try {
                    Categoria.valueOf(parti[0].trim())
                } catch (ruoloIgnoto: IllegalArgumentException) {
                    return null
                }
                val variante = sistema.variantiDi(ruolo).firstOrNull { it.nome == parti[1].trim() }
                    ?: return null
                varianti = varianti.con(variante)
            }
            return varianti
        }

        /** Un prezzo scritto nel file: malformato o negativo vale 0 (non impostato), non un errore. */
        fun prezzo(campo: String): Double = try {
            val letto = campo.trim().replace(',', '.').toDouble()
            if (letto > 0) letto else 0.0
        } catch (nonUnNumero: NumberFormatException) {
            0.0
        }

        fun togliBom(riga: String): String =
            if (riga.isNotEmpty() && riga[0] == BOM) riga.substring(1) else riga
    }
}
