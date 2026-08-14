package com.cutcalculator.formule;

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
                vetri.add(new Vetro(lastra.lunghezza(), lastra.larghezza(),
                        lastra.quantita() * serramento.quantita(), serramento.prezzi()));
            }
        }
        return new Distinta(pezzi, vetri);
    }
}
