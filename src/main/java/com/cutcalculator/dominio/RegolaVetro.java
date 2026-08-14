package com.cutcalculator.dominio;

/**
 * Una riga "vetro" della scheda di taglio: dice come ricavare le misure di una lastra dalle
 * misure finite del serramento.
 * <p>
 * È il gemello bidimensionale della {@link RegolaTaglio}: dove quella ha <b>una</b> {@link Formula}
 * (la lunghezza dell'asta), questa ne ha <b>due</b>, una per lato della lastra. Stesse basi
 * (L, H, HF) e stessa forma lineare, così le quote del vetro si trascrivono dalle schede
 * esattamente come quelle dei profili (es. vetro {@code L − 200} × {@code H − 200}).
 *
 * @param descrizione       il ruolo della lastra nella tipologia (es. "Vetro anta destra")
 * @param formulaLunghezza  come si ricava il lato maggiore
 * @param formulaLarghezza  come si ricava il lato minore
 * @param quantita          quante lastre identiche per serramento
 */
public record RegolaVetro(
        String descrizione,
        Formula formulaLunghezza,
        Formula formulaLarghezza,
        int quantita
) {
    /** La lastra (misure + quantità) che questa riga produce per le misure date. */
    public Vetro calcola(Dimensione d) {
        return new Vetro(formulaLunghezza.calcola(d), formulaLarghezza.calcola(d), quantita);
    }
}
