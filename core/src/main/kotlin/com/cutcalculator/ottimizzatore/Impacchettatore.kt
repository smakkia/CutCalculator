package com.cutcalculator.ottimizzatore

import com.cutcalculator.dominio.Avanzo
import com.cutcalculator.dominio.Materiale
import com.cutcalculator.dominio.Pezzo
import com.cutcalculator.formule.Distinta
import java.util.Collections
import java.util.Random

/** L'ordine in cui considerare i pezzi: riceve la lista e la restituisce riordinata. */
internal typealias OrdinePezzi = (MutableList<Pezzo>) -> List<Pezzo>

/** Sceglie in quale barra aperta mettere un pezzo; `null` = apri una barra nuova. */
internal typealias SelettoreBarra = (List<BarraTagliata>, Pezzo) -> BarraTagliata?

/**
 * Nucleo condiviso delle euristiche di taglio 1D: il ciclo "un profilo alla volta, ordina i pezzi,
 * metti ognuno nella barra scelta, apri barre nuove quando serve".
 * Le strategie concrete cambiano solo due cose, passate qui come parametri:
 * - l'**ordine** in cui considerare i pezzi ([DECRESCENTE], [casuale]);
 * - il **selettore** della barra per un pezzo ([PRIMO_CHE_ENTRA], [MIGLIOR_INCASTRO],
 *   [AVANZI_PRIMA]).
 *
 * Gli avanzi sono sempre in testa alle barre aperte (dal più corto), così vengono riusati prima di
 * comprare barre nuove.
 */
internal object Impacchettatore {

    /** Pezzi dal più lungo al più corto (la "Decreasing" delle euristiche *-Decreasing). */
    val DECRESCENTE: OrdinePezzi = { pezzi ->
        pezzi.sortByDescending { it.lunghezza }
        pezzi
    }

    /** First-Fit: la prima barra aperta in cui il pezzo entra. */
    val PRIMO_CHE_ENTRA: SelettoreBarra = { aperte, pezzo ->
        aperte.firstOrNull { it.entra(pezzo) }
    }

    /** Best-Fit: fra le barre in cui entra, quella che resta più piena (sfrido minore). */
    val MIGLIOR_INCASTRO: SelettoreBarra = { aperte, pezzo ->
        aperte.filter { it.entra(pezzo) }.minByOrNull { it.sfrido() }
    }

    /** Come best-fit ma completa prima gli avanzi; le barre nuove solo se nessun avanzo entra. */
    val AVANZI_PRIMA: SelettoreBarra = { aperte, pezzo ->
        aperte.filter { it.avanzo() && it.entra(pezzo) }.minByOrNull { it.sfrido() }
            ?: aperte.filter { !it.avanzo() && it.entra(pezzo) }.minByOrNull { it.sfrido() }
    }

    /**
     * Ordine casuale (per il multi-start): mescola i pezzi con l'`rng` dato.
     * Resta `java.util.Random` con `Collections.shuffle`, non il `Random` di Kotlin: a parità di
     * seed la sequenza dev'essere la stessa di prima, altrimenti i piani "riproducibili" cambiano.
     */
    fun casuale(rng: Random): OrdinePezzi = { pezzi ->
        Collections.shuffle(pezzi, rng)
        pezzi
    }

    fun impacchetta(
        distinta: Distinta,
        avanzi: List<Avanzo>,
        lunghezzaBarraStandard: Double,
        ordine: OrdinePezzi,
        selettore: SelettoreBarra
    ): PianoDiTaglio {
        val avanziPerMateriale = raggruppaAvanzi(avanzi)
        val risultato = ArrayList<BarraTagliata>()

        for ((materiale, pezziDelMateriale) in distinta.perMateriale()) {
            val pezzi = ordine(ArrayList(pezziDelMateriale))
            val aperte = daAvanzi(materiale, avanziPerMateriale[materiale])

            for (pezzo in pezzi) {
                var scelta = selettore(aperte, pezzo)
                if (scelta == null) {
                    scelta = apriNuova(materiale, lunghezzaBarraStandard, pezzo)
                    aperte.add(scelta)
                }
                scelta.aggiungi(pezzo)
            }
            risultato.addAll(aperte.filter { it.pezzi().isNotEmpty() })
        }
        return PianoDiTaglio(risultato)
    }

    fun raggruppaAvanzi(avanzi: List<Avanzo>): Map<Materiale, List<Avanzo>> {
        val perMateriale = LinkedHashMap<Materiale, MutableList<Avanzo>>()
        for (avanzo in avanzi) {
            perMateriale.getOrPut(avanzo.materiale()) { ArrayList() }.add(avanzo)
        }
        return perMateriale
    }

    /** Espande gli avanzi di un materiale (dal più corto) in barre vuote riusabili. */
    fun daAvanzi(materiale: Materiale, avanzi: List<Avanzo>?): MutableList<BarraTagliata> {
        val barre = ArrayList<BarraTagliata>()
        if (avanzi == null) {
            return barre
        }
        for (avanzo in avanzi.sortedBy { it.lunghezza }) {
            repeat(avanzo.quantita) {
                barre.add(BarraTagliata(materiale.profilo, materiale.colore, avanzo.lunghezza, true))
            }
        }
        return barre
    }

    /** Apre una barra nuova; se il pezzo non ci sta nemmeno lì, è fisicamente impossibile. */
    fun apriNuova(materiale: Materiale, lunghezzaBarraStandard: Double, pezzo: Pezzo): BarraTagliata {
        val nuova = BarraTagliata(materiale.profilo, materiale.colore, lunghezzaBarraStandard, false)
        require(nuova.entra(pezzo)) {
            "Pezzo da " + pezzo.lunghezza + " mm troppo lungo per la barra standard da " +
                    lunghezzaBarraStandard + " mm (profilo " + materiale.profilo.codice + " " +
                    materiale.colore.nome() + ")"
        }
        return nuova
    }
}
