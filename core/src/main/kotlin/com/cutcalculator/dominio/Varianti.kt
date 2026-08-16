package com.cutcalculator.dominio

import java.util.Collections
import java.util.EnumMap

/**
 * Le [Variante] varianti scelte per un [Serramento]: al più una per ruolo (telaio, anta, ...).
 * È l'unico posto dove vive la regola del **contenimento**, così il generatore della distinta non
 * deve saperne nulla.
 *
 * La regola: una variante restringe tutto ciò che le sta **dentro**, cioè le categorie di
 * [Categoria.livello] maggiore del suo. Il telaio maggiorato accorcia anta, montante, traverso,
 * fermavetro e vetro; l'anta maggiorata accorcia traverso, fermavetro e vetro ma **non** il montante
 * d'incontro, che sta fra le ante e non dentro; nessuna delle due accorcia sé stessa. I
 * restringimenti di ruoli diversi **si sommano** (telaio maggiorato + anta maggiorata = 48 per lato
 * sul vetro).
 *
 * Non è una `data class` perché la mappa va **copiata e validata** prima di diventare proprietà, e
 * il costruttore primario di una data class non può riscrivere i propri parametri.
 */
class Varianti(scelte: Map<Categoria, Variante>) {

    private val scelte: Map<Categoria, Variante> = normalizza(scelte)

    fun scelte(): Map<Categoria, Variante> = scelte

    /** Questa scelta più un'altra (o la sostituzione di quella dello stesso ruolo). */
    fun con(variante: Variante): Varianti {
        // Non si usa EnumMap(Map): rifiuta le mappe vuote, e partire da NESSUNA è il caso normale.
        val nuove = EnumMap<Categoria, Variante>(Categoria::class.java)
        nuove.putAll(scelte)
        nuove[variante.ruolo()] = variante
        return Varianti(nuove)
    }

    fun vuota(): Boolean = scelte.isEmpty()

    /**
     * Il profilo da tagliare davvero al posto di `base`: quello della variante scelta per il suo
     * ruolo, se c'è. Un profilo di un ruolo senza varianti torna identico.
     */
    fun profiloDi(base: Profilo): Profilo = scelte[base.categoria]?.profilo ?: base

    /**
     * Quanti millimetri, **per estremità**, perde un pezzo di questa categoria: la somma dei
     * restringimenti delle varianti che lo contengono.
     */
    fun restringimentoDi(categoria: Categoria): Double = restringimentoAlLivello(categoria.livello())

    /** Come sopra per il vetro, che non è un profilo ma sta dentro tutto. */
    fun restringimentoDelVetro(): Double = restringimentoAlLivello(Categoria.LIVELLO_VETRO)

    private fun restringimentoAlLivello(livello: Int): Double =
        scelte.values.filter { it.ruolo().livello() < livello }.sumOf { it.restringimento }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Varianti && scelte == other.scelte)

    override fun hashCode(): Int = scelte.hashCode()

    override fun toString(): String = "Varianti[scelte=$scelte]"

    companion object {
        /** Nessuna scelta: tutti i profili base, quote invariate. */
        @JvmField
        val NESSUNA: Varianti = Varianti(emptyMap())

        /** Una sola scelta, chiavata dal ruolo del suo profilo. */
        @JvmStatic
        fun di(variante: Variante): Varianti = Varianti(mapOf(variante.ruolo() to variante))

        /** Copia difensiva, e ogni variante deve stare sotto la chiave del proprio ruolo. */
        private fun normalizza(scelte: Map<Categoria, Variante>): Map<Categoria, Variante> {
            val copia = EnumMap<Categoria, Variante>(Categoria::class.java)
            scelte.forEach { (ruolo, variante) ->
                require(variante.ruolo() == ruolo) {
                    "La variante ${variante.nome} è del ruolo ${variante.ruolo()}, non $ruolo"
                }
                copia[ruolo] = variante
            }
            return Collections.unmodifiableMap(copia)
        }
    }
}
