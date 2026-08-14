package com.cutcalculator.dominio;

/**
 * Una riga della scheda di taglio: l'<b>uso</b> di un {@link Profilo} dentro una tipologia.
 * Dice quale profilo tagliare, con quale {@link Formula} calcolarne la lunghezza, quanti
 * pezzi servono e con quali tagli alle estremità.
 * <p>
 * La {@code descrizione} è il <i>ruolo</i> nella tipologia (es. "Telaio orizzontale"),
 * diverso dalla descrizione dell'anagrafica del profilo (es. "Telaio ad L piccolo"): lo
 * stesso profilo può avere ruoli diversi in righe diverse.
 *
 * @param descrizione           il ruolo del pezzo nella tipologia
 * @param profilo               quale profilo tagliare (può essere sostituito da una {@link Variante})
 * @param formula               come si ricava la lunghezza dalle misure del serramento
 * @param quantita              quanti pezzi identici per serramento
 * @param tipoTaglio            come sono tagliate le due estremità
 * @param estremitaSulPerimetro quante delle due estremità vanno a battere sul <b>perimetro</b>
 *                              (telaio o anta) invece che su un traverso: è il moltiplicatore del
 *                              {@link Variante#restringimento()}. Quasi sempre 2; vale 1 per i pezzi
 *                              spezzati da un traverso, che da quel lato non perdono nulla perché il
 *                              traverso non ingrossa
 */
public record RegolaTaglio(
        String descrizione,
        Profilo profilo,
        Formula formula,
        int quantita,
        TipoTaglio tipoTaglio,
        int estremitaSulPerimetro
) {
    /** Il caso normale: entrambe le estremità vanno a battere sul perimetro. */
    public RegolaTaglio(String descrizione, Profilo profilo, Formula formula, int quantita,
            TipoTaglio tipoTaglio) {
        this(descrizione, profilo, formula, quantita, tipoTaglio, 2);
    }

    /** Lunghezza (mm) di un singolo pezzo di questa riga, per le misure date. */
    public double lunghezza(Dimensione d) {
        return formula.calcola(d);
    }

    /**
     * Lunghezza (mm) tenendo conto delle varianti scelte: la quota di scheda meno il restringimento
     * dei profili che racchiudono questo pezzo, pagato una volta per estremità sul perimetro.
     */
    public double lunghezza(Dimensione d, Varianti varianti) {
        return lunghezza(d) - varianti.restringimentoDi(profilo.categoria()) * estremitaSulPerimetro;
    }
}
