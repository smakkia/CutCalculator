package com.cutcalculator.pianificazione

import com.cutcalculator.dominio.Avanzo
import com.cutcalculator.dominio.Colore
import com.cutcalculator.dominio.Materiale
import com.cutcalculator.dominio.Ordine
import com.cutcalculator.dominio.Pezzo
import com.cutcalculator.dominio.Profilo
import com.cutcalculator.dominio.Vetro
import com.cutcalculator.formule.Distinta
import com.cutcalculator.formule.GeneratoreDistinta
import com.cutcalculator.ottimizzatore.BarraTagliata
import com.cutcalculator.ottimizzatore.Ottimizzatore
import com.cutcalculator.ottimizzatore.PianoDiTaglio
import com.cutcalculator.ottimizzatore.Strategia
import com.cutcalculator.preventivo.GeneratorePreventivo
import com.cutcalculator.preventivo.Preventivo
import com.cutcalculator.preventivo.RigaProfilo
import kotlin.math.roundToLong

/**
 * Pianifica **più ordini insieme unendoli come se fossero uno solo**: i pezzi di tutti gli ordini
 * finiscono in un'unica [Distinta] e vengono ottimizzati in un colpo solo contro il magazzino
 * condiviso. Così pezzi di ordini diversi possono **condividere la stessa barra** e lo sfrido cala,
 * rispetto all'ottimizzare ogni ordine per conto suo.
 *
 * Algoritmo:
 * 1. si genera la distinta di ogni ordine e si mettono tutti i pezzi in una sola distinta;
 * 2. l'[Ottimizzatore] standard impacchetta il tutto: prima riusa gli avanzi di magazzino (per
 *    [Materiale]), poi apre barre nuove per i pezzi rimasti;
 * 3. gli avanzi usati sono **consumati**; i residui delle barre **≥ soglia** tornano in magazzino
 *    come nuovi avanzi (per le sessioni future, non per questo stesso calcolo).
 *
 * L'oggetto è puro: non tocca né stato né disco, restituisce tutto in [EvasioneOrdini].
 */
class PianificatoreOrdini(private val ottimizzatore: Ottimizzatore) {

    private val generatoreDistinta = GeneratoreDistinta()
    private val generatorePreventivo = GeneratorePreventivo()

    /** Con l'euristica [Strategia.PREDEFINITA]. */
    constructor() : this(Strategia.PREDEFINITA.crea())

    /** Come [pianifica] con soglia e barra standard di default. */
    fun pianifica(ordini: List<Ordine>, magazzino: List<Avanzo>): EvasioneOrdini =
        pianifica(ordini, magazzino, SOGLIA_RITAGLIO_DEFAULT, Ottimizzatore.BARRA_STANDARD_DEFAULT)

    /**
     * I prezzi non sono un parametro: ogni pezzo e ogni lastra si portano dietro il listino del
     * serramento da cui vengono, quindi il preventivo si valorizza da sé.
     *
     * @param ordini        gli ordini da evadere insieme (uniti in un unico piano)
     * @param magazzino     gli avanzi condivisi disponibili (non viene modificato)
     * @param soglia        lunghezza minima perché un ritaglio rientri come nuovo avanzo
     * @param barraStandard lunghezza della barra nuova
     * @throws IllegalArgumentException se un pezzo è più lungo della barra standard
     */
    fun pianifica(
        ordini: List<Ordine>, magazzino: List<Avanzo>,
        soglia: Double, barraStandard: Double
    ): EvasioneOrdini {

        // 1. Tutti i pezzi (e tutte le lastre) di tutti gli ordini in un'unica distinta.
        //    Le singole distinte restano da parte: servono a ripartire il preventivo per ordine.
        val tutti = ArrayList<Pezzo>()
        val tuttiVetri = ArrayList<Vetro>()
        val perOrdine = ArrayList<Distinta>()
        for (ordine in ordini) {
            val distinta = generatoreDistinta.genera(ordine)
            perOrdine.add(distinta)
            tutti.addAll(distinta.pezzi())
            tuttiVetri.addAll(distinta.vetri())
        }

        // 2. Un solo piano di taglio per l'insieme (riusa gli avanzi condivisi, poi barre nuove).
        //    Il vetro non passa di qui: l'ottimizzatore guarda solo i pezzi, le lastre proseguono
        //    verso il preventivo. La distinta unita resta comunque il documento d'officina.
        val unita = Distinta(tutti, tuttiVetri)
        val piano = ottimizzatore.ottimizza(unita, magazzino, barraStandard)

        // 3. Preventivo aggregato + magazzino aggiornato (avanzi consumati + ritagli sopra soglia).
        val preventivo = generatorePreventivo.genera(piano, tuttiVetri, soglia)
        val aggiornato = aggiornaMagazzino(magazzino, piano, soglia)
        val quote = ripartisci(ordini, perOrdine, preventivo)
        val distinte = ordini.indices.map { DistintaOrdine(ordini[it].nome(), perOrdine[it]) }
        return EvasioneOrdini(ordini, distinte, piano, preventivo, aggiornato, quote)
    }

