package com.cutcalculator.ottimizzatore

import com.cutcalculator.dominio.Avanzo
import com.cutcalculator.formule.Distinta

/**
 * Euristica Best-Fit-Decreasing: pezzi dal più lungo al più corto, ognuno nella barra aperta che
 * resta **più piena** (sfrido residuo minimo) fra quelle in cui entra; se non entra da nessuna
 * parte, una barra nuova.
 *
 * È la **base di partenza** di tutte le altre: incastra i pezzi più stretti di FFD e, dato che gli
 * avanzi hanno sfrido piccolo, tende naturalmente a riusarli per primi. [MultiStartCasuale] —
 * l'euristica predefinita — parte proprio dal suo piano e lo sostituisce solo se un ordine casuale
 * fa meglio, quindi questa resta il pavimento sotto cui non si scende.
 */
open class BestFitDecreasing : Ottimizzatore {

    override fun ottimizza(
        distinta: Distinta, avanzi: List<Avanzo>, lunghezzaBarraStandard: Double
    ): PianoDiTaglio = Impacchettatore.impacchetta(
        distinta, avanzi, lunghezzaBarraStandard,
        Impacchettatore.DECRESCENTE, Impacchettatore.MIGLIOR_INCASTRO
    )
}
