package com.cutcalculator.ottimizzatore;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.formule.Distinta;

import java.util.List;

/**
 * Euristica Best-Fit-Decreasing: pezzi dal più lungo al più corto, ognuno nella barra
 * aperta che resta <b>più piena</b> (sfrido residuo minimo) fra quelle in cui entra;
 * se non entra da nessuna parte, una barra nuova.
 * <p>
 * È la <b>base di partenza</b> di tutte le altre: incastra i pezzi più stretti di FFD e, dato che gli
 * avanzi hanno sfrido piccolo, tende naturalmente a riusarli per primi. {@link MultiStartCasuale} —
 * l'euristica predefinita — parte proprio dal suo piano e lo sostituisce solo se un ordine casuale
 * fa meglio, quindi questa resta il pavimento sotto cui non si scende.
 */
public class BestFitDecreasing implements Ottimizzatore {

    @Override
    public PianoDiTaglio ottimizza(Distinta distinta, List<Avanzo> avanzi, double lunghezzaBarraStandard) {
        return Impacchettatore.impacchetta(distinta, avanzi, lunghezzaBarraStandard,
                Impacchettatore.DECRESCENTE, Impacchettatore.MIGLIOR_INCASTRO);
    }
}
