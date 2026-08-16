package com.cutcalculator.formule

import com.cutcalculator.dominio.Materiale
import com.cutcalculator.dominio.Pezzo
import com.cutcalculator.dominio.Vetro
import java.util.Collections

/**
 * L'output della Fase 1: tutto ciò che serve produrre per un ordine — i **pezzi** da tagliare dalle
 * barre e le **lastre di vetro** da ordinare.
 *
 * Oltre alla lista piatta offre il raggruppamento per [Materiale] (profilo + colore), che serve
 * all'ottimizzatore: un pezzo può essere ricavato solo da una barra dello stesso profilo **e**
 * colore.
 *
 * I [Vetro] vetri viaggiano a parte perché non entrano nell'ottimizzatore 1D: si contano e si sommano
 * per area. Sono anche l'unica parte non "esplosa" — ogni riga porta la sua quantità.
 *
 * Non è una `data class` per via delle **copie difensive**, che il costruttore primario di una data
 * class non può fare.
 */
class Distinta(pezzi: List<Pezzo>, vetri: List<Vetro>) {

    private val pezzi: List<Pezzo> = Collections.unmodifiableList(ArrayList(pezzi))
    private val vetri: List<Vetro> = Collections.unmodifiableList(ArrayList(vetri))

    /** Distinta di soli profili: per le tipologie di cui non si conoscono le quote del vetro. */
    constructor(pezzi: List<Pezzo>) : this(pezzi, emptyList())

    fun pezzi(): List<Pezzo> = pezzi

    fun vetri(): List<Vetro> = vetri

    /** Numero totale di pezzi da tagliare. */
    fun totalePezzi(): Int = pezzi.size

    /** Numero totale di lastre da ordinare (somma delle quantità delle righe vetro). */
    fun totaleLastre(): Int = vetri.sumOf { it.quantita }

    /** Superficie vetrata complessiva, in metri quadri. */
    fun areaVetroTotaleMq(): Double = vetri.sumOf { it.areaTotaleMq() }

    /** Pezzi raggruppati per [Materiale] (profilo + colore), nell'ordine in cui compaiono. */
    fun perMateriale(): Map<Materiale, List<Pezzo>> = pezzi.groupByTo(LinkedHashMap()) { it.materiale() }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Distinta && pezzi == other.pezzi && vetri == other.vetri)

    override fun hashCode(): Int = 31 * pezzi.hashCode() + vetri.hashCode()

    override fun toString(): String = "Distinta[pezzi=$pezzi, vetri=$vetri]"
}
