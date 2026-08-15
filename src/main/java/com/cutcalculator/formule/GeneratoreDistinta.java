package com.cutcalculator.formule;

import com.cutcalculator.dominio.Dimensione;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Pezzo;
import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.dominio.RegolaTaglio;
import com.cutcalculator.dominio.RegolaVetro;
import com.cutcalculator.dominio.Serramento;
import com.cutcalculator.dominio.Varianti;
import com.cutcalculator.dominio.Vetro;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Il primo motore della pipeline: trasforma un {@link Ordine} nella {@link Distinta} dei pezzi da
 * tagliare e delle lastre da ordinare, applicando le regole di ogni tipologia alle misure del
 * serramento.
 * <p>
 * I pezzi sono "esplosi": una riga con quantità Q, in un serramento richiesto N volte,
 * produce Q×N pezzi identici. Il <b>vetro</b> no: la stessa doppia moltiplicazione finisce nella
 * {@code quantita} di una sola riga {@link Vetro}, perché le lastre non vanno piazzate una a una
 * in una barra — vanno solo contate e sommate per area.
 */
public class GeneratoreDistinta {

    public Distinta genera(Ordine ordine) {
        List<Pezzo> pezzi = new ArrayList<>();
        List<Vetro> vetri = new ArrayList<>();
        for (Serramento serramento : ordine.serramenti()) {
            Varianti varianti = serramento.varianti();
            for (RegolaTaglio regola : serramento.tipologia().regole()) {
                // Le varianti sostituiscono il profilo e accorciano ciò che gli sta dentro: la
                // ricetta resta quella, cambiano il codice da tagliare e la quota.
                Profilo profilo = varianti.profiloDi(regola.profilo());
                double lunghezza = regola.lunghezza(serramento.dimensione(), varianti);
                verifica(lunghezza, regola.descrizione(), serramento);
                int quantita = regola.quantita() * serramento.quantita();
                for (int i = 0; i < quantita; i++) {
                    // Il listino viaggia col pezzo: due serramenti dello stesso profilo possono
                    // avere prezzi diversi (colori diversi), e il costo va calcolato con il suo.
                    pezzi.add(new Pezzo(profilo, serramento.colore(), lunghezza,
                            regola.tipoTaglio(), regola.descrizione(), serramento.prezzi()));
                }
            }
            for (RegolaVetro regola : serramento.tipologia().regoleVetro()) {
                Vetro lastra = regola.calcola(serramento.dimensione(), varianti);
                verifica(lastra.lunghezza(), regola.descrizione() + " (altezza)", serramento);
                verifica(lastra.larghezza(), regola.descrizione() + " (larghezza)", serramento);
                vetri.add(new Vetro(lastra.lunghezza(), lastra.larghezza(),
                        lastra.quantita() * serramento.quantita(), serramento.prezzi()));
            }
        }
        return new Distinta(pezzi, vetri);
    }

    /**
     * Una quota calcolata dev'essere positiva: se una formula dà zero o un numero negativo il
     * serramento è più piccolo di quanto la sua ricetta permetta (una porta con HF sotto i 188 mm,
     * per dire), oppure le varianti scelte lo restringono più della sua stessa luce.
     * <p>
     * Senza questo controllo un pezzo negativo passerebbe l'intera pipeline senza un avviso, e
     * nell'ottimizzatore <b>libererebbe</b> spazio sulla barra invece di occuparlo: piano, peso e
     * costo verrebbero sbagliati con l'aria di essere giusti. Meglio fermarsi qui e dirlo.
     */
    private static void verifica(double quota, String descrizione, Serramento serramento) {
        if (quota > 0) {
            return;
        }
        Dimensione d = serramento.dimensione();
        throw new IllegalArgumentException(String.format(Locale.ROOT,
                "quota non valida in \"%s\": %s viene %.1f mm, ma deve essere positiva."
                        + " Misure del serramento: L %.1f, H %.1f, HF %.1f mm."
                        + " Controlla le misure e le varianti scelte.",
                serramento.tipologia().nome(), descrizione, quota, d.L(), d.H(), d.HF()));
    }
}
