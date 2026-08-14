package com.cutcalculator.formule;

import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.dominio.Pezzo;
import com.cutcalculator.dominio.Vetro;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * L'output della Fase 1: tutto ciò che serve produrre per un ordine — i <b>pezzi</b> da tagliare
 * dalle barre e le <b>lastre di vetro</b> da ordinare.
 * <p>
 * Oltre alla lista piatta offre il raggruppamento per {@link Materiale} (profilo + colore),
 * che serve all'ottimizzatore: un pezzo può essere ricavato solo da una barra dello stesso
 * profilo <b>e</b> colore.
 * <p>
 * I {@link Vetro vetri} viaggiano a parte perché non entrano nell'ottimizzatore 1D: si contano e si
 * sommano per area. Sono anche l'unica parte non "esplosa" — ogni riga porta la sua quantità.
 */
public record Distinta(List<Pezzo> pezzi, List<Vetro> vetri) {

    /** Copie difensive: le liste sono immutabili una volta creata la distinta. */
    public Distinta {
        pezzi = List.copyOf(pezzi);
        vetri = List.copyOf(vetri);
    }

    /** Distinta di soli profili: per le tipologie di cui non si conoscono le quote del vetro. */
    public Distinta(List<Pezzo> pezzi) {
        this(pezzi, List.of());
    }

    /** Numero totale di pezzi da tagliare. */
    public int totalePezzi() {
        return pezzi.size();
    }

    /** Numero totale di lastre da ordinare (somma delle quantità delle righe vetro). */
    public int totaleLastre() {
        return vetri.stream().mapToInt(Vetro::quantita).sum();
    }

    /** Superficie vetrata complessiva, in metri quadri. */
    public double areaVetroTotaleMq() {
        return vetri.stream().mapToDouble(Vetro::areaTotaleMq).sum();
    }

    /** Pezzi raggruppati per {@link Materiale} (profilo + colore), nell'ordine in cui compaiono. */
    public Map<Materiale, List<Pezzo>> perMateriale() {
        return pezzi.stream().collect(Collectors.groupingBy(
                Pezzo::materiale, LinkedHashMap::new, Collectors.toList()));
    }
}
