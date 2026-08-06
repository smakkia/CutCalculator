package com.cutcalculator.dominio;

/**
 * Un serramento richiesto dall'utente: una {@link Tipologia} (la ricetta) applicata a
 * misure concrete ({@link Dimensione}), in un certo {@link Colore} e quantità.
 * <p>
 * È l'input della pipeline: "voglio {@code quantita} serramenti di questa tipologia,
 * grandi L×H, di questo colore". Il {@code GeneratoreDistinta} lo trasformerà nella lista dei
 * pezzi, tutti del colore del serramento (un serramento = un colore; un ordine può però
 * contenere serramenti di colori diversi).
 *
 * @param tipologia  quale scheda di taglio applicare
 * @param colore     la finitura: tutti i pezzi del serramento sono di questo colore
 * @param dimensione misure finite (L×H, più HF per le porte)
 * @param quantita   quanti serramenti identici
 */
public record Serramento(Tipologia tipologia, Colore colore, Dimensione dimensione, int quantita) {

    /** Comodo per le finestre: misure L×H senza altezza parziale (HF = 0). */
    public Serramento(Tipologia tipologia, Colore colore, double L, double H, int quantita) {
        this(tipologia, colore, new Dimensione(L, H), quantita);
    }
}
