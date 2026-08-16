package com.cutcalculator.pianificazione

import com.cutcalculator.dominio.Colore
import com.cutcalculator.dominio.Ordine
import com.cutcalculator.dominio.Profilo
import java.util.Collections

/**
 * Quanto costa un singolo ordine dentro un calcolo globale: **una voce per ordine**, non il suo
 * dettaglio per materiale (quello resta nelle tabelle complessive, che sono l'unico posto dove barre
 * e sfrido hanno un senso).
 *
 * Il calcolo globale unisce gli ordini in un piano solo, e le barre finiscono condivise: la stessa
 * barra può contenere pezzi di commesse diverse. Non esiste quindi "il preventivo di quell'ordine"
 * come somma di barre intere — esiste la sua **quota**.
 *
 * **Come si divide.** Per ogni materiale (profilo + colore) il peso e il costo della riga globale si
 * spartiscono tra gli ordini in proporzione ai **millimetri di pezzi** che ciascuno ha chiesto di
 * quel materiale. Chi taglia di più paga di più, e con la stessa proporzione si spartiscono le barre
 * intere e lo sfrido: sono il prezzo dell'averle messe insieme, e non sarebbe corretto addossarlo a
 * chi per caso è finito nella barra più vuota. Le quote di tutti gli ordini **sommano esattamente al
 * totale**.
 *
 * Il **vetro** non ha questo problema: le lastre non si condividono, quindi lastre, superficie e
 * costo sono quelli veri dell'ordine, non una quota.
 *
 * @param ordine           il nome dell'ordine
 * @param lunghezzaProfili millimetri di profilo tagliati per quest'ordine (la base del riparto)
 * @param pesoProfili      quota del peso (kg) delle barre nuove
 * @param costoProfili     quota del costo (€) delle barre nuove
 * @param lastre           lastre di vetro dell'ordine (esatte, non una quota)
 * @param areaVetroMq      superficie vetrata dell'ordine in m² (esatta)
 * @param costoVetro       costo del vetro dell'ordine (esatto)
 */
class QuotaOrdine(
    private val ordine: String,
    private val lunghezzaProfili: Double,
    private val pesoProfili: Double,
    private val costoProfili: Double,
    private val lastre: Int,
    private val areaVetroMq: Double,
    private val costoVetro: Double,
    righe: List<Riga>
) {
    private val righe: List<Riga> = Collections.unmodifiableList(ArrayList(righe))

    fun ordine(): String = ordine

    fun lunghezzaProfili(): Double = lunghezzaProfili

    fun pesoProfili(): Double = pesoProfili

    fun costoProfili(): Double = costoProfili

    fun lastre(): Int = lastre

    fun areaVetroMq(): Double = areaVetroMq

    fun costoVetro(): Double = costoVetro

    fun righe(): List<Riga> = righe

    /** Quanto costa in tutto quest'ordine: la sua quota di alluminio più il suo vetro. */
    fun costoTotale(): Double = costoProfili + costoVetro

    /**
     * Il dettaglio per materiale dietro i totali: **non** si mostra nelle tabelle (lì basta una voce
     * per ordine) ma si **salva su disco**, perché il preventivo archiviato di una commessa senza le
     * sue righe non servirebbe a nulla.
     *
     * @param lunghezza millimetri di pezzi di questo materiale (la base del riparto)
     * @param peso      quota del peso (kg) delle barre nuove di questo materiale
     * @param costo     quota del costo (€) di quelle barre
     */
    @JvmRecord
    data class Riga(
        val profilo: Profilo,
        val colore: Colore,
        val lunghezza: Double,
        val peso: Double,
        val costo: Double
    )

    override fun equals(other: Any?): Boolean = this === other || (other is QuotaOrdine &&
            ordine == other.ordine &&
            lunghezzaProfili == other.lunghezzaProfili &&
            pesoProfili == other.pesoProfili &&
            costoProfili == other.costoProfili &&
            lastre == other.lastre &&
            areaVetroMq == other.areaVetroMq &&
            costoVetro == other.costoVetro &&
            righe == other.righe)

    override fun hashCode(): Int {
        var risultato = ordine.hashCode()
        risultato = 31 * risultato + lunghezzaProfili.hashCode()
        risultato = 31 * risultato + pesoProfili.hashCode()
        risultato = 31 * risultato + costoProfili.hashCode()
        risultato = 31 * risultato + lastre
        risultato = 31 * risultato + areaVetroMq.hashCode()
        risultato = 31 * risultato + costoVetro.hashCode()
        return 31 * risultato + righe.hashCode()
    }

    override fun toString(): String =
        "QuotaOrdine[ordine=$ordine, lunghezzaProfili=$lunghezzaProfili, pesoProfili=$pesoProfili," +
                " costoProfili=$costoProfili, lastre=$lastre, areaVetroMq=$areaVetroMq," +
                " costoVetro=$costoVetro, righe=$righe]"

    companion object {
        /** Quota vuota, per un [Ordine] senza serramenti. */
        @JvmStatic
        fun vuota(ordine: String): QuotaOrdine =
            QuotaOrdine(ordine, 0.0, 0.0, 0.0, 0, 0.0, 0.0, emptyList())
    }
}
