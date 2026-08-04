package com.cutcalculator.dominio;

/**
 * Una riga della scheda di taglio: l'<b>uso</b> di un {@link Profilo} dentro una tipologia.
 * Dice quale profilo tagliare, con quale {@link Formula} calcolarne la lunghezza, quanti
 * pezzi servono e con quali tagli alle estremità.
 * <p>
 * La {@code descrizione} è il <i>ruolo</i> nella tipologia (es. "Telaio orizzontale"),
 * diverso dalla descrizione dell'anagrafica del profilo (es. "Telaio ad L piccolo"): lo
 * stesso profilo può avere ruoli diversi in righe diverse.
 */
public record RegolaTaglio(
        String descrizione,
        Profilo profilo,
        Formula formula,
        int quantita,
        TipoTaglio tipoTaglio
) {
    /** Lunghezza (mm) di un singolo pezzo di questa riga, per le misure date. */
    public double lunghezza(Dimensione d) {
        return formula.calcola(d);
    }
}
