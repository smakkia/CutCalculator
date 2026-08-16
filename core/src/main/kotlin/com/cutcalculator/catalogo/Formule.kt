package com.cutcalculator.catalogo

import com.cutcalculator.dominio.Formula

/**
 * Le formule di comodo con cui si trascrivono le schede di taglio: `L − 184`, `L/2 + 2`,
 * `H − HF − 92`. Sono scorciatoie su [Formula], che resta la primitiva (una combinazione lineare di
 * L, H e HF).
 *
 * Stavano **ricopiate in tutti e quattro i cataloghi**, identiche riga per riga. Qui ce n'è una copia
 * sola, e i cataloghi le importano una per una (`import com.cutcalculator.catalogo.Formule.perL`).
 *
 * I nomi seguono la scheda, non l'algebra: `perMezzaL(3)` è `L/2 − 3` e `mezzaLpiu(2)` è `L/2 + 2`.
 * La coppia "meno/più" esiste apposta perché la scheda scrive l'offset col suo segno e chi trascrive
 * non deve fare conti a mente — è la stessa ragione per cui `SX 110` compone il vetro con
 * [Formula.meno] invece di scrivere il risultato già fatto.
 */
internal object Formule {

    /** `L − offset`. */
    fun perL(offset: Double): Formula = Formula(1.0, 0.0, 0.0, -offset)

    /** `L/2 − offset`. */
    fun perMezzaL(offset: Double): Formula = Formula(0.5, 0.0, 0.0, -offset)

    /** `L/2 + add`. */
    fun mezzaLpiu(add: Double): Formula = Formula(0.5, 0.0, 0.0, add)

    /** `L/3 − offset`. */
    fun terzoL(offset: Double): Formula = Formula(1.0 / 3.0, 0.0, 0.0, -offset)

    /** `L/3 + add`. */
    fun terzoLpiu(add: Double): Formula = Formula(1.0 / 3.0, 0.0, 0.0, add)

    /** `L/4 − offset`. */
    fun quartoL(offset: Double): Formula = Formula(0.25, 0.0, 0.0, -offset)

    /** `L/4 + add`. */
    fun quartoLpiu(add: Double): Formula = Formula(0.25, 0.0, 0.0, add)

    /** `H − offset`. */
    fun perH(offset: Double): Formula = Formula(0.0, 1.0, 0.0, -offset)

    /** `HF − offset`: il pezzo sotto il traverso di una porta. */
    fun perHF(offset: Double): Formula = Formula(0.0, 0.0, 1.0, -offset)

    /** `H − HF − offset`: il pezzo sopra il traverso di una porta. */
    fun hMenoHF(offset: Double): Formula = Formula(0.0, 1.0, -1.0, -offset)
}
