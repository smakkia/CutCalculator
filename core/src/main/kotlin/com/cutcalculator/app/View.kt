package com.cutcalculator.app

import com.cutcalculator.catalogo.Catalogo
import com.cutcalculator.dominio.Avanzo
import com.cutcalculator.dominio.Ordine
import com.cutcalculator.ottimizzatore.PianoDiTaglio
import com.cutcalculator.pianificazione.DistintaOrdine
import com.cutcalculator.pianificazione.EvasioneOrdini
import com.cutcalculator.preventivo.Preventivo

/**
 * L'astrazione del **front-end**: sa avviarsi sopra un [Controller] e sa **mostrare** all'utente gli
 * oggetti del dominio. Ogni realizzazione fa l'override di questi metodi a modo suo: `CliView` (testo
 * su console) e `GuiFx` (JavaFX, con i widget) — senza toccare il controller né il core.
 *
 * Qui ci sono solo le firme. Restano **fuori** dall'interfaccia i dettagli specifici di ciascun
 * front-end (per la CLI: loop dei menu, lettura da tastiera, formattazione ASCII): non avrebbero
 * senso per una view grafica, quindi vivono privati dentro `CliView`.
 */
interface View {

    /** Avvia il front-end sullo stato dato e lo guida fino all'uscita dell'utente. */
    fun avvia(controller: Controller)

    // --- Viste sui dati: ogni realizzazione le mostra a modo suo -----------------------

    fun catalogo(catalogo: Catalogo)

    fun ordine(ordine: Ordine)

    fun magazzino(avanzi: List<Avanzo>)

    /** I pezzi da tagliare, **raggruppati per ordine** come il piano lo è per barra. */
    fun distinta(distinte: List<DistintaOrdine>)

    fun piano(piano: PianoDiTaglio)

    fun sfridi(piano: PianoDiTaglio)

    fun preventivo(preventivo: Preventivo)

    fun evasione(evasione: EvasioneOrdini)
}
