package com.cutcalculator.android

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.cutcalculator.app.Controller
import com.cutcalculator.app.Unita
import com.cutcalculator.catalogo.Catalogo
import com.cutcalculator.dominio.Avanzo
import com.cutcalculator.dominio.Colore
import com.cutcalculator.dominio.Dimensione
import com.cutcalculator.dominio.Ordine
import com.cutcalculator.dominio.Prezzi
import com.cutcalculator.dominio.Profilo
import com.cutcalculator.dominio.Serramento
import com.cutcalculator.dominio.Tipologia
import com.cutcalculator.ottimizzatore.Strategia
import com.cutcalculator.persistenza.ArchivioCalcoli
import com.cutcalculator.persistenza.ArchivioImpostazioni
import com.cutcalculator.persistenza.ArchivioMagazzino
import com.cutcalculator.persistenza.ArchivioOrdini
import com.cutcalculator.persistenza.ArchivioRipristino
import com.cutcalculator.persistenza.CalcoloOrdine
import com.cutcalculator.pianificazione.EvasioneOrdini

/**
 * Il ponte fra le schermate Compose e il [Controller] di `:core` — lo **stesso** che usano la CLI e
 * la GUI JavaFX. Qui non c'è logica di calcolo: solo lo stato osservabile che Compose sa ridisegnare.
 *
 * Il `Controller` è un oggetto mutabile che non notifica nessuno quando cambia (è nato per la CLI):
 * dopo ogni operazione si richiamano quindi le liste e le si riassegnano a uno `State`, che è il
 * segnale con cui Compose ricompone. Lo stato "vero" resta comunque su disco, perché ogni modifica
 * viene già persistita da sé.
 *
 * I dati stanno in `filesDir/dati`, la sandbox privata dell'app: stessi file CSV del desktop, stesso
 * formato, solo in un'altra cartella.
 */
class CutCalculatorViewModel(app: Application) : AndroidViewModel(app) {

    private val catalogo: Catalogo = Catalogo.completo()
    private val controller: Controller

    /**
     * Gli ordini, ricaricati dal controller dopo ogni modifica.
     *
     * ⚠️ [neverEqualPolicy] non è un'esagerazione, è **necessario**: `Ordine` è una classe mutabile e
     * non ridefinisce `equals`, quindi vale l'identità. Il controller modifica gli ordini **sul posto**
     * (evadendoli, per esempio, ne alza il flag `calcolato`) e restituisce una lista nuova con dentro
     * gli **stessi** oggetti: con la politica predefinita, che confronta con `==`, la vecchia e la
     * nuova lista risultano uguali e Compose **non ridisegna niente**. Si vedeva col bottone "Calcola
     * tutti gli ordini", che restava acceso dopo un calcolo pur non essendoci più nulla da calcolare.
     */
    var ordini by mutableStateOf<List<Ordine>>(emptyList(), neverEqualPolicy())
        private set

    /** Il magazzino degli avanzi. Stessa politica degli ordini, per lo stesso motivo. */
    var magazzino by mutableStateOf<List<Avanzo>>(emptyList(), neverEqualPolicy())
        private set

    /** L'ultimo calcolo globale, se ne è stato fatto uno in questa sessione. */
    var ultimaEvasione by mutableStateOf<EvasioneOrdini?>(null)
        private set

    /**
     * I nomi degli ordini dell'**ultimo calcolo**: quelli che un ripristino rimetterebbe da fare.
     * Vuota se non c'è niente da annullare.
     *
     * È uno `State` riletto in [aggiorna] e non una domanda al controller ogni volta perché quella
     * risposta arriva **da disco**: chiederla durante la composizione vorrebbe dire leggere un file
     * a ogni ridisegno.
     */
    var ripristinabili by mutableStateOf<List<String>>(emptyList())
        private set

    /**
     * L'ordine su cui appoggiare l'annullamento, o `null` se non c'è niente da annullare.
     *
     * Il comando è **del calcolo**, non dell'ordine — il gruppo torna indietro tutto insieme — ma il
     * controller vuole comunque un ordine di quel gruppo per riconoscerlo. Il bottone si mostra
     * esattamente quando questo non è `null`: la condizione della UI e la precondizione dell'azione
     * devono essere **la stessa cosa**, altrimenti si offre un comando che poi fallisce.
     */
    val ordineDaRipristinare: Ordine? get() = ordini.firstOrNull { it.nome() in ripristinabili }

