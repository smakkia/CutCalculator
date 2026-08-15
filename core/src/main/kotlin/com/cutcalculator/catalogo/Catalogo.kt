package com.cutcalculator.catalogo

import com.cutcalculator.dominio.Tipologia
import java.util.Collections
import java.util.EnumMap
import java.util.Optional

/**
 * Il registro di tutti i sistemi di profili disponibili, con il raggruppamento per
 * [FamigliaSistema]. Oggi i sistemi sono classi scritte a mano (`CatalogoRX700`, ...); in futuro
 * questi dati arriveranno da file/DB.
 */
open class Catalogo(sistemi: List<Sistema>) {

    private val sistemi: List<Sistema> = Collections.unmodifiableList(ArrayList(sistemi))

    /**
     * Indice tipologia → sistema, costruito una volta sola. Senza, [sistemaDi] doveva scorrere i
     * sistemi con un `contains`, cioè confrontare la [Tipologia] **regola per regola e formula per
     * formula** con tutte quelle del catalogo — e lo si chiama per ogni serramento a ogni salvataggio
     * automatico e per ogni cella della colonna Sistema.
     */
    private val sistemaPerTipologia: Map<Tipologia, Sistema> = run {
        val indice = HashMap<Tipologia, Sistema>()
        for (sistema in this.sistemi) {
            // putIfAbsent: se due sistemi avessero la stessa identica ricetta vince il primo,
            // esattamente come faceva il findFirst() di prima.
            sistema.tipologie().forEach { indice.putIfAbsent(it, sistema) }
        }
        Collections.unmodifiableMap(indice)
    }

    fun sistemi(): List<Sistema> = sistemi

    /** I sistemi raggruppati per famiglia, nell'ordine in cui compaiono. */
    fun perFamiglia(): Map<FamigliaSistema, List<Sistema>> =
        sistemi.groupByTo(EnumMap(FamigliaSistema::class.java)) { it.famiglia() }

    /** Il sistema con questo nome, se presente nel catalogo. */
    fun sistema(nome: String): Optional<Sistema> =
        Optional.ofNullable(sistemi.firstOrNull { it.nome() == nome })

    /**
     * Il sistema a cui appartiene una tipologia: serve a risalire dalla ricetta alla serie
     * commerciale (per mostrarla o per salvarla su file), visto che la [Tipologia] non conosce il
     * proprio sistema.
     */
    fun sistemaDi(tipologia: Tipologia): Optional<Sistema> =
        Optional.ofNullable(sistemaPerTipologia[tipologia])

    companion object {
        /** Il catalogo completo con tutti i sistemi finora trascritti. */
        @JvmStatic
        fun completo(): Catalogo = Catalogo(
            listOf(
                CatalogoRX700.sistema(),
                CatalogoCX700.sistema(),
                CatalogoSX110.sistema(),
                CatalogoSX120.sistema()
            )
        )
    }
}
