package com.cutcalculator.ottimizzatore

import com.cutcalculator.dominio.Avanzo
import com.cutcalculator.formule.Distinta

/**
 * Euristica First-Fit-Decreasing: pezzi dal più lungo al più corto, ognuno nella **prima** barra
 * aperta in cui entra (avanzi in testa), altrimenti una barra nuova.
 *
 * Rimane come alternativa/candidato; per la qualità del taglio è di solito preferibile
 * [BestFitDecreasing].
 */
open class FirstFitDecreasing : Ottimizzatore {

    override fun ottimizza(
        distinta: Distinta, avanzi: List<Avanzo>, lunghezzaBarraStandard: Double
    ): PianoDiTaglio = Impacchettatore.impacchetta(
        distinta, avanzi, lunghezzaBarraStandard,
        Impacchettatore.DECRESCENTE, Impacchettatore.PRIMO_CHE_ENTRA
    )
}
