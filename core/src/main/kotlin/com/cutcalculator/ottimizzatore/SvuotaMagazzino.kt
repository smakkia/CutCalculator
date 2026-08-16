package com.cutcalculator.ottimizzatore

import com.cutcalculator.dominio.Avanzo
import com.cutcalculator.formule.Distinta

/**
 * Strategia "svuota-magazzino": come best-fit, ma privilegia il **completamento degli avanzi**.
 * Finché un avanzo può ospitare il pezzo lo si usa (best-fit tra gli avanzi), e si apre/riempie una
 * barra nuova solo quando nessun avanzo entra.
 *
 * È una scelta di business più che matematica: può lasciare un filo di sfrido in più sulle barre
 * nuove, ma libera prima il magazzino dagli spezzoni.
 */
open class SvuotaMagazzino : Ottimizzatore {

    override fun ottimizza(
        distinta: Distinta, avanzi: List<Avanzo>, lunghezzaBarraStandard: Double
    ): PianoDiTaglio = Impacchettatore.impacchetta(
        distinta, avanzi, lunghezzaBarraStandard,
        Impacchettatore.DECRESCENTE, Impacchettatore.AVANZI_PRIMA
    )
}
