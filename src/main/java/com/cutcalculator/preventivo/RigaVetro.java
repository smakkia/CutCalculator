package com.cutcalculator.preventivo;

import com.cutcalculator.dominio.Prezzi;
import com.cutcalculator.dominio.Vetro;

/**
 * Una riga vetro del preventivo: quante lastre di una certa misura servono in tutto.
 * <p>
 * È l'aggregato delle {@link Vetro lastre} della distinta: tutte quelle con le stesse misure —
 * anche se vengono da serramenti o tipologie diverse — finiscono in una riga sola, con le quantità
 * sommate. È l'ordine che si passa al vetraio: "5 lastre 1350 × 1050".
 * <p>
 * Niente colore né materiale: il vetro non ha (ancora) una tipizzazione nel modello, e comunque non
 * eredita la finitura dell'alluminio.
 *
 * Lastre della stessa misura ma con <b>prezzi diversi</b> restano righe separate: il €/mq arriva dal
 * serramento, e due commesse possono comprare lo stesso vetro a prezzi diversi.
 *
 * @param lunghezza il lato ricavato dall'altezza H (mm)
 * @param larghezza il lato ricavato dalla larghezza L (mm)
 * @param quantita  quante lastre di questa misura in tutto
 * @param costo     quanto costano: superficie × €/mq. Si paga la superficie ordinata — lo sfrido del
 *                  vetraio non è affar nostro, il vetro non lo tagliamo noi
 */
public record RigaVetro(double lunghezza, double larghezza, int quantita, double costo) {

    /** Riga senza costo: quando non interessa la valorizzazione. */
    public RigaVetro(double lunghezza, double larghezza, int quantita) {
        this(lunghezza, larghezza, quantita, 0);
    }

    /** Area di una lastra, in metri quadri: il vetro si compra a m². */
    public double areaMq() {
        return new Vetro(lunghezza, larghezza).areaMq();
    }

    /** Area complessiva della riga, in metri quadri. */
    public double areaTotaleMq() {
        return new Vetro(lunghezza, larghezza, quantita).areaTotaleMq();
    }

}
