package com.cutcalculator.formule;

import com.cutcalculator.dominio.Pezzo;
import com.cutcalculator.dominio.Profilo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * L'output della Fase 1: tutti i pezzi da tagliare per un ordine.
 * <p>
 * Oltre alla lista piatta offre il raggruppamento per profilo, che serve
 * all'ottimizzatore: un pezzo può essere ricavato solo da una barra del suo
 * stesso profilo.
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

    /** Pezzi raggruppati per profilo, nell'ordine in cui compaiono. */
    public Map<Profilo, List<Pezzo>> perProfilo() {
        return pezzi.stream().collect(Collectors.groupingBy(
                Pezzo::profilo, LinkedHashMap::new, Collectors.toList()));
    }
}
