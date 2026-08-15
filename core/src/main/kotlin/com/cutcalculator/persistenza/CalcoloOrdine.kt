package com.cutcalculator.persistenza

/**
 * I documenti di un ordine **già calcolato**, riletti da disco: la sua distinta, il suo preventivo, i
 * suoi vetri e quanto costa in tutto.
 *
 * Le righe sono volutamente "piatte" — codici e numeri, non oggetti del dominio. Sono la fotografia
 * di un calcolo fatto in passato: ricostruire `Profilo` e `Tipologia` dal catalogo di *oggi* darebbe
 * l'illusione che il documento sia ancora valido, mentre nel frattempo le schede possono essere
 * cambiate. Per rifare i conti si ricalcola l'ordine.
 */
@JvmRecord
data class CalcoloOrdine(
    val ordine: String,
    val distinta: List<VoceDistinta>,
    val preventivo: List<VocePreventivo>,
    val vetri: List<VoceVetro>
) {
    /** Un gruppo di pezzi uguali da tagliare. */
    @JvmRecord
    data class VoceDistinta(
        val profilo: String,
        val colore: String,
        val lunghezza: Double,
        val taglio: String,
        val quantita: Int
    )

    /** La quota di un materiale: quanto se ne taglia, quanto pesa e quanto costa. */
    @JvmRecord
    data class VocePreventivo(
        val profilo: String,
        val colore: String,
        val lunghezza: Double,
        val peso: Double,
        val costo: Double
    )

    /** Le lastre di una misura. */
    @JvmRecord
    data class VoceVetro(
        val altezza: Double,
        val larghezza: Double,
        val quantita: Int,
        val areaMq: Double,
        val costo: Double
    )

    /** `true` se di quest'ordine non c'è (ancora) nessun calcolo salvato. */
    fun vuoto(): Boolean = distinta.isEmpty() && preventivo.isEmpty() && vetri.isEmpty()

    fun costoProfili(): Double = preventivo.sumOf { it.costo }

    fun costoVetro(): Double = vetri.sumOf { it.costo }

    /** Il prezzo dell'ordine: la sua quota di alluminio più il suo vetro. */
    fun costoTotale(): Double = costoProfili() + costoVetro()

    fun pesoProfili(): Double = preventivo.sumOf { it.peso }

    fun totalePezzi(): Int = distinta.sumOf { it.quantita }

    fun totaleLastre(): Int = vetri.sumOf { it.quantita }

    fun areaVetroMq(): Double = vetri.sumOf { it.areaMq }
}
