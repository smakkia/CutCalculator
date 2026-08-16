package com.cutcalculator.app

import com.cutcalculator.catalogo.Catalogo
import com.cutcalculator.dominio.Avanzo
import com.cutcalculator.dominio.Ordine
import com.cutcalculator.dominio.Serramento
import com.cutcalculator.formule.Distinta
import com.cutcalculator.formule.GeneratoreDistinta
import com.cutcalculator.ottimizzatore.PianoDiTaglio
import com.cutcalculator.ottimizzatore.Strategia
import com.cutcalculator.persistenza.ArchivioCalcoli
import com.cutcalculator.persistenza.ArchivioImpostazioni
import com.cutcalculator.persistenza.ArchivioMagazzino
import com.cutcalculator.persistenza.ArchivioOrdini
import com.cutcalculator.persistenza.ArchivioRipristino
import com.cutcalculator.persistenza.CalcoloOrdine
import com.cutcalculator.pianificazione.EvasioneOrdini
import com.cutcalculator.pianificazione.PianificatoreOrdini
import com.cutcalculator.preventivo.GeneratorePreventivo
import com.cutcalculator.preventivo.Preventivo
import java.util.Collections

/**
 * Lo **stato** dell'applicazione e le operazioni su di esso, del tutto indipendenti dalla UI (non
 * stampa e non legge nulla). Tiene il [Catalogo], il magazzino degli [Avanzo] e la lista degli
 * [Ordine], ed espone le azioni per modificarli e per far scorrere la pipeline ([calcola]).
 *
 * Una view — oggi `CliView`, domani `GuiFx` — ci si appoggia sopra: legge lo stato e invoca le
 * operazioni, senza duplicarne la gestione. Il magazzino è condiviso da tutti gli ordini; [calcola]
 * riusa gli avanzi correnti senza consumarli.
 *
 * I sei costruttori del Java sono qui un solo costruttore con parametri di default e `@JvmOverloads`,
 * che ne genera esattamente la stessa cascata: con un archivio `null` la rispettiva persistenza è
 * disattivata e quei dati restano solo in memoria.
 *
 * @param archivio             magazzino su disco (caricato all'avvio, risalvato a ogni modifica)
 * @param archivioOrdini       ordini su disco, con le stesse regole
 * @param archivioImpostazioni unità di misura e strategia
 * @param archivioCalcoli      distinta, preventivo e vetri di ogni ordine evaso, consultabili dopo
 * @param archivioRipristino   la fotografia del magazzino prima dell'ultimo calcolo ([ripristina])
 */
