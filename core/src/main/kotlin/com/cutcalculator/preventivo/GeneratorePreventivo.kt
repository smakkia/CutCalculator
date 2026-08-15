package com.cutcalculator.preventivo

import com.cutcalculator.dominio.Avanzo
import com.cutcalculator.dominio.Materiale
import com.cutcalculator.dominio.Vetro
import com.cutcalculator.ottimizzatore.BarraTagliata
import com.cutcalculator.ottimizzatore.PianoDiTaglio

/**
 * L'ultimo motore della pipeline: aggrega un [PianoDiTaglio] in un [Preventivo], una riga per
 * [Materiale] (profilo + colore). Per ogni materiale conta le barre nuove e gli avanzi riusati,
 * somma le lunghezze delle barre nuove e lo sfrido di tutte le barre, e separa dallo sfrido i
 * **ritagli recuperabili** — i residui lunghi almeno la soglia, che dopo l'evasione torneranno in
 * magazzino come nuovi avanzi.
 *
 * Il **vetro** non passa dal piano di taglio (non si ricava da una barra): le lastre arrivano
 * direttamente dalla distinta e vengono aggregate per misura in [RigaVetro].
 */
class GeneratorePreventivo {

    /** Come [genera] con la soglia di riuso di default. */
    fun genera(piano: PianoDiTaglio): Preventivo = genera(piano, Avanzo.LUNGHEZZA_MINIMA_RIUSO)

    /** Come [genera] con la soglia di riuso di default. */
    fun genera(piano: PianoDiTaglio, vetri: List<Vetro>): Preventivo =
        genera(piano, vetri, Avanzo.LUNGHEZZA_MINIMA_RIUSO)

    /**
     * @param piano  il piano da aggregare
     * @param soglia lunghezza minima perché un residuo conti come ritaglio recuperabile e non come
     *               scarto (stessa regola che applica poi il pianificatore al magazzino)
     */
    fun genera(piano: PianoDiTaglio, soglia: Double): Preventivo =
        genera(piano, emptyList(), soglia)

    /**
     * @param piano  il piano da aggregare
     * @param vetri  le lastre della distinta, da aggregare per misura
     * @param soglia lunghezza minima perché un residuo conti come ritaglio recuperabile
     */
    fun genera(piano: PianoDiTaglio, vetri: List<Vetro>, soglia: Double): Preventivo {
        val righe = ArrayList<RigaProfilo>()

        for ((materiale, barre) in piano.perMateriale()) {
            var barreNuove = 0
            var avanziUsati = 0
            var lunghezzaNuova = 0.0
            var sfrido = 0.0
            var ritagli = 0
            var lunghezzaRecuperabile = 0.0
            var costo = 0.0

            for (barra in barre) {
                sfrido += barra.sfrido()
                if (barra.sfrido() >= soglia) {
                    ritagli++
                    lunghezzaRecuperabile += barra.sfrido()
                }
                if (barra.avanzo()) {
                    avanziUsati++
                } else {
                    barreNuove++
                    lunghezzaNuova += barra.lunghezzaBarra()
                    costo += costoDi(barra)
                }
            }
            righe.add(
                RigaProfilo(
                    materiale.profilo, materiale.colore, barreNuove,
                    avanziUsati, lunghezzaNuova, sfrido, ritagli, lunghezzaRecuperabile, costo
                )
            )
        }
        return Preventivo(righe, aggregaVetri(vetri))
    }

    private companion object {
        /**
         * Quanto costa una barra **nuova**: si paga tutta, sfrido compreso, e il conto lo dividono i
         * pezzi che ci stanno sopra in proporzione alla loro lunghezza — ciascuno al **proprio**
         * €/kg.
         *
         * Non è un dettaglio pedante: il prezzo dell'alluminio arriva dal serramento (cambia col
         * colore), quindi la stessa barra può contenere pezzi pagati a prezzi diversi, e un solo
         * prezzo per riga darebbe un totale sbagliato. Gli **avanzi** non entrano nel conto: sono
         * già di proprietà.
         */
        fun costoDi(barra: BarraTagliata): Double {
            val lunghezzaPezzi = barra.pezzi().sumOf { it.lunghezza }
            if (lunghezzaPezzi <= 0) {
                return 0.0   // barra nuova senza pezzi: non dovrebbe succedere, ma non si paga
            }
            val pesoBarra = barra.profilo().peso(barra.lunghezzaBarra())
            return barra.pezzi().sumOf { pezzo ->
                pesoBarra * (pezzo.lunghezza / lunghezzaPezzi) * pezzo.prezzoAlChilo()
            }
        }

        /**
         * Lastre della stessa misura fuse in una riga sola, con le quantità sommate, nell'ordine in
         * cui compaiono. Le misure identiche escono da formule identiche, quindi il confronto esatto
         * tra double è affidabile qui: nessuna tolleranza da tarare.
         */
        fun aggregaVetri(vetri: List<Vetro>): List<RigaVetro> {
            val perMisura = LinkedHashMap<String, RigaVetro>()
            for (vetro in vetri) {
                // Il prezzo entra nella chiave: stessa misura ma €/mq diverso = due righe diverse,
                // altrimenti il costo di una si perderebbe dentro l'altra.
                val chiave = "${vetro.lunghezza}x${vetro.larghezza}@${vetro.prezzi.alMqVetro}"
                perMisura.merge(
                    chiave,
                    RigaVetro(vetro.lunghezza, vetro.larghezza, vetro.quantita, vetro.costo())
                ) { vecchia, nuova ->
                    RigaVetro(
                        vecchia.lunghezza, vecchia.larghezza,
                        vecchia.quantita + nuova.quantita, vecchia.costo + nuova.costo
                    )
                }
            }
            return ArrayList(perMisura.values)
        }
    }
}
