package com.cutcalculator.ottimizzatore;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.formule.Distinta;

import java.util.List;

/**
 * Euristica First-Fit-Decreasing: pezzi dal più lungo al più corto, ognuno nella
 * <b>prima</b> barra aperta in cui entra (avanzi in testa), altrimenti una barra nuova.
 * <p>
 * Rimane come alternativa/candidato; per la qualità del taglio è di solito preferibile
 * {@link BestFitDecreasing}.
 */
public class FirstFitDecreasing implements Ottimizzatore {

    @Override
    public PianoDiTaglio ottimizza(Distinta distinta, List<Avanzo> avanzi, double lunghezzaBarraStandard) {
        return Impacchettatore.impacchetta(distinta, avanzi, lunghezzaBarraStandard,
                Impacchettatore.DECRESCENTE, Impacchettatore.PRIMO_CHE_ENTRA);
    }
}
