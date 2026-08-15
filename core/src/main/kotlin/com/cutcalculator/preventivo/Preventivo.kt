package com.cutcalculator.preventivo

import java.util.Collections

/**
 * L'output della Fase 3: la stima del materiale necessario per un ordine — una [RigaProfilo] per
 * profilo (barre da comprare, avanzi riusati, sfrido) e una [RigaVetro] per misura di lastra. Gli
 * accessori entreranno qui quando saranno nel modello.
 *
 * Le due parti si contano in unità diverse e restano quindi separate: le barre in **metri lineari**
 * (e passano dall'ottimizzatore), il vetro in **metri quadri** (e no). Anche il **costo** segue
 * quella divisione: l'alluminio si paga a peso (€/kg), il vetro a superficie (€/mq).
 *
 * Il listino usato viaggia **dentro** il preventivo: i costi non sono memorizzati ma ricalcolati
 * dalle quantità, e restano legati ai prezzi con cui il preventivo è stato fatto — che è esattamente
 * quello che ci si aspetta da un preventivo. Senza listino i costi sono zero.
 */
class Preventivo(righe: List<RigaProfilo>, righeVetro: List<RigaVetro>) {

    private val righe: List<RigaProfilo> = Collections.unmodifiableList(ArrayList(righe))
    private val righeVetro: List<RigaVetro> = Collections.unmodifiableList(ArrayList(righeVetro))

    /** Preventivo di soli profili: quando la tipologia non ha (ancora) le quote del vetro. */
    constructor(righe: List<RigaProfilo>) : this(righe, emptyList())

    fun righe(): List<RigaProfilo> = righe

    fun righeVetro(): List<RigaVetro> = righeVetro

    /** Barre nuove da comprare in totale, su tutti i profili. */
    fun totaleBarreNuove(): Int = righe.sumOf { it.barreNuove }

    /** Spezzoni di magazzino riutilizzati in totale. */
    fun totaleAvanziUsati(): Int = righe.sumOf { it.avanziUsati }

    /** Metri lineari di barra nuova ordinati in totale. */
    fun lunghezzaNuovaTotale(): Double = righe.sumOf { it.lunghezzaNuova }

    /** Sfrido complessivo del piano, su tutte le barre (nuove e avanzi). */
    fun sfridoTotale(): Double = righe.sumOf { it.sfrido }

    /** Quanti ritagli sopra soglia rientreranno in magazzino come nuovi avanzi. */
    fun totaleRitagliRecuperabili(): Int = righe.sumOf { it.ritagliRecuperabili }

    /** Lunghezza totale dei ritagli recuperabili: la parte di sfrido che non va persa. */
    fun lunghezzaRecuperabileTotale(): Double = righe.sumOf { it.lunghezzaRecuperabile }

    /** Lo sfrido che resta scarto vero: troppo corto per tornare a magazzino. */
    fun scartoTotale(): Double = sfridoTotale() - lunghezzaRecuperabileTotale()

    /** Quante lastre di vetro in tutto, di qualunque misura. */
    fun totaleLastre(): Int = righeVetro.sumOf { it.quantita }

    /** Superficie vetrata complessiva da ordinare, in metri quadri. */
    fun areaVetroTotaleMq(): Double = righeVetro.sumOf { it.areaTotaleMq() }

    // --- Costi: quantità × listino dell'utente ----------------------------------------

    /** Peso (kg) delle barre nuove da comprare, su tutti i profili. */
    fun pesoTotale(): Double = righe.sumOf { it.peso() }

    /** Quanto costano le barre nuove da comprare. */
    fun costoBarre(): Double = righe.sumOf { it.costo }

    /** Quanto costa il vetro da ordinare. */
    fun costoVetro(): Double = righeVetro.sumOf { it.costo }

    /** `true` se almeno una voce ha un costo: sotto questa condizione i totali hanno senso. */
    fun valorizzato(): Boolean = costoTotale() > 0

    /** Il costo del materiale dell'ordine: barre nuove + vetro. */
    fun costoTotale(): Double = costoBarre() + costoVetro()

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is Preventivo && righe == other.righe && righeVetro == other.righeVetro)

    override fun hashCode(): Int = 31 * righe.hashCode() + righeVetro.hashCode()

    override fun toString(): String = "Preventivo[righe=$righe, righeVetro=$righeVetro]"
}
