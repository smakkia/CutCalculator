package com.cutcalculator.ottimizzatore;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.formule.Distinta;

import java.util.List;

/**
 * Strategia "svuota-magazzino": come best-fit, ma privilegia il <b>completamento degli
 * avanzi</b>. Finché un avanzo può ospitare il pezzo lo si usa (best-fit tra gli avanzi),
 * e si apre/riempie una barra nuova solo quando nessun avanzo entra.
 * <p>
 * È una scelta di business più che matematica: può lasciare un filo di sfrido in più sulle
 * barre nuove, ma libera prima il magazzino dagli spezzoni.
 */
public class SvuotaMagazzino implements Ottimizzatore {

    @Override
    public PianoDiTaglio ottimizza(Distinta distinta, List<Avanzo> avanzi, double lunghezzaBarraStandard) {
        return Impacchettatore.impacchetta(distinta, avanzi, lunghezzaBarraStandard,
                Impacchettatore.DECRESCENTE, Impacchettatore.AVANZI_PRIMA);
    }
}
