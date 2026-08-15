package com.cutcalculator.ottimizzatore

import com.cutcalculator.dominio.Colore
import com.cutcalculator.dominio.Materiale
import com.cutcalculator.dominio.Pezzo
import com.cutcalculator.dominio.Profilo
import java.util.Collections

/**
 * Una singola barra grezza presa a magazzino, con i pezzi già tagliati dentro.
 * È l'unità che l'ottimizzatore riempie, un pezzo alla volta, finché c'è spazio.
 *
 * Una barra appartiene a **un solo [Materiale]** (profilo + colore): da lei si possono ricavare solo
 * pezzi dello stesso profilo e colore (un telaio bianco non esce da una barra d'anta, né da una
 * barra bronzo). È **mutabile**, perché l'ottimizzatore ci aggiunge pezzi progressivamente.
 *
 * Può essere un **avanzo** già in magazzino (gratis, di proprietà) oppure una **barra nuova** da
 * comprare: lo dice il flag [avanzo]. Il preventivo conta come materiale da acquistare solo le barre
 * nuove.
 *
 * Modello del kerf (spessore della lama): semplice, ogni pezzo "costa" `lunghezza + kerf`, quindi
 * `occupato = Σ lunghezze + n·kerf`. Anche il primo pezzo paga un kerf, il che tiene conto di un po'
 * di spuntatura ai bordi.
 *
 * A questo si somma, per i profili che lo dichiarano, il **sovrapprezzo dei tagli in diagonale**
 * ([Pezzo.extraKerf]): un profilo maggiorato tagliato a 45° mangia più barra di uno piccolo. Non è
 * più vero, quindi, che tutti i pezzi costano uguale a parità di lunghezza — il conto passa da
 * `n · KERF` alla somma pezzo per pezzo.
 */
class BarraTagliata(
    private val profilo: Profilo,
    private val colore: Colore,
    private val lunghezzaBarra: Double,
    private val avanzo: Boolean
) {
    private val pezzi: MutableList<Pezzo> = ArrayList()

    /**
     * Lo spazio consumato, tenuto aggiornato a ogni [aggiungi]: è il numero più consultato
     * dell'ottimizzatore. Il selettore best-fit chiama [entra] e [sfrido] su ogni barra aperta per
     * ogni pezzo, e ricalcolare la somma da capo ogni volta rendeva l'impacchettamento
     * **quadratico** nel numero di pezzi. I pezzi non si tolgono mai da una barra, quindi tenere il
     * totale non può disallinearsi.
     */
    private var occupato: Double = 0.0

    /** Spazio consumato: somma delle lunghezze più il kerf (base + diagonali) di ogni pezzo. */
    fun occupato(): Double = occupato

    /**
     * Quanta barra costa un pezzo: la sua lunghezza, il kerf della lama e il sovrapprezzo dei tagli
     * a 45°, che dipende dalla sezione del profilo (vedi [Pezzo.extraKerf]).
     */
    private fun costoDi(pezzo: Pezzo): Double = pezzo.lunghezza + KERF + pezzo.extraKerf()

    /** Spazio ancora libero sulla barra (lo scarto se la si chiudesse così com'è). */
    fun sfrido(): Double = lunghezzaBarra - occupato()

    /**
     * Il pezzo ci starebbe? Verifica solo la lunghezza: si assume che il profilo sia quello giusto
     * (l'ottimizzatore raggruppa i pezzi per profilo prima di riempire).
     */
    fun entra(pezzo: Pezzo): Boolean = occupato() + costoDi(pezzo) <= lunghezzaBarra + EPS

    /**
     * Colloca il pezzo sulla barra.
     *
     * @throws IllegalArgumentException se il pezzo è di un materiale diverso (profilo o colore)
     * @throws IllegalStateException    se il pezzo non ci sta (chiama prima [entra])
     */
    fun aggiungi(pezzo: Pezzo) {
        if (pezzo.materiale() != materiale()) {
            throw IllegalArgumentException(
                "Pezzo " + pezzo.profilo.codice + " " + pezzo.colore.nome() +
                        " non collocabile su una barra " + profilo.codice + " " + colore.nome()
            )
        }
        if (!entra(pezzo)) {
            throw IllegalStateException(
                "Il pezzo (" + pezzo.lunghezza + " mm) non entra: sfrido residuo " + sfrido() + " mm"
            )
        }
        pezzi.add(pezzo)
        occupato += costoDi(pezzo)
    }

    fun profilo(): Profilo = profilo

    fun colore(): Colore = colore

    /** Il [Materiale] (profilo + colore) di questa barra: la chiave con cui la si raggruppa. */
    fun materiale(): Materiale = Materiale(profilo, colore)

    fun lunghezzaBarra(): Double = lunghezzaBarra

    /** `true` se è uno spezzone di magazzino riusato, `false` se è una barra nuova da comprare. */
    fun avanzo(): Boolean = avanzo

    fun kerf(): Double = KERF

    /** Peso (kg) della barra **intera**, sfrido compreso: è quello che si compra e si trasporta. */
    fun peso(): Double = profilo.peso(lunghezzaBarra)

    /** Peso (kg) dei soli pezzi utili ricavati: il netto, senza sfrido né kerf. */
    fun pesoPezzi(): Double = pezzi.sumOf { it.peso() }

    /**
     * Costo (€) della barra intera: peso × prezzo al chilo. Si paga tutta la barra, anche la parte
     * che finirà in sfrido — per questo il prezzo si calcola sulla lunghezza grezza.
     */
    fun prezzo(): Double = profilo.prezzo(lunghezzaBarra)

    /** Vista in sola lettura dei pezzi collocati, nell'ordine di taglio. */
    fun pezzi(): List<Pezzo> = Collections.unmodifiableList(ArrayList(pezzi))

    companion object {
        /** Spessore della lama a ogni taglio, in mm: ogni pezzo "costa" la sua lunghezza piu' questo. */
        const val KERF: Double = 4.0

        /** Tolleranza per i confronti tra double (le lunghezze sono in mm). */
        private const val EPS: Double = 1e-9
    }
}
