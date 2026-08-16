package com.cutcalculator.dominio

/**
 * Una lastra di vetro con le sue misure finite, in millimetri.
 *
 * È il pendant 2D del [Pezzo]: mentre il pezzo è un'asta (una sola misura utile, la lunghezza), il
 * vetro è una superficie e si misura in `lunghezza × larghezza`. Per ora non entra
 * nell'ottimizzatore (niente nesting 2D): si dichiara e se ne calcola l'**area**, che è quanto serve
 * a stimare il materiale.
 *
 * A differenza del [Pezzo] il vetro **non** è esploso: lastre identiche restano una riga sola con la
 * `quantita`, perché nessuno deve piazzarle una a una in una barra.
 *
 * Unità: le misure sono in mm (come tutto il modello), quindi le aree vengono in mm². Il vetro però
 * si compra a metro quadro: [areaMq] e [areaTotaleMq] fanno la conversione.
 *
 * I due lati seguono le colonne delle "distinte di taglio vetri" del catalogo, che sono **H** e
 * **L**: la `lunghezza` è la quota in altezza (colonna H), la `larghezza` quella in larghezza
 * (colonna L). Non è detto che la lunghezza sia il lato maggiore — una finestra bassa e larga dà una
 * lastra più larga che alta.
 *
 * @param lunghezza il lato della lastra ricavato dall'altezza H (mm)
 * @param larghezza il lato della lastra ricavato dalla larghezza L (mm)
 * @param quantita  quante lastre identiche servono
 * @param prezzi    il listino del [Serramento] da cui vengono: il €/mq cambia col tipo di vetro
 *                  richiesto, quindi ogni riga si porta dietro il proprio
 */
@JvmRecord
data class Vetro(
    val lunghezza: Double,
    val larghezza: Double,
    val quantita: Int,
    val prezzi: Prezzi
) {
    /** Senza listino: la lastra si può ordinare ma non valorizzare. */
    constructor(lunghezza: Double, larghezza: Double, quantita: Int) :
            this(lunghezza, larghezza, quantita, Prezzi.NESSUNO)

    /** Comodo per la lastra singola. */
    constructor(lunghezza: Double, larghezza: Double) : this(lunghezza, larghezza, 1)

    /** Il costo di queste lastre: superficie × €/mq del serramento da cui vengono. */
    fun costo(): Double = prezzi.costoVetro(areaTotaleMq())

    /** Area di **una** lastra, in mm². */
    fun area(): Double = lunghezza * larghezza

    /** Area di tutte le lastre di questa riga (`area × quantita`), in mm². */
    fun areaTotale(): Double = area() * quantita

    /** Area di una lastra in metri quadri (l'unità con cui il vetro si acquista). */
    fun areaMq(): Double = area() / MMQ_PER_MQ

    /** Area totale della riga in metri quadri. */
    fun areaTotaleMq(): Double = areaTotale() / MMQ_PER_MQ

    /** Perimetro di una lastra (mm): serve ai fermavetri e alle guarnizioni. */
    fun perimetro(): Double = 2 * (lunghezza + larghezza)

    private companion object {
        /** Millimetri quadri in un metro quadro: 1 m² = 1000 mm × 1000 mm. */
        const val MMQ_PER_MQ = 1_000_000.0
    }
}
