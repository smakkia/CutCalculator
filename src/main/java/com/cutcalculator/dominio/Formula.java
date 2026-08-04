package com.cutcalculator.dominio;

/**
 * Formula lineare che calcola la lunghezza di un pezzo dalle misure del serramento.
 * <p>
 * È una combinazione lineare delle basi (Larghezza, Altezza, altezza parziale) più una costante:
 * <pre>lunghezza = cL·L + cH·H + cHF·HF + cost</pre>
 * Il segno è già nel numero: sottrarre una base = coefficiente negativo; una detrazione = costante negativa.
 * <ul>
 *   <li>finestra {@code L − 90}      → {@code new Formula(1, 0, 0, -90)}</li>
 *   <li>porta    {@code H + HF − 40} → {@code new Formula(0, 1, 1, -40)}</li>
 *   <li>costante {@code 500}         → {@code new Formula(0, 0, 0, 500)}</li>
 * </ul>
 */
public record Formula(double cL, double cH, double cHF, double cost) {

    /** Lunghezza del pezzo (mm) per le misure date. */
    public double calcola(Dimensione d) {
        return cL * d.L() + cH * d.H() + cHF * d.HF() + cost;
    }
}
