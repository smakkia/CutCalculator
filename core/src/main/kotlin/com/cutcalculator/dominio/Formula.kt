package com.cutcalculator.dominio

/**
 * Formula lineare che calcola la lunghezza di un pezzo dalle misure del serramento.
 *
 * È una combinazione lineare delle basi (Larghezza, Altezza, altezza parziale) più una costante:
 * ```
 * lunghezza = cL·L + cH·H + cHF·HF + cost
 * ```
 * Il segno è già nel numero: sottrarre una base = coefficiente negativo; una detrazione = costante
 * negativa.
 * - finestra `L − 90`      → `Formula(1, 0, 0, -90)`
 * - porta    `H + HF − 40` → `Formula(0, 1, 1, -40)`
 * - costante `500`         → `Formula(0, 0, 0, 500)`
 */
@JvmRecord
data class Formula(val cL: Double, val cH: Double, val cHF: Double, val cost: Double) {

    /** Lunghezza del pezzo (mm) per le misure date. */
    fun calcola(d: Dimensione): Double = cL * d.L + cH * d.H + cHF * d.HF + cost

    /**
     * La stessa formula con una detrazione in più, cioè `questa − offset`.
     *
     * Serve a **derivare una quota da un'altra** restando in una sola formula lineare: nelle schede
     * SX 110, per esempio, il vetro non è quotato sul serramento ma sull'**anta** (`Hₐ − 140`), e
     * l'anta a sua volta sul serramento (`Hₐ = H − 78`). Comporre le due con questo metodo tiene la
     * derivazione visibile nel catalogo, invece di scriverci il risultato già fatto (`H − 218`) che
     * nessuno saprebbe più ricondurre alla scheda.
     */
    fun meno(offset: Double): Formula = Formula(cL, cH, cHF, cost - offset)
}
