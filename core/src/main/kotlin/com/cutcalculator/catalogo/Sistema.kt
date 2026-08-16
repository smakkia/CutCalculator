package com.cutcalculator.catalogo

import com.cutcalculator.dominio.Categoria
import com.cutcalculator.dominio.Profilo
import com.cutcalculator.dominio.Tipologia
import com.cutcalculator.dominio.Variante
import java.util.Collections
import java.util.EnumMap
import java.util.Optional

/**
 * Un sistema di profili (es. "RX 700"): una serie commerciale con la sua [FamigliaSistema] e l'elenco
 * delle [Tipologia] (finestre/porte) che ci si possono costruire, ognuna con le sue formule di
 * taglio.
 *
 * Non è una `data class` per via delle copie difensive, che il costruttore primario di una data class
 * non può fare.
 */
class Sistema(
    private val nome: String,
    private val famiglia: FamigliaSistema,
    tipologie: List<Tipologia>,
    varianti: Map<Categoria, List<Variante>>
) {
    private val tipologie: List<Tipologia> = Collections.unmodifiableList(ArrayList(tipologie))

    // EnumMap: i ruoli escono nell'ordine dell'enum (dal telaio verso l'interno), che è anche
    // l'ordine in cui ha senso mostrarli e sceglierli. Si riempie a mano perché il costruttore
    // EnumMap(Map) rifiuta le mappe vuote (non saprebbe dedurre il tipo delle chiavi).
    private val varianti: Map<Categoria, List<Variante>> = run {
        val perRuolo = EnumMap<Categoria, List<Variante>>(Categoria::class.java)
        varianti.forEach { (ruolo, elenco) ->
            perRuolo[ruolo] = Collections.unmodifiableList(ArrayList(elenco))
        }
        Collections.unmodifiableMap(perRuolo)
    }

    /** Sistema senza varianti dichiarate: si taglia sempre il profilo base delle tipologie. */
    constructor(nome: String, famiglia: FamigliaSistema, tipologie: List<Tipologia>) :
            this(nome, famiglia, tipologie, emptyMap())

    fun nome(): String = nome

    fun famiglia(): FamigliaSistema = famiglia

    fun tipologie(): List<Tipologia> = tipologie

    fun varianti(): Map<Categoria, List<Variante>> = varianti

    /**
     * Le varianti fra cui si può scegliere per un ruolo, la prima delle quali è la **base** (quella
     * già cablata nelle tipologie). Vuota = quel ruolo non ha alternative, e la UI non deve mostrare
     * nulla.
     */
    fun variantiDi(ruolo: Categoria): List<Variante> = varianti[ruolo] ?: emptyList()

    /** I ruoli che hanno davvero più di una variante: quelli da chiedere all'utente. */
    fun ruoliConScelta(): List<Categoria> =
        varianti.entries.filter { it.value.size > 1 }.map { it.key }

    /**
     * Come [ruoliConScelta], ma limitato ai ruoli che **questa tipologia usa davvero**: è questo che
     * le UI devono chiedere. Un elemento fisso non ha ante, e una variante d'anta scelta lì
     * accorcerebbe fermavetro e vetro senza che nessun'anta esista (vedi [Tipologia.ruoli]).
     */
    fun ruoliConScelta(tipologia: Tipologia): List<Categoria> {
        val usati = tipologia.ruoli()
        return ruoliConScelta().filter { it in usati }
    }

    /** La tipologia con questo nome, se il sistema la prevede. */
    fun tipologia(nome: String): Optional<Tipologia> =
        Optional.ofNullable(tipologie.firstOrNull { it.nome() == nome })

    /**
     * I profili distinti del sistema, presi dalle regole di tutte le sue tipologie: sono quelli fra
     * cui l'utente sceglie quando dichiara un avanzo a magazzino.
     */
    fun profili(): List<Profilo> =
        (tipologie.flatMap { it.regole() }.map { it.profilo } +
                varianti.values.flatten().map { it.profilo }).distinct()

    override fun equals(other: Any?): Boolean = this === other || (other is Sistema &&
            nome == other.nome && famiglia == other.famiglia &&
            tipologie == other.tipologie && varianti == other.varianti)

    override fun hashCode(): Int {
        var risultato = nome.hashCode()
        risultato = 31 * risultato + famiglia.hashCode()
        risultato = 31 * risultato + tipologie.hashCode()
        return 31 * risultato + varianti.hashCode()
    }

    override fun toString(): String =
        "Sistema[nome=$nome, famiglia=$famiglia, tipologie=$tipologie, varianti=$varianti]"
}
