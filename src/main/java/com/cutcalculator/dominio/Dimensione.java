package com.cutcalculator.dominio;

/**
 * Le misure finite di un serramento, in millimetri.
 * <p>
 * {@code L} = larghezza, {@code H} = altezza piena, {@code HF} = altezza parziale
 * (usata solo da alcuni profili di porte; per le finestre vale 0).
 */
public record Dimensione(double L, double H, double HF) {

    /** Comodo per finestre e serramenti senza altezza parziale: HF = 0. */
    public Dimensione(double L, double H) {
        this(L, H, 0);
    }
}
