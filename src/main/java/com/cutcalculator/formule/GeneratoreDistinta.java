package com.cutcalculator.formule;

import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Pezzo;
import com.cutcalculator.dominio.RegolaTaglio;
import com.cutcalculator.dominio.Serramento;

import java.util.ArrayList;
import java.util.List;

/**
 * Il primo motore della pipeline: trasforma un {@link Ordine} nella {@link Distinta}
 * dei pezzi da tagliare, applicando le regole di ogni tipologia alle misure del serramento.
 * <p>
 * I pezzi sono "esplosi": una riga con quantità Q, in un serramento richiesto N volte,
 * produce Q×N pezzi identici.
 */
public class GeneratoreDistinta {

    public Distinta genera(Ordine ordine) {
        List<Pezzo> pezzi = new ArrayList<>();
        for (Serramento serramento : ordine.serramenti()) {
            for (RegolaTaglio regola : serramento.tipologia().regole()) {
                double lunghezza = regola.lunghezza(serramento.dimensione());
                int quantita = regola.quantita() * serramento.quantita();
                for (int i = 0; i < quantita; i++) {
                    pezzi.add(new Pezzo(regola.profilo(), lunghezza,
                            regola.tipoTaglio(), regola.descrizione()));
                }
            }
        }
        return new Distinta(pezzi);
    }
}
