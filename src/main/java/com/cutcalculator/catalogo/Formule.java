package com.cutcalculator.catalogo;

import com.cutcalculator.dominio.Formula;

/**
 * Le formule di comodo con cui si trascrivono le schede di taglio: {@code L − 184}, {@code L/2 + 2},
 * {@code H − HF − 92}. Sono scorciatoie su {@link Formula}, che resta la primitiva
 * (una combinazione lineare di L, H e HF).
 * <p>
 * Stavano <b>ricopiate in tutti e quattro i cataloghi</b>, identiche riga per riga. Qui ce n'è una
 * copia sola, e i cataloghi le importano con {@code import static}.
 * <p>
 * I nomi seguono la scheda, non l'algebra: {@code perMezzaL(3)} è {@code L/2 − 3} e
 * {@code mezzaLpiu(2)} è {@code L/2 + 2}. La coppia "meno/più" esiste apposta perché la scheda
 * scrive l'offset col suo segno e chi trascrive non deve fare conti a mente — è la stessa ragione
 * per cui {@code SX 110} compone il vetro con {@link Formula#meno} invece di scrivere il risultato
 * già fatto.
 */
final class Formule {

    private Formule() {
    }

    /** {@code L − offset}. */
    static Formula perL(double offset) {
        return new Formula(1, 0, 0, -offset);
    }

    /** {@code L/2 − offset}. */
    static Formula perMezzaL(double offset) {
        return new Formula(0.5, 0, 0, -offset);
    }

    /** {@code L/2 + add}. */
    static Formula mezzaLpiu(double add) {
        return new Formula(0.5, 0, 0, add);
    }

    /** {@code L/3 − offset}. */
    static Formula terzoL(double offset) {
        return new Formula(1.0 / 3.0, 0, 0, -offset);
    }

    /** {@code L/3 + add}. */
    static Formula terzoLpiu(double add) {
        return new Formula(1.0 / 3.0, 0, 0, add);
    }

    /** {@code L/4 − offset}. */
    static Formula quartoL(double offset) {
        return new Formula(0.25, 0, 0, -offset);
    }

    /** {@code L/4 + add}. */
    static Formula quartoLpiu(double add) {
        return new Formula(0.25, 0, 0, add);
    }

    /** {@code H − offset}. */
    static Formula perH(double offset) {
        return new Formula(0, 1, 0, -offset);
    }

    /** {@code HF − offset}: il pezzo sotto il traverso di una porta. */
    static Formula perHF(double offset) {
        return new Formula(0, 0, 1, -offset);
    }

    /** {@code H − HF − offset}: il pezzo sopra il traverso di una porta. */
    static Formula hMenoHF(double offset) {
        return new Formula(0, 1, -1, -offset);
    }
}
