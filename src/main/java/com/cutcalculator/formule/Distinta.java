package com.cutcalculator.formule;

import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.dominio.Pezzo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * L'output della Fase 1: tutti i pezzi da tagliare per un ordine.
 * <p>
 * Oltre alla lista piatta offre il raggruppamento per {@link Materiale} (profilo + colore),
 * che serve all'ottimizzatore: un pezzo può essere ricavato solo da una barra dello stesso
 * profilo <b>e</b> colore.
 */
public record Distinta(List<Pezzo> pezzi) {

    /** Copia difensiva: la lista di pezzi è immutabile una volta creata la distinta. */
    public Distinta {
        pezzi = List.copyOf(pezzi);
    }

    /** Numero totale di pezzi da tagliare. */
    public int totalePezzi() {
        return pezzi.size();
    }

    /** Pezzi raggruppati per {@link Materiale} (profilo + colore), nell'ordine in cui compaiono. */
    public Map<Materiale, List<Pezzo>> perMateriale() {
        return pezzi.stream().collect(Collectors.groupingBy(
                Pezzo::materiale, LinkedHashMap::new, Collectors.toList()));
    }
}
