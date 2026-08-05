package com.cutcalculator.ottimizzatore;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.formule.Distinta;

import java.util.List;

/**
 * Impacchetta i pezzi di una {@link Distinta} in barre a magazzino minimizzando lo
 * sfrido (il problema del taglio 1D, cutting-stock). Strategie diverse
 * (First-Fit-Decreasing, best-fit, ...) implementano questa interfaccia e sono
 * intercambiabili: chi sta sopra sceglie l'algoritmo senza cambiare il resto.
 */
public interface Ottimizzatore {

    /** Lunghezza di default della barra nuova, in mm (6,5 m). */
    double BARRA_STANDARD_DEFAULT = 6500;

    /**
     * Produce il piano di taglio: prima riusa gli {@code avanzi} di magazzino, poi apre
     * barre nuove da {@code lunghezzaBarraStandard} per i pezzi che restano.
     */
    PianoDiTaglio ottimizza(Distinta distinta, List<Avanzo> avanzi, double lunghezzaBarraStandard);

    /** Comodità: usa la barra standard di default ({@link #BARRA_STANDARD_DEFAULT}). */
    default PianoDiTaglio ottimizza(Distinta distinta, List<Avanzo> avanzi) {
        return ottimizza(distinta, avanzi, BARRA_STANDARD_DEFAULT);
    }
}
