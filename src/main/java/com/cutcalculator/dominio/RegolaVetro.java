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
 * @param descrizione        il ruolo della lastra nella tipologia (es. "Vetro anta destra")
 * @param formulaLunghezza   come si ricava il lato maggiore
 * @param formulaLarghezza   come si ricava il lato minore
 * @param quantita           quante lastre identiche per serramento
 * @param estremitaLunghezza quanti dei due bordi del lato maggiore battono sul <b>perimetro</b>
 *                           (2 di norma; 1 se da un lato c'è un traverso, che non ingrossa)
 * @param estremitaLarghezza lo stesso per il lato minore
 */
public record RegolaVetro(
        String descrizione,
        Formula formulaLunghezza,
        Formula formulaLarghezza,
        int quantita,
        int estremitaLunghezza,
        int estremitaLarghezza
) {
    /** Il caso normale: la lastra è circondata dal perimetro su tutti e quattro i bordi. */
    public RegolaVetro(String descrizione, Formula formulaLunghezza, Formula formulaLarghezza,
            int quantita) {
        this(descrizione, formulaLunghezza, formulaLarghezza, quantita, 2, 2);
    }

    /** La lastra (misure + quantità) che questa riga produce per le misure date. */
    public Vetro calcola(Dimensione d) {
        return calcola(d, Varianti.NESSUNA);
    }

    /**
     * La lastra tenendo conto delle varianti: ogni profilo che la racchiude le ruba il proprio
     * restringimento su ciascun bordo che gli va contro.
     */
    public Vetro calcola(Dimensione d, Varianti varianti) {
        double restringimento = varianti.restringimentoDelVetro();
        return new Vetro(
                formulaLunghezza.calcola(d) - restringimento * estremitaLunghezza,
                formulaLarghezza.calcola(d) - restringimento * estremitaLarghezza,
                quantita);
    }
}
