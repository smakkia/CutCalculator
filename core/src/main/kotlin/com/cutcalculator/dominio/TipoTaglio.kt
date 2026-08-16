package com.cutcalculator.dominio

/**
 * Combinazione dei due tagli alle estremità di un pezzo, tenuta in un solo valore.
 * I tagli sono quasi sempre a 45° o 90°, quindi le combinazioni utili sono poche.
 * Il caso misto (un'estremità a 90°, l'altra a 45°) esiste in due versioni speculari,
 * [TAGLIO_90_45_DX] e [TAGLIO_90_45_SX], perché il pezzo destro e quello sinistro non sono
 * intercambiabili. Il tipo si intuisce dalla forma del pezzo nella scheda.
 */
enum class TipoTaglio(private val tagliA45: Int) {
    TAGLIO_45_45(2),
    TAGLIO_90_90(0),
    TAGLIO_90_45_DX(1),
    TAGLIO_90_45_SX(1);

    /**
     * Quante delle due estremità sono tagliate a 45° (0, 1 o 2).
     *
     * Serve al consumo di barra: un taglio in diagonale ne mangia più di uno dritto, e quanto di più
     * dipende dalla sezione del profilo ([Profilo.extraKerf45]). Un fermavetro 90/90 non paga nulla,
     * un montante di telaio 45/45 paga due volte.
     */
    fun tagliA45(): Int = tagliA45
}
