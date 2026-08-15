package com.cutcalculator.dominio

/**
 * Le misure finite di un serramento, in millimetri.
 *
 * `L` = larghezza, `H` = altezza piena, `HF` = altezza parziale
 * (usata solo da alcuni profili di porte; per le finestre vale 0).
 *
 * `@JvmRecord` la tiene un vero `java.lang.Record`: gli accessor restano `L()`, `H()`, `HF()`
 * e i chiamanti Java non cambiano di una riga.
 */
@JvmRecord
data class Dimensione(val L: Double, val H: Double, val HF: Double) {

    /** Comodo per finestre e serramenti senza altezza parziale: HF = 0. */
    constructor(L: Double, H: Double) : this(L, H, 0.0)
}
