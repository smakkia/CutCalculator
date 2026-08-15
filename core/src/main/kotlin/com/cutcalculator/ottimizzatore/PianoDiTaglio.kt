package com.cutcalculator.ottimizzatore

import com.cutcalculator.dominio.Materiale
import java.util.Collections
import kotlin.math.exp
import kotlin.math.ln

/**
 * L'output della Fase 2: il piano di taglio completo di un ordine — l'elenco delle barre (nuove da
 * comprare + avanzi riusati) con dentro i pezzi già assegnati.
 *
 * È immutabile (copia difensiva): una volta prodotto dall'ottimizzatore non si tocca.
 * L'ottimizzatore può generare più piani candidati e scegliere il migliore: la metrica è la
 * [mediaGeometricaSfrido] per barra — più è bassa, meno materiale si spreca, migliore è il piano.
 */
class PianoDiTaglio(barre: List<BarraTagliata>) {

    private val barre: List<BarraTagliata> = Collections.unmodifiableList(ArrayList(barre))

    fun barre(): List<BarraTagliata> = barre

    /** Barre totali usate: nuove comprate più avanzi riutilizzati. */
    fun numeroBarre(): Int = barre.size

    /** Solo le barre nuove da comprare: il fabbisogno reale di materiale. */
    fun barreNuove(): Int = barre.count { !it.avanzo() }

    /**
     * Media geometrica degli sfridi per barra — la metrica di qualità del piano: a parità di
     * condizioni, il piano con media più bassa spreca meno ed è quello scelto.
     *
     * Calcolata via logaritmi per stabilità numerica, con un piccolo [OFFSET_SFRIDO] additivo che
     * evita il caso limite dello sfrido 0 senza alterarne il valore. Piano vuoto → 0.
     */
    fun mediaGeometricaSfrido(): Double {
        if (barre.isEmpty()) {
            return 0.0
        }
        val sommaLog = barre.sumOf { ln(it.sfrido() + OFFSET_SFRIDO) }
        return exp(sommaLog / barre.size)
    }

    /** Barre raggruppate per [Materiale] (profilo + colore), nell'ordine in cui compaiono. */
    fun perMateriale(): Map<Materiale, List<BarraTagliata>> =
        barre.groupByTo(LinkedHashMap()) { it.materiale() }

    override fun equals(other: Any?): Boolean =
        this === other || (other is PianoDiTaglio && barre == other.barre)

    override fun hashCode(): Int = barre.hashCode()

    override fun toString(): String = "PianoDiTaglio[barre=$barre]"

    private companion object {
        /**
         * Offset additivo (mm) sommato a ogni sfrido prima della media geometrica: tiene la media
         * pressoché intatta ma evita il caso limite dello sfrido 0 (che la azzererebbe).
         */
        const val OFFSET_SFRIDO = 0.1
    }
}
