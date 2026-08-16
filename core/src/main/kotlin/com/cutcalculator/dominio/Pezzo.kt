package com.cutcalculator.dominio

/**
 * Un singolo pezzo fisico già tagliato: il risultato di applicare una [RegolaTaglio] a una
 * [Dimensione]. Sta sul lato "output" della pipeline ed è l'unità che l'ottimizzatore piazza nelle
 * barre.
 *
 * È "esploso": una riga con quantità N produce N oggetti [Pezzo] identici, così l'ottimizzatore li
 * può piazzare uno a uno.
 *
 * @param profilo     da quale profilo è ricavato (con il colore forma la chiave di raggruppamento)
 * @param colore      la finitura richiesta: eredita il colore dell'[Ordine]
 * @param lunghezza   lunghezza in mm, già calcolata dalla formula
 * @param tipoTaglio  come sono tagliate le due estremità
 * @param descrizione etichetta/ruolo, ereditata dalla riga di taglio
 * @param prezzi      il listino del [Serramento] da cui viene: il €/kg può cambiare da un serramento
 *                    all'altro (colori diversi, fornitori diversi), quindi ogni pezzo si porta
 *                    dietro il proprio invece di attingere a un listino unico
 */
@JvmRecord
data class Pezzo(
    val profilo: Profilo,
    val colore: Colore,
    val lunghezza: Double,
    val tipoTaglio: TipoTaglio,
    val descrizione: String,
    val prezzi: Prezzi
) {
    /** Senza listino: il pezzo si può tagliare ma non valorizzare. */
    constructor(
        profilo: Profilo, colore: Colore, lunghezza: Double,
        tipoTaglio: TipoTaglio, descrizione: String
    ) : this(profilo, colore, lunghezza, tipoTaglio, descrizione, Prezzi.NESSUNO)

    /** Il €/kg da applicare a questo pezzo: quello del serramento da cui viene. */
    fun prezzoAlChilo(): Double = prezzi.alChiloDi(profilo)

    /** Il [Materiale] (profilo + colore): la chiave con cui l'ottimizzatore raggruppa i pezzi. */
    fun materiale(): Materiale = Materiale(profilo, colore)

    /**
     * Millimetri di barra che questo pezzo consuma **oltre** alla sua lunghezza e al kerf base:
     * il sovrapprezzo dei tagli in diagonale, `extraKerf45 del profilo × estremità a 45°`.
     *
     * Vale 0 per i profili base e per qualunque pezzo tagliato 90/90, quindi finché il catalogo non
     * dichiara profili maggiorati il conto della barra non cambia di un millimetro.
     */
    fun extraKerf(): Double = profilo.extraKerf45 * tipoTaglio.tagliA45()

    /** Peso del pezzo (kg): lunghezza × peso lineare del profilo. */
    fun peso(): Double = profilo.peso(lunghezza)

    /** Costo del solo pezzo (€): peso × €/kg. Il costo **vero** è quello della barra intera. */
    fun prezzo(): Double = peso() * prezzoAlChilo()
}