    /** Messaggio da mostrare in una snackbar (errore o conferma); si consuma con [messaggioLetto]. */
    var messaggio by mutableStateOf<String?>(null)
        private set

    /**
     * L'unità con cui la UI mostra e legge le misure. È uno `State` e non una lettura diretta dal
     * controller perché il controller non notifica nessuno: senza, cambiare unità non ridisegnerebbe
     * le schermate già in video. Il modello resta comunque **sempre in millimetri**.
     */
    var unita by mutableStateOf(Unita.PREDEFINITA)
        private set

    /** L'euristica del **prossimo** calcolo. Vale lo stesso discorso dell'unità. */
    var strategia by mutableStateOf(Strategia.PREDEFINITA)
        private set

    init {
        val cartella = app.filesDir.toPath().resolve("dati")
        controller = Controller(
            catalogo,
            ArchivioMagazzino(cartella.resolve("magazzino.csv"), catalogo),
            ArchivioOrdini(cartella.resolve("ordini.csv"), catalogo),
            ArchivioImpostazioni(cartella.resolve("impostazioni.properties")),
            ArchivioCalcoli(cartella),
            ArchivioRipristino(cartella, catalogo)
        )
        aggiorna()
        val scartate = controller.righeOrdiniScartate()
        if (scartate > 0) {
            messaggio = "$scartate righe del file ordini non sono state lette: correggile prima di " +
                    "modificare qualcosa, o il prossimo salvataggio le cancellera'."
        }
    }

    val sistemi get() = catalogo.sistemi()

    val ordiniDaCalcolare: List<Ordine> get() = ordini.filter { !it.calcolato() }

    /** I profili fra cui scegliere quando si dichiara un avanzo (varianti comprese). */
    fun profili(): List<Profilo> = sistemi.flatMap { it.profili() }.distinct()

    // --- Ordini -------------------------------------------------------------------------

    fun nomeOrdineProposto(): String = controller.nomeOrdineProposto()

    /**
     * `true` se nessun **altro** ordine si chiama già così: il nome è la chiave con cui gli ordini si
     * rileggono da disco, e due omonimi si fonderebbero in uno solo al prossimo avvio.
     *
     * @param tranne l'ordine a cui il nome appartiene già (nella rinomina), oppure `null`
     */
    fun nomeLibero(nome: String, tranne: Ordine? = null): Boolean =
        controller.nomeLibero(nome, tranne)

    fun nuovoOrdine(nome: String) = protetto {
        controller.nuovoOrdine(nome)
    }

    /**
     * Cambia il nome di un ordine. Non ne tocca lo stato di calcolo, ma i documenti già archiviati
     * restano sotto il vecchio nome e vengono **dimenticati**: è il controller a farlo, per non
     * lasciarli lì a farsi trovare da un futuro omonimo.
     */
    fun rinominaOrdine(ordine: Ordine, nome: String) = protetto {
        controller.rinominaOrdine(ordine, nome)
    }

    fun rimuoviOrdine(ordine: Ordine) = protetto {
        controller.rimuoviOrdine(ordine)
    }

    /** I documenti dell'ultimo calcolo di quest'ordine, riletti da disco (vuoti se non c'è). */
    fun calcoloDi(ordine: Ordine): CalcoloOrdine = controller.calcoloDi(ordine)

    fun aggiungiSerramento(
        ordine: Ordine,
        tipologia: Tipologia,
        colore: String,
        l: Double,
        h: Double,
        hf: Double,
        quantita: Int,
        prezzoKg: Double,
        prezzoMq: Double
    ) = protetto {
        val serramento = Serramento(
            tipologia,
            Colore(colore),
            Dimensione(l, h, hf),
            quantita,
            Prezzi(prezzoKg, prezzoMq)
        )
        controller.aggiungiSerramento(ordine, serramento)
    }

