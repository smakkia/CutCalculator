package com.cutcalculator.dominio

/**
 * Una riga della scheda di taglio: l'**uso** di un [Profilo] dentro una tipologia.
 * Dice quale profilo tagliare, con quale [Formula] calcolarne la lunghezza, quanti pezzi servono
 * e con quali tagli alle estremità.
 *
 * La `descrizione` è il *ruolo* nella tipologia (es. "Telaio orizzontale"), diverso dalla
 * descrizione dell'anagrafica del profilo (es. "Telaio ad L piccolo"): lo stesso profilo può avere
 * ruoli diversi in righe diverse.
 *
 * @param descrizione           il ruolo del pezzo nella tipologia
 * @param profilo               quale profilo tagliare (può essere sostituito da una [Variante])
 * @param formula               come si ricava la lunghezza dalle misure del serramento
 * @param quantita              quanti pezzi identici per serramento
 * @param tipoTaglio            come sono tagliate le due estremità
 * @param estremitaSulPerimetro quante delle due estremità vanno a battere sul **perimetro**
 *                              (telaio o anta) invece che su un traverso: è il moltiplicatore del
 *                              [Variante.restringimento]. Quasi sempre 2; vale 1 per i pezzi
 *                              spezzati da un traverso, che da quel lato non perdono nulla perché
 *                              il traverso non ingrossa
 */
@JvmRecord
data class RegolaTaglio(
    val descrizione: String,
    val profilo: Profilo,
    val formula: Formula,
    val quantita: Int,
    val tipoTaglio: TipoTaglio,
    val estremitaSulPerimetro: Int
) {
    /** Il caso normale: entrambe le estremità vanno a battere sul perimetro. */
    constructor(
        descrizione: String, profilo: Profilo, formula: Formula, quantita: Int,
        tipoTaglio: TipoTaglio
    ) : this(descrizione, profilo, formula, quantita, tipoTaglio, 2)

    /** Lunghezza (mm) di un singolo pezzo di questa riga, per le misure date. */
    fun lunghezza(d: Dimensione): Double = formula.calcola(d)

    /**
     * Lunghezza (mm) tenendo conto delle varianti scelte: la quota di scheda meno il restringimento
     * dei profili che racchiudono questo pezzo, pagato una volta per estremità sul perimetro.
     */
    fun lunghezza(d: Dimensione, varianti: Varianti): Double =
        lunghezza(d) - varianti.restringimentoDi(profilo.categoria) * estremitaSulPerimetro
}
