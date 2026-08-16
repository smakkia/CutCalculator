package com.cutcalculator.dominio

/**
 * Una riga "vetro" della scheda di taglio: dice come ricavare le misure di una lastra dalle misure
 * finite del serramento.
 *
 * È il gemello bidimensionale della [RegolaTaglio]: dove quella ha **una** [Formula] (la lunghezza
 * dell'asta), questa ne ha **due**, una per lato della lastra. Stesse basi (L, H, HF) e stessa forma
 * lineare, così le quote del vetro si trascrivono dalle schede esattamente come quelle dei profili
 * (es. vetro `L − 200` × `H − 200`).
 *
 * @param descrizione        il ruolo della lastra nella tipologia (es. "Vetro anta destra")
 * @param formulaLunghezza   come si ricava il lato maggiore
 * @param formulaLarghezza   come si ricava il lato minore
 * @param quantita           quante lastre identiche per serramento
 * @param estremitaLunghezza quanti dei due bordi del lato maggiore battono sul **perimetro**
 *                           (2 di norma; 1 se da un lato c'è un traverso, che non ingrossa)
 * @param estremitaLarghezza lo stesso per il lato minore
 */
@JvmRecord
data class RegolaVetro(
    val descrizione: String,
    val formulaLunghezza: Formula,
    val formulaLarghezza: Formula,
    val quantita: Int,
    val estremitaLunghezza: Int,
    val estremitaLarghezza: Int
) {
    /** Il caso normale: la lastra è circondata dal perimetro su tutti e quattro i bordi. */
    constructor(
        descrizione: String, formulaLunghezza: Formula, formulaLarghezza: Formula, quantita: Int
    ) : this(descrizione, formulaLunghezza, formulaLarghezza, quantita, 2, 2)

    /** La lastra (misure + quantità) che questa riga produce per le misure date. */
    fun calcola(d: Dimensione): Vetro = calcola(d, Varianti.NESSUNA)

    /**
     * La lastra tenendo conto delle varianti: ogni profilo che la racchiude le ruba il proprio
     * restringimento su ciascun bordo che gli va contro.
     */
    fun calcola(d: Dimensione, varianti: Varianti): Vetro {
        val restringimento = varianti.restringimentoDelVetro()
        return Vetro(
            formulaLunghezza.calcola(d) - restringimento * estremitaLunghezza,
            formulaLarghezza.calcola(d) - restringimento * estremitaLarghezza,
            quantita
        )
    }
}