    fun rimuoviSerramento(ordine: Ordine, indice: Int) = protetto {
        controller.rimuoviSerramento(ordine, indice)
    }

    /**
     * Il calcolo **globale**: tutti gli ordini da calcolare insieme, così i pezzi condividono le
     * barre. Consuma il magazzino e segna gli ordini come calcolati, esattamente come sul desktop.
     */
    fun calcolaTutto() = protetto {
        if (ordiniDaCalcolare.isEmpty()) {
            messaggio = "Nessun ordine da calcolare."
            return@protetto
        }
        val evasione = controller.evadiOrdini()
        ultimaEvasione = evasione
        messaggio = "Calcolati ${evasione.ordini().size} ordini: " +
                "${evasione.preventivoTotale().totaleBarreNuove()} barre nuove."
    }

    /**
     * Annulla l'ultimo calcolo: rimette il magazzino esattamente com'era e riporta **tutti** gli
     * ordini di quel calcolo fra quelli da fare.
     *
     * Il gruppo si annulla tutto insieme perché il calcolo lo aveva unito in un piano solo, con le
     * barre condivise: la "parte di magazzino" di un ordine singolo non esiste.
     */
    fun ripristinaUltimoCalcolo() = protetto {
        val ordine = ordineDaRipristinare
            ?: throw IllegalStateException("Non c'e' nessun calcolo da annullare.")
        val tornati = controller.ripristina(ordine)
        ultimaEvasione = null
        messaggio = "Ripristinati: ${tornati.joinToString(", ")}"
    }

    /** La soglia (mm) sopra cui un ritaglio non è scarto ma torna a magazzino come nuovo avanzo. */
    fun sogliaRitaglio(): Double = controller.sogliaRitaglio()

    // --- Magazzino ----------------------------------------------------------------------

    fun aggiungiAvanzo(profilo: Profilo, colore: String, lunghezza: Double, quantita: Int) = protetto {
        controller.aggiungiAvanzo(Avanzo(profilo, Colore(colore), lunghezza, quantita))
    }

    fun rimuoviAvanzo(indice: Int, quantita: Int) = protetto {
        controller.rimuoviAvanzo(indice, quantita)
    }

    fun svuotaMagazzino() = protetto {
        val tolti = controller.svuotaMagazzino()
        messaggio = "Tolti $tolti pezzi dal magazzino."
    }

    // --- Impostazioni -------------------------------------------------------------------

    /**
     * Cambia l'unità di misura e la persiste. Riguarda solo la presentazione: i dati su disco non si
     * toccano, quindi passare a metri e tornare a millimetri non può perdere niente.
     */
    fun impostaUnita(scelta: Unita) = protetto {
        controller.impostaUnita(scelta)
    }

    /**
     * Cambia l'euristica di taglio e la persiste. Vale dal **prossimo** calcolo: i piani già fatti
     * restano quelli, e riscriverne i numeri farebbe credere che siano stati rifatti.
     */
    fun impostaStrategia(scelta: Strategia) = protetto {
        controller.impostaStrategia(scelta)
    }

    /** Da quel che l'utente scrive, nell'unità scelta, ai millimetri del modello. */
    fun versoMm(valore: Double): Double = unita.versoMm(valore)

    // --- Infrastruttura -----------------------------------------------------------------

    fun messaggioLetto() {
        messaggio = null
    }

    /**
     * Esegue un'operazione e riallinea lo stato. Gli errori del dominio (misure impossibili, nome
     * gia' usato, pezzo piu' lungo della barra) arrivano come eccezioni con un messaggio scritto per
     * l'utente: si mostrano invece di far chiudere l'app.
     */
    private inline fun protetto(azione: () -> Unit) {
        try {
            azione()
        } catch (errore: IllegalArgumentException) {
            messaggio = errore.message ?: "Operazione non valida."
        } catch (errore: IllegalStateException) {
            messaggio = errore.message ?: "Operazione non possibile."
        }
        aggiorna()
    }

    private fun aggiorna() {
        ordini = controller.ordini()
        magazzino = controller.magazzino()
        unita = controller.unita()
        strategia = controller.strategia()
        ripristinabili = controller.ordiniRipristinabili()
    }
}
