package com.cutcalculator.pianificazione

import com.cutcalculator.dominio.Avanzo
import com.cutcalculator.dominio.Ordine
import com.cutcalculator.ottimizzatore.PianoDiTaglio
import com.cutcalculator.preventivo.Preventivo
import java.util.Collections

/**
 * Il risultato del **calcolo globale**: tutti gli ordini vengono **uniti come se fossero uno solo** e
 * ottimizzati insieme (vedi [PianificatoreOrdini]), così i pezzi di ordini diversi possono
 * condividere la stessa barra e lo sfrido cala.
 *
 * Contiene gli [Ordine] inclusi (solo per riferimento/stampa), le [DistintaOrdine] — cosa tagliare,
 * **tenute divise per commessa** anche se il piano le unisce, perché chi taglia vuole sapere di chi è
 * ogni pezzo —, il **piano di taglio unico** (come sistemarli sulle barre), il [Preventivo]
 * aggregato, il **magazzino aggiornato** dopo aver consumato gli avanzi usati e rimesso i ritagli
 * sopra soglia, e la **ripartizione per ordine** ([QuotaOrdine]) di quel preventivo — che è una
 * quota, non una somma di barre intere, proprio perché le barre sono condivise.
 *
 * È un valore immutabile: chi lo riceve (il controller) decide se applicarlo, cioè sostituire il
 * magazzino con [magazzinoAggiornato] e persisterlo.
 */
class EvasioneOrdini(
    ordini: List<Ordine>,
    distinte: List<DistintaOrdine>,
    private val piano: PianoDiTaglio,
    private val preventivoTotale: Preventivo,
    magazzinoAggiornato: List<Avanzo>,
    quote: List<QuotaOrdine>
) {
    private val ordini: List<Ordine> = Collections.unmodifiableList(ArrayList(ordini))
    private val distinte: List<DistintaOrdine> = Collections.unmodifiableList(ArrayList(distinte))
    private val magazzinoAggiornato: List<Avanzo> =
        Collections.unmodifiableList(ArrayList(magazzinoAggiornato))
    private val quote: List<QuotaOrdine> = Collections.unmodifiableList(ArrayList(quote))

    /**
     * Evasione essenziale, senza distinte né ripartizione: per chi costruisce il record a mano
     * (test).
     */
    constructor(
        ordini: List<Ordine>, piano: PianoDiTaglio, preventivoTotale: Preventivo,
        magazzinoAggiornato: List<Avanzo>
    ) : this(ordini, emptyList(), piano, preventivoTotale, magazzinoAggiornato, emptyList())

    fun ordini(): List<Ordine> = ordini

    fun distinte(): List<DistintaOrdine> = distinte

    fun piano(): PianoDiTaglio = piano

    fun preventivoTotale(): Preventivo = preventivoTotale

    fun magazzinoAggiornato(): List<Avanzo> = magazzinoAggiornato

    fun quote(): List<QuotaOrdine> = quote

    override fun equals(other: Any?): Boolean = this === other || (other is EvasioneOrdini &&
            ordini == other.ordini &&
            distinte == other.distinte &&
            piano == other.piano &&
            preventivoTotale == other.preventivoTotale &&
            magazzinoAggiornato == other.magazzinoAggiornato &&
            quote == other.quote)

    override fun hashCode(): Int {
        var risultato = ordini.hashCode()
        risultato = 31 * risultato + distinte.hashCode()
        risultato = 31 * risultato + piano.hashCode()
        risultato = 31 * risultato + preventivoTotale.hashCode()
        risultato = 31 * risultato + magazzinoAggiornato.hashCode()
        return 31 * risultato + quote.hashCode()
    }

    override fun toString(): String =
        "EvasioneOrdini[ordini=$ordini, distinte=$distinte, piano=$piano," +
                " preventivoTotale=$preventivoTotale, magazzinoAggiornato=$magazzinoAggiornato," +
                " quote=$quote]"
}
