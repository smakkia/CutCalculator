package com.cutcalculator.dominio;

/**
 * Combinazione dei due tagli alle estremità di un pezzo, tenuta in un solo valore.
 * I tagli sono quasi sempre a 45° o 90°, quindi le combinazioni utili sono poche.
 * Il caso misto (un'estremità a 90°, l'altra a 45°) esiste in due versioni speculari,
 * {@link #TAGLIO_90_45_DX} e {@link #TAGLIO_90_45_SX}, perché il pezzo destro e quello
 * sinistro non sono intercambiabili. Il tipo si intuisce dalla forma del pezzo nella scheda.
 */
public enum TipoTaglio {
    TAGLIO_45_45(2),
    TAGLIO_90_90(0),
    TAGLIO_90_45_DX(1),
    TAGLIO_90_45_SX(1);

    private final int tagliA45;

    TipoTaglio(int tagliA45) {
        this.tagliA45 = tagliA45;
    }

    /**
     * Quante delle due estremità sono tagliate a 45° (0, 1 o 2).
     * <p>
     * Serve al consumo di barra: un taglio in diagonale ne mangia più di uno dritto, e quanto di più
     * dipende dalla sezione del profilo ({@link Profilo#extraKerf45()}). Un fermavetro 90/90 non paga
     * nulla, un montante di telaio 45/45 paga due volte.
     */
    public int tagliA45() {
        return tagliA45;
    }
}