    companion object {
        /**
         * Lunghezza minima (mm) perché un residuo di taglio valga come nuovo avanzo riusabile.
         * Pubblica perché la legge il `Controller`.
         */
        const val SOGLIA_RITAGLIO_DEFAULT: Double = Avanzo.LUNGHEZZA_MINIMA_RIUSO

        /**
         * Divide il preventivo globale tra gli ordini. Il criterio (e il perché) sta in
         * [QuotaOrdine]: per ogni materiale si spartiscono peso e costo in proporzione ai
         * millimetri di pezzi chiesti da ciascun ordine; il vetro invece è esatto, non si condivide.
         */
        private fun ripartisci(
            ordini: List<Ordine>, distinte: List<Distinta>, preventivo: Preventivo
        ): List<QuotaOrdine> {

            // Millimetri complessivi per materiale: è il denominatore del riparto.
            val lunghezzaTotale = LinkedHashMap<Materiale, Double>()
            for (distinta in distinte) {
                lunghezzePerMateriale(distinta).forEach { (materiale, mm) ->
                    lunghezzaTotale.merge(materiale, mm, Double::plus)
                }
            }
            // Peso e costo che il preventivo globale attribuisce a ogni materiale.
            val righe = LinkedHashMap<Materiale, RigaProfilo>()
            for (riga in preventivo.righe()) {
                righe[Materiale(riga.profilo, riga.colore)] = riga
            }

            val quote = ArrayList<QuotaOrdine>()
            for (i in ordini.indices) {
                val distinta = distinte[i]
                val nome = ordini[i].nome()
                if (distinta.pezzi().isEmpty() && distinta.vetri().isEmpty()) {
                    quote.add(QuotaOrdine.vuota(nome))
                    continue
                }
                var lunghezza = 0.0
                var peso = 0.0
                var costo = 0.0
                val dettaglio = ArrayList<QuotaOrdine.Riga>()
                for ((materiale, mm) in lunghezzePerMateriale(distinta)) {
                    val riga = righe[materiale]
                    val totale = lunghezzaTotale[materiale] ?: 0.0
                    val quota = if (riga == null || totale <= 0) 0.0 else mm / totale
                    val pesoRiga = if (riga == null) 0.0 else riga.peso() * quota
                    val costoRiga = if (riga == null) 0.0 else riga.costo * quota
                    lunghezza += mm
                    peso += pesoRiga
                    costo += costoRiga
                    dettaglio.add(
                        QuotaOrdine.Riga(materiale.profilo, materiale.colore, mm, pesoRiga, costoRiga)
                    )
                }
                // Il vetro non si divide: le lastre sono dell'ordine, col prezzo del loro serramento.
                val costoVetro = distinta.vetri().sumOf { it.costo() }
                quote.add(
                    QuotaOrdine(
                        nome, lunghezza, peso, costo,
                        distinta.totaleLastre(), distinta.areaVetroTotaleMq(), costoVetro, dettaglio
                    )
                )
            }
            return quote
        }

        /** Millimetri di pezzi per materiale in una distinta. */
        private fun lunghezzePerMateriale(distinta: Distinta): Map<Materiale, Double> {
            val lunghezze = LinkedHashMap<Materiale, Double>()
            for (pezzo in distinta.pezzi()) {
                lunghezze.merge(pezzo.materiale(), pezzo.lunghezza, Double::plus)
            }
            return lunghezze
        }

        /**
         * Magazzino post-evasione: avanzi non usati + ritagli ≥ soglia, uniti per
         * materiale+lunghezza.
         */
        private fun aggiornaMagazzino(
            magazzino: List<Avanzo>, piano: PianoDiTaglio, soglia: Double
        ): List<Avanzo> {
            // Quantità di avanzi disponibili per chiave (materiale + lunghezza), con un esempio per
            // ricostruirli.
            val disponibili = LinkedHashMap<String, Int>()
            val esempio = LinkedHashMap<String, Avanzo>()
            for (avanzo in magazzino) {
                val chiave = chiave(avanzo.materiale(), avanzo.lunghezza)
                disponibili.merge(chiave, avanzo.quantita, Int::plus)
                esempio.putIfAbsent(chiave, avanzo)
            }
            // Sottrai gli avanzi effettivamente usati: una barra-avanzo nel piano = un'unità consumata.
            for (barra in piano.barre()) {
                if (barra.avanzo()) {
                    disponibili.computeIfPresent(chiave(barra.materiale(), barra.lunghezzaBarra())) { _, n -> n - 1 }
                }
            }
            // Unione finale: avanzi rimasti + ritagli (sfrido) delle barre sopra soglia.
            val unione = LinkedHashMap<String, Avanzo>()
            disponibili.forEach { (chiave, quantita) ->
                if (quantita > 0) {
                    val a = esempio.getValue(chiave)
                    accumula(unione, a.profilo, a.colore, a.lunghezza, quantita)
                }
            }
            for (barra in piano.barre()) {
                if (barra.sfrido() >= soglia) {
                    accumula(unione, barra.profilo(), barra.colore(), arrotonda(barra.sfrido()), 1)
                }
            }
            return ArrayList(unione.values)
        }

        /**
         * Il ritaglio al decimo di millimetro. Gli avanzi si fondono per **lunghezza esatta**, e uno
         * sfrido è una somma di float: due residui identici in officina possono differire di 1e-13 e
         * diventare due righe di magazzino invece di una `x2`. Arrotondare qui li riunisce — e
         * comunque nessuno rimette in rastrelliera uno spezzone quotato al nanometro.
         */
        private fun arrotonda(mm: Double): Double = (mm * 10.0).roundToLong() / 10.0

        /**
         * Aggiunge `quantita` avanzi (profilo, colore, lunghezza), fondendoli con gli identici già
         * presenti.
         */
        private fun accumula(
            unione: MutableMap<String, Avanzo>, profilo: Profilo, colore: Colore,
            lunghezza: Double, quantita: Int
        ) {
            val chiave = chiave(Materiale(profilo, colore), lunghezza)
            unione.merge(chiave, Avanzo(profilo, colore, lunghezza, quantita)) { vecchio, _ ->
                Avanzo(profilo, colore, lunghezza, vecchio.quantita + quantita)
            }
        }

        private fun chiave(materiale: Materiale, lunghezza: Double): String =
            materiale.profilo.codice + "|" + materiale.colore.nome() + "@" + lunghezza
    }
}
