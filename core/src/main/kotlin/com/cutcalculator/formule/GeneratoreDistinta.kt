package com.cutcalculator.formule

import com.cutcalculator.dominio.Ordine
import com.cutcalculator.dominio.Pezzo
import com.cutcalculator.dominio.Serramento
import com.cutcalculator.dominio.Vetro
import java.util.Locale

/**
 * Il primo motore della pipeline: trasforma un [Ordine] nella [Distinta] dei pezzi da tagliare e
 * delle lastre da ordinare, applicando le regole di ogni tipologia alle misure del serramento.
 *
 * I pezzi sono "esplosi": una riga con quantità Q, in un serramento richiesto N volte, produce Q×N
 * pezzi identici. Il **vetro** no: la stessa doppia moltiplicazione finisce nella `quantita` di una
 * sola riga [Vetro], perché le lastre non vanno piazzate una a una in una barra — vanno solo contate
 * e sommate per area.
 */
class GeneratoreDistinta {

    fun genera(ordine: Ordine): Distinta {
        val pezzi = ArrayList<Pezzo>()
        val vetri = ArrayList<Vetro>()
        for (serramento in ordine.serramenti()) {
            val varianti = serramento.varianti
            for (regola in serramento.tipologia.regole()) {
                // Le varianti sostituiscono il profilo e accorciano ciò che gli sta dentro: la
                // ricetta resta quella, cambiano il codice da tagliare e la quota.
                val profilo = varianti.profiloDi(regola.profilo)
                val lunghezza = regola.lunghezza(serramento.dimensione, varianti)
                verifica(lunghezza, regola.descrizione, serramento)
                val quantita = regola.quantita * serramento.quantita
                repeat(quantita) {
                    // Il listino viaggia col pezzo: due serramenti dello stesso profilo possono
                    // avere prezzi diversi (colori diversi), e il costo va calcolato con il suo.
                    pezzi.add(
                        Pezzo(
                            profilo, serramento.colore, lunghezza,
                            regola.tipoTaglio, regola.descrizione, serramento.prezzi
                        )
                    )
                }
            }
            for (regola in serramento.tipologia.regoleVetro()) {
                val lastra = regola.calcola(serramento.dimensione, varianti)
                verifica(lastra.lunghezza, regola.descrizione + " (altezza)", serramento)
                verifica(lastra.larghezza, regola.descrizione + " (larghezza)", serramento)
                vetri.add(
                    Vetro(
                        lastra.lunghezza, lastra.larghezza,
                        lastra.quantita * serramento.quantita, serramento.prezzi
                    )
                )
            }
        }
        return Distinta(pezzi, vetri)
    }

    private companion object {
        /**
         * Una quota calcolata dev'essere positiva: se una formula dà zero o un numero negativo il
         * serramento è più piccolo di quanto la sua ricetta permetta (una porta con HF sotto i
         * 188 mm, per dire), oppure le varianti scelte lo restringono più della sua stessa luce.
         *
         * Senza questo controllo un pezzo negativo passerebbe l'intera pipeline senza un avviso, e
         * nell'ottimizzatore **libererebbe** spazio sulla barra invece di occuparlo: piano, peso e
         * costo verrebbero sbagliati con l'aria di essere giusti. Meglio fermarsi qui e dirlo.
         */
        fun verifica(quota: Double, descrizione: String, serramento: Serramento) {
            if (quota > 0) {
                return
            }
            val d = serramento.dimensione
            throw IllegalArgumentException(
                String.format(
                    Locale.ROOT,
                    "quota non valida in \"%s\": %s viene %.1f mm, ma deve essere positiva." +
                            " Misure del serramento: L %.1f, H %.1f, HF %.1f mm." +
                            " Controlla le misure e le varianti scelte.",
                    serramento.tipologia.nome(), descrizione, quota, d.L, d.H, d.HF
                )
            )
        }
    }
}
