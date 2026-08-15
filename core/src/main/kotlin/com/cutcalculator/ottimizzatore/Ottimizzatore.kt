package com.cutcalculator.ottimizzatore

import com.cutcalculator.dominio.Avanzo
import com.cutcalculator.formule.Distinta

/**
 * Impacchetta i pezzi di una [Distinta] in barre a magazzino minimizzando lo sfrido (il problema del
 * taglio 1D, cutting-stock). Strategie diverse (First-Fit-Decreasing, best-fit, ...) implementano
 * questa interfaccia e sono intercambiabili: chi sta sopra sceglie l'algoritmo senza cambiare il
 * resto.
 */
interface Ottimizzatore {

    /**
     * Produce il piano di taglio: prima riusa gli `avanzi` di magazzino, poi apre barre nuove da
     * `lunghezzaBarraStandard` per i pezzi che restano.
     */
    fun ottimizza(distinta: Distinta, avanzi: List<Avanzo>, lunghezzaBarraStandard: Double): PianoDiTaglio

    /** Comodità: usa la barra standard di default ([BARRA_STANDARD_DEFAULT]). */
    fun ottimizza(distinta: Distinta, avanzi: List<Avanzo>): PianoDiTaglio =
        ottimizza(distinta, avanzi, BARRA_STANDARD_DEFAULT)

    companion object {
        /** Lunghezza di default della barra nuova, in mm (6,5 m). */
        const val BARRA_STANDARD_DEFAULT: Double = 6500.0
    }
}
