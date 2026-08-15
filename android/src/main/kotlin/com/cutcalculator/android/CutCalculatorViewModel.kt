package com.cutcalculator.android

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.cutcalculator.persistenza.ArchivioCalcoli
import com.cutcalculator.persistenza.ArchivioImpostazioni
import com.cutcalculator.persistenza.ArchivioMagazzino
import com.cutcalculator.persistenza.ArchivioOrdini
import com.cutcalculator.persistenza.ArchivioRipristino
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

    /** Gli ordini, ricaricati dal controller dopo ogni modifica. */
    var ordini by mutableStateOf<List<Ordine>>(emptyList())
        private set

    /** Il magazzino degli avanzi. */
    var magazzino by mutableStateOf<List<Avanzo>>(emptyList())
        private set

    /** L'ultimo calcolo globale, se ne è stato fatto uno in questa sessione. */
    var ultimaEvasione by mutableStateOf<EvasioneOrdini?>(null)
        private set

    /** Messaggio da mostrare in una snackbar (errore o conferma); si consuma con [messaggioLetto]. */
    var messaggio by mutableStateOf<String?>(null)
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

    val unita: Unita get() = controller.unita()

    val sistemi get() = catalogo.sistemi()

    val ordiniDaCalcolare: List<Ordine> get() = ordini.filter { !it.calcolato() }

    /** I profili fra cui scegliere quando si dichiara un avanzo (varianti comprese). */
    fun profili(): List<Profilo> = sistemi.flatMap { it.profili() }.distinct()

    // --- Ordini -------------------------------------------------------------------------

    fun nomeOrdineProposto(): String = controller.nomeOrdineProposto()

    fun nomeLibero(nome: String): Boolean = controller.nomeLibero(nome, null)

    fun nuovoOrdine(nome: String) = protetto {
        controller.nuovoOrdine(nome)
    }

    fun rimuoviOrdine(ordine: Ordine) = protetto {
        controller.rimuoviOrdine(ordine)
    }

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

    fun ripristinabile(ordine: Ordine): Boolean = controller.ripristinabile(ordine)

    /** Annulla l'ultimo calcolo: rimette il magazzino com'era e gli ordini fra quelli da calcolare. */
    fun ripristina(ordine: Ordine) = protetto {
        val tornati = controller.ripristina(ordine)
        ultimaEvasione = null
        messaggio = "Ripristinati: ${tornati.joinToString(", ")}"
    }

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
    }
}
