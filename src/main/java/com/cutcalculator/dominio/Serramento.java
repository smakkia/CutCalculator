package com.cutcalculator.dominio;

/**
 * Un serramento richiesto dall'utente: una {@link Tipologia} (la ricetta) applicata a
 * misure concrete ({@link Dimensione}), in una certa quantità.
 * <p>
 * È l'input della pipeline: "voglio {@code quantita} serramenti di questa tipologia,
 * grandi L×H". Il {@code GeneratoreDistinta} lo trasformerà nella lista dei pezzi.
 *
 * @param tipologia  quale scheda di taglio applicare
 * @param dimensione misure finite (L×H, più HF per le porte)
 * @param quantita   quanti serramenti identici
 */
public record Serramento(Tipologia tipologia, Dimensione dimensione, int quantita) {

    /** Comodo per le finestre: misure L×H senza altezza parziale (HF = 0). */
    public Serramento(Tipologia tipologia, double L, double H, int quantita) {
        this(tipologia, new Dimensione(L, H), quantita);
    }
}