class Controller @JvmOverloads constructor(
    private val catalogo: Catalogo,
    private val archivio: ArchivioMagazzino? = null,
    private val archivioOrdini: ArchivioOrdini? = null,
    private val archivioImpostazioni: ArchivioImpostazioni? = null,
    private val archivioCalcoli: ArchivioCalcoli? = null,
    private val archivioRipristino: ArchivioRipristino? = null
) {
    private val magazzino: MutableList<Avanzo> = ArrayList()
    private val ordini: MutableList<Ordine> = ArrayList()
    private var unita: Unita = Unita.PREDEFINITA
    private var strategia: Strategia = Strategia.PREDEFINITA

    init {
        if (archivio != null) {
            magazzino.addAll(archivio.carica())
        }
        if (archivioOrdini != null) {
            ordini.addAll(archivioOrdini.carica())
        }
        if (archivioImpostazioni != null) {
            unita = archivioImpostazioni.caricaUnita()
            strategia = archivioImpostazioni.caricaStrategia()
        }
    }

    // --- Impostazioni ------------------------------------------------------------------

    /**
     * L'unità con cui la UI mostra e legge le misure. Il modello resta sempre in millimetri: questa è
     * solo la lente con cui l'utente ci guarda attraverso.
     */
    fun unita(): Unita = unita

    /** Cambia l'unità di misura e la persiste, se c'è un archivio impostazioni collegato. */
    fun impostaUnita(unita: Unita) {
        this.unita = unita
        archivioImpostazioni?.salvaUnita(unita)
    }

    /**
     * L'euristica con cui il prossimo calcolo impacchetterà i pezzi nelle barre. Come l'unità è
     * un'impostazione: vale da qui in avanti e non tocca i calcoli già fatti, che restano quelli.
     */
    fun strategia(): Strategia = strategia

    /** Cambia l'euristica di taglio e la persiste, se c'è un archivio impostazioni collegato. */
    fun impostaStrategia(strategia: Strategia) {
        this.strategia = strategia
        archivioImpostazioni?.salvaStrategia(strategia)
    }

    // I prezzi non sono un'impostazione: si dichiarano su ogni Serramento, dove si sceglie anche il
    // colore da cui dipendono. Le UI propongono quelli dell'ultimo serramento inserito.

    // --- Lettura dello stato -----------------------------------------------------------

    fun catalogo(): Catalogo = catalogo

    /** Vista in sola lettura del magazzino (l'ordine è quello di inserimento). */
    fun magazzino(): List<Avanzo> = Collections.unmodifiableList(ArrayList(magazzino))

    /** Numero totale di spezzoni a magazzino: la somma delle quantità di ogni [Avanzo]. */
    fun totaleAvanzi(): Int = magazzino.sumOf { it.quantita }

    /** Vista in sola lettura degli ordini (l'ordine è quello di inserimento). */
    fun ordini(): List<Ordine> = Collections.unmodifiableList(ArrayList(ordini))

    /**
     * Gli ordini **da calcolare**: quelli che il prossimo calcolo globale prenderà in carico. Un
     * ordine ci resta finché non viene evaso, e ci **ritorna** se lo si modifica.
     */
    fun ordiniDaCalcolare(): List<Ordine> = ordini.filter { !it.calcolato() }

    /** Gli ordini **già calcolati**: il loro materiale è stato scalato dal magazzino. */
    fun ordiniCalcolati(): List<Ordine> = ordini.filter { it.calcolato() }

    // --- Operazioni sul magazzino ------------------------------------------------------

    fun aggiungiAvanzo(avanzo: Avanzo) {
        magazzino.add(avanzo)
        salva()
    }

    /**
     * Toglie `quantita` spezzoni dall'avanzo alla posizione data. Se sono tutti (o più), rimuove
     * l'intera riga; altrimenti la sostituisce con una copia dalla quantità ridotta (l'[Avanzo] è
     * immutabile).
     *
     * @return l'avanzo aggiornato che resta a magazzino, oppure `null` se la riga è sparita
     */
    fun rimuoviAvanzo(indice: Int, quantita: Int): Avanzo? {
        val avanzo = magazzino[indice]
        val rimasto: Avanzo?
        if (quantita >= avanzo.quantita) {
            magazzino.removeAt(indice)
            rimasto = null
        } else {
            rimasto = Avanzo(
                avanzo.profilo, avanzo.colore,
                avanzo.lunghezza, avanzo.quantita - quantita
            )
            magazzino[indice] = rimasto
        }
        salva()
        return rimasto
    }

    /**
     * Svuota il magazzino: rimuove **tutti** gli spezzoni presenti e persiste.
     *
     * @return quanti spezzoni sono stati rimossi in totale
     */
    fun svuotaMagazzino(): Int {
        val totale = totaleAvanzi()
        magazzino.clear()
        salva()
        return totale
    }

    /** Persiste il magazzino se c'è un archivio collegato (altrimenti resta solo in memoria). */
    private fun salva() {
        archivio?.salva(magazzino)
    }

    // --- Operazioni sugli ordini -------------------------------------------------------

    /**
     * Crea un ordine con questo nome, lo aggiunge e lo restituisce (l'[Ordine] è modificabile).
     * Il colore si sceglie sul singolo [Serramento], non sull'ordine.
     *
     * @throws IllegalArgumentException se il nome è già di un altro ordine (vedi [nomeLibero])
     */
    fun nuovoOrdine(nome: String): Ordine {
        pretendiNomeLibero(nome, null)
        val ordine = Ordine(nome)
        ordini.add(ordine)
        salvaOrdini()
        return ordine
    }

    fun rimuoviOrdine(ordine: Ordine) {
        ordini.remove(ordine)
        salvaOrdini()
        archivioCalcoli?.dimentica(ordine.nome())   // niente documenti orfani sul disco
        // ...ne' un punto di ripristino che nomina un ordine che non c'e' piu': resterebbe offerto
        // e non troverebbe niente da annullare.
        archivioRipristino?.dimentica(ordine.nome())
    }

    /**
     * `true` se nessun altro ordine si chiama già così. Il nome è la **chiave** con cui gli ordini
     * vengono riletti da disco (`ArchivioOrdini` li raggruppa per nome): due omonimi si fonderebbero
     * in uno solo al prossimo avvio, coi serramenti mescolati. Il vincolo sta qui e non nelle view
     * perché è una regola della persistenza, non di come si chiede il nome.
     *
     * @param tranne l'ordine a cui il nome appartiene già (nel rinomina), oppure `null`
     */
    fun nomeLibero(nome: String, tranne: Ordine?): Boolean =
        ordini.none { it !== tranne && it.nome() == nome }

    private fun pretendiNomeLibero(nome: String, tranne: Ordine?) {
        require(nomeLibero(nome, tranne)) { "Esiste gia' un ordine chiamato \"$nome\"" }
    }

    /**
     * Un nome libero da proporre per il prossimo ordine: `"Ordine N"` col primo N non ancora usato.
     * Contare gli ordini non basta — dopo una rimozione il conteggio ripropone un nome occupato, ed è
     * proprio così che nascevano gli omonimi.
     */
    fun nomeOrdineProposto(): String {
        var n = ordini.size + 1
        while (!nomeLibero("Ordine $n", null)) {
            n++
        }
        return "Ordine $n"
    }

    /**
     * Aggiunge un serramento a un ordine e **persiste**.
     *
     * Le modifiche ai serramenti passano di qui e non direttamente dall'[Ordine], che pure sarebbe
     * capace di farle: è l'unico modo perché il salvataggio automatico se ne accorga. La regola è la
     * stessa del magazzino — lo stato lo possiede il controller, le view glielo chiedono e gli
     * delegano le modifiche.
     */
    fun aggiungiSerramento(ordine: Ordine, serramento: Serramento) {
        ordine.aggiungi(serramento)
        salvaOrdini()
    }

    /** Toglie il serramento alla posizione data e persiste. */
    fun rimuoviSerramento(ordine: Ordine, indice: Int) {
        ordine.rimuovi(indice)
        salvaOrdini()
    }

    /** Sostituisce un serramento con la versione corretta e persiste. */
    fun sostituisciSerramento(ordine: Ordine, indice: Int, serramento: Serramento) {
        ordine.sostituisci(indice, serramento)
        salvaOrdini()
    }

    /**
     * Rinomina l'ordine e persiste (il nome non cambia lo stato di calcolo). I documenti già
     * calcolati sono archiviati **sotto il vecchio nome** e non seguono la rinomina: si dimenticano,
     * invece di restare lì a farsi trovare da un futuro omonimo.
     *
     * Il **punto di ripristino** invece la segue, e non è un'incoerenza: i documenti descrivono un
     * calcolo che dopo la rinomina non si può più rifare uguale, mentre l'annullamento riguarda il
     * *magazzino*, che il cambio di nome non tocca. Senza, rinominare un ordine appena calcolato
     * lasciava un ripristino che nominava un ordine inesistente.
     *
     * @throws IllegalArgumentException se il nome è già di un altro ordine
     */
    fun rinominaOrdine(ordine: Ordine, nome: String) {
        pretendiNomeLibero(nome, ordine)
        val precedente = ordine.nome()
        ordine.rinomina(nome)
        salvaOrdini()
        if (precedente != nome) {
            archivioCalcoli?.dimentica(precedente)
            archivioRipristino?.rinomina(precedente, nome)
        }
    }

    /** `true` se c'è un archivio ordini collegato (salvataggio/caricamento disponibili). */
    fun persistenzaOrdiniAttiva(): Boolean = archivioOrdini != null

    /**
     * Quante righe del file ordini l'ultimo caricamento non ha saputo interpretare: righe che il
     * prossimo salvataggio automatico farebbe sparire. Le UI lo dicono all'avvio — zero significa che
     * il file è stato letto per intero.
     */
    fun righeOrdiniScartate(): Int = archivioOrdini?.righeScartate() ?: 0

    /**
     * Salva su disco tutti gli ordini correnti; no-op se non c'è un archivio collegato. Lo chiamano
     * da sole tutte le operazioni che toccano gli ordini: non è un comando dell'utente.
     */
    fun salvaOrdini() {
        archivioOrdini?.salva(ordini)
    }

    /**
     * Rimpiazza gli ordini in memoria con quelli caricati da disco; no-op (ritorna 0) se non c'è un
     * archivio collegato.
     *
     * @return quanti ordini sono stati caricati
     */
    fun caricaOrdini(): Int {
        if (archivioOrdini == null) {
            return 0
        }
        val caricati = archivioOrdini.carica()
        ordini.clear()
        ordini.addAll(caricati)
        return caricati.size
    }

    // --- Pipeline ----------------------------------------------------------------------

    /**
     * Fa scorrere l'ordine lungo le tre fasi (distinta → piano → preventivo) riusando gli avanzi
     * correnti del magazzino **senza consumarli**, e restituisce i tre risultati insieme.
     *
     * **Non è più esposto dalle UI** (2026-08-14): il "calcolo provvisorio" del singolo ordine
     * confondeva, perché mostrava un piano che il calcolo globale poi rifaceva diverso — lì gli
     * ordini si uniscono e condividono le barre. Resta qui solo perché lo usano i test: quando li
     * rivedremo, questo metodo e il record [Risultato] se ne vanno insieme a loro.
     *
     * @throws IllegalArgumentException se un pezzo è più lungo della barra standard
     */
    fun calcola(ordine: Ordine): Risultato {
        val distinta = GeneratoreDistinta().genera(ordine)
        val piano = strategia.crea().ottimizza(distinta, magazzino)
        val preventivo = GeneratorePreventivo().genera(piano, distinta.vetri(), sogliaRitaglio())
        return Risultato(distinta, piano, preventivo)
    }

    /**
     * I documenti dell'ultimo calcolo di quest'ordine — distinta, preventivo, vetri e costo — riletti
     * da disco. Vuoto se l'ordine non è mai stato calcolato o se non c'è un archivio collegato.
     */
    fun calcoloDi(ordine: Ordine): CalcoloOrdine =
        archivioCalcoli?.carica(ordine.nome())
            ?: CalcoloOrdine(ordine.nome(), emptyList(), emptyList(), emptyList())

    /** La soglia (mm) sopra cui un ritaglio rientra in magazzino come nuovo avanzo. */
    fun sogliaRitaglio(): Double = PianificatoreOrdini.SOGLIA_RITAGLIO_DEFAULT

    /**
     * Calcolo **globale**: pianifica insieme gli ordini **da calcolare** (non quelli già evasi) sul
     * magazzino condiviso, così i loro pezzi condividono le barre, e poi **applica** il risultato —
     * sostituisce il magazzino con quello aggiornato (avanzi usati consumati, ritagli sopra soglia
     * rientrati), lo persiste e segna gli ordini come calcolati.
     *
     * Gli ordini già calcolati restano fuori: il loro materiale è stato scalato una volta e
     * ripassarci sopra lo conterebbe due volte.
     *
     * @throws IllegalArgumentException se un pezzo è più lungo della barra standard
     */
    fun evadiOrdini(): EvasioneOrdini {
        val daCalcolare = ordiniDaCalcolare()
        val evasione = PianificatoreOrdini(strategia.crea()).pianifica(daCalcolare, magazzino)
        // La fotografia si prende PRIMA di toccare il magazzino: e' l'unico momento in cui esiste
        // ancora lo stato a cui un eventuale ripristino dovra' tornare.
        archivioRipristino?.salva(daCalcolare.map { it.nome() }, magazzino())
        magazzino.clear()
        magazzino.addAll(evasione.magazzinoAggiornato())
        salva()
        daCalcolare.forEach { it.segnaCalcolato() }
        salvaOrdini()   // e' cambiato lo stato: sono passati tra i calcolati
        archivioCalcoli?.salva(evasione)   // distinta, preventivo e vetri di ogni ordine evaso
        return evasione
    }

    // --- Ripristino (annulla l'ultimo calcolo) -----------------------------------------

    /**
     * Gli ordini dell'**ultimo calcolo**, cioè quelli che un [ripristina] rimetterebbe da calcolare;
     * vuoto se non c'è niente da annullare. Le UI lo usano per abilitare il comando e per dire, nella
     * conferma, chi altro verrà coinvolto.
     */
    fun ordiniRipristinabili(): List<String> =
        archivioRipristino?.carica()?.map { it.ordini() }?.orElse(emptyList()) ?: emptyList()

    /** `true` se quest'ordine fa parte dell'ultimo calcolo e si può quindi ripristinare. */
    fun ripristinabile(ordine: Ordine): Boolean = ordiniRipristinabili().contains(ordine.nome())

    /**
     * **Annulla l'ultimo calcolo globale**: riporta il magazzino esattamente com'era prima (avanzi
     * consumati restituiti, ritagli rientrati tolti), rimette **tutti** gli ordini di quel calcolo tra
     * quelli da calcolare e ne butta via i documenti archiviati.
     *
     * Il gruppo si ripristina **tutto insieme** perché il calcolo lo aveva unito in un piano solo, con
     * le barre condivise: la parte di magazzino di un singolo ordine non esiste — esisterebbe solo una
     * stima, e una stima qui vorrebbe dire lasciare per sempre il magazzino diverso dal vero.
     *
     * Si può annullare **solo l'ultimo** calcolo: di quelli prima non si sa più da dove erano partiti,
     * e riscrivere un magazzino vecchio cancellerebbe l'effetto dei calcoli successivi.
     *
     * @param ordine uno qualunque degli ordini di quel calcolo
     * @return i nomi degli ordini tornati da calcolare (tutto il gruppo, quelli ancora esistenti)
     * @throws IllegalStateException se quest'ordine non fa parte dell'ultimo calcolo
     */
    fun ripristina(ordine: Ordine): List<String> {
        val ripristino = archivioRipristino?.carica()?.orElse(null)
        if (ripristino == null || !ripristino.contiene(ordine.nome())) {
            throw IllegalStateException(
                "\"" + ordine.nome() + "\" non fa parte dell'ultimo calcolo: si puo' annullare solo" +
                        " quello, perche' degli altri non si sa piu' com'era il magazzino di partenza"
            )
        }
        magazzino.clear()
        magazzino.addAll(ripristino.magazzino())
        salva()

        val tornati = ArrayList<String>()
        for (candidato in ordini) {
            if (ripristino.contiene(candidato.nome())) {
                candidato.segnaDaCalcolare()
                tornati.add(candidato.nome())
                // I documenti descrivevano un calcolo che non vale piu': tenerli sarebbe peggio che
                // non averli, perche' sembrerebbero ancora buoni.
                archivioCalcoli?.dimentica(candidato.nome())
            }
        }
        salvaOrdini()
        archivioRipristino.cancella()   // annullato una volta, non si annulla due
        return tornati
    }

    /** I tre output della pipeline per un ordine. Vedi [calcola]: non più usato dalle UI. */
    @JvmRecord
    data class Risultato(
        val distinta: Distinta,
        val piano: PianoDiTaglio,
        val preventivo: Preventivo
    )
}
