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

    /**
     * La stessa formula con una detrazione in più, cioè {@code questa − offset}.
     * <p>
     * Serve a <b>derivare una quota da un'altra</b> restando in una sola formula lineare: nelle schede
     * SX 110, per esempio, il vetro non è quotato sul serramento ma sull'<b>anta</b>
     * ({@code Hₐ − 140}), e l'anta a sua volta sul serramento ({@code Hₐ = H − 78}). Comporre le due
     * con questo metodo tiene la derivazione visibile nel catalogo, invece di scriverci il risultato
     * già fatto ({@code H − 218}) che nessuno saprebbe più ricondurre alla scheda.
     */
    public Formula meno(double offset) {
        return new Formula(cL, cH, cHF, cost - offset);
    }
}
