package com.cutcalculator.preventivo

import com.cutcalculator.dominio.Vetro

/**
 * Una riga vetro del preventivo: quante lastre di una certa misura servono in tutto.
 *
 * È l'aggregato delle [Vetro] lastre della distinta: tutte quelle con le stesse misure — anche se
 * vengono da serramenti o tipologie diverse — finiscono in una riga sola, con le quantità sommate.
 * È l'ordine che si passa al vetraio: "5 lastre 1350 × 1050".
 *
 * Niente colore né materiale: il vetro non ha (ancora) una tipizzazione nel modello, e comunque non
 * eredita la finitura dell'alluminio.
 *
 * Lastre della stessa misura ma con **prezzi diversi** restano righe separate: il €/mq arriva dal
 * serramento, e due commesse possono comprare lo stesso vetro a prezzi diversi.
 *
 * @param lunghezza il lato ricavato dall'altezza H (mm)
 * @param larghezza il lato ricavato dalla larghezza L (mm)
 * @param quantita  quante lastre di questa misura in tutto
 * @param costo     quanto costano: superficie × €/mq. Si paga la superficie ordinata — lo sfrido del
 *                  vetraio non è affar nostro, il vetro non lo tagliamo noi
 */
@JvmRecord
data class RigaVetro(
    val lunghezza: Double,
    val larghezza: Double,
    val quantita: Int,
    val costo: Double
) {
    /** Riga senza costo: quando non interessa la valorizzazione. */
    constructor(lunghezza: Double, larghezza: Double, quantita: Int) :
            this(lunghezza, larghezza, quantita, 0.0)

    /** Area di una lastra, in metri quadri: il vetro si compra a m². */
    fun areaMq(): Double = Vetro(lunghezza, larghezza).areaMq()

    /** Area complessiva della riga, in metri quadri. */
    fun areaTotaleMq(): Double = Vetro(lunghezza, larghezza, quantita).areaTotaleMq()
}
