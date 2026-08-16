package com.cutcalculator.dominio

import java.util.Collections
import java.util.EnumSet

/**
 * La "ricetta" di un tipo di serramento: un nome (es. "Finestra a 2 ante"), l'elenco completo delle
 * righe di taglio (la scheda del Gruppo E) e le righe del **vetro**.
 *
 * È pura descrizione: **non** genera i pezzi. La logica "applica le formule a una misura L×H →
 * lista di pezzi" vive nel pacchetto `formule` (GeneratoreDistinta).
 *
 * Le [RegolaVetro] sono separate dalle [RegolaTaglio] perché il vetro non è un'asta: non si taglia
 * da una barra e non passa dall'ottimizzatore 1D, si dimensiona e si somma per area. Molte tipologie
 * non ne hanno ancora (le schede non riportano la quota del vetro): in quel caso la lista è vuota e
 * si usa il costruttore a due argomenti.
 *
 * Gli accessori verranno aggiunti più avanti.
 *
 * Non è una `data class` per via della **copia difensiva** delle due liste, che il costruttore
 * primario di una data class non potrebbe fare (non può riscrivere i propri parametri).
 */
class Tipologia(nome: String, regole: List<RegolaTaglio>, regoleVetro: List<RegolaVetro>) {

    private val nome: String = nome
    private val regole: List<RegolaTaglio> = immutabile(regole)
    private val regoleVetro: List<RegolaVetro> = immutabile(regoleVetro)

    /** Tipologia di cui non si conoscono (ancora) le quote del vetro: solo righe di taglio. */
    constructor(nome: String, regole: List<RegolaTaglio>) : this(nome, regole, emptyList())

    fun nome(): String = nome

    fun regole(): List<RegolaTaglio> = regole

    fun regoleVetro(): List<RegolaVetro> = regoleVetro

    /**
     * `true` se qualche regola usa l'altezza parziale HF (coefficiente `cHF` non nullo), come nelle
     * porte: la UI deve allora chiedere anche HF, non solo L e H.
     */
    fun usaHF(): Boolean =
        regole.any { it.formula.cHF != 0.0 } ||
                regoleVetro.any { it.formulaLunghezza.cHF != 0.0 || it.formulaLarghezza.cHF != 0.0 }

    /** `true` se della tipologia si conoscono le quote del vetro. */
    fun haVetro(): Boolean = regoleVetro.isNotEmpty()

    /**
     * I ruoli ([Categoria]) che compaiono davvero nelle sue regole: gli unici per cui ha senso
     * scegliere una [Variante].
     *
     * Le varianti sono dichiarate per **sistema**, non per tipologia, perché telaio e anta si
     * scelgono separatamente e il prodotto cartesiano darebbe un catalogo ingestibile. Ma non tutte
     * le tipologie di un sistema usano tutti i ruoli: un elemento fisso non ha ante, e lasciar
     * scegliere "anta maggiorata" su un fisso accorcerebbe fermavetro e vetro per un profilo che non
     * c'è — quote sbagliate senza nessun errore, perché il restringimento guarda il *livello* della
     * categoria e non se quel profilo sia stato davvero tagliato.
     */
    fun ruoli(): Set<Categoria> =
        regole.mapTo(EnumSet.noneOf(Categoria::class.java)) { it.profilo.categoria }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Tipologia &&
                nome == other.nome && regole == other.regole && regoleVetro == other.regoleVetro)

    override fun hashCode(): Int {
        var risultato = nome.hashCode()
        risultato = 31 * risultato + regole.hashCode()
        risultato = 31 * risultato + regoleVetro.hashCode()
        return risultato
    }

    override fun toString(): String =
        "Tipologia[nome=$nome, regole=$regole, regoleVetro=$regoleVetro]"

    private companion object {
        /**
         * Copia difensiva: le regole diventano immutabili una volta costruita la tipologia.
         * `toList()` non basterebbe — restituisce una lista che a runtime resta modificabile con un
         * cast, mentre il contratto (e i test) vogliono `UnsupportedOperationException`.
         */
        fun <T> immutabile(elementi: List<T>): List<T> =
            Collections.unmodifiableList(ArrayList(elementi))
    }
}
