package com.cutcalculator.ottimizzatore;

import com.cutcalculator.dominio.Materiale;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * L'output della Fase 2: il piano di taglio completo di un ordine — l'elenco delle
 * barre (nuove da comprare + avanzi riusati) con dentro i pezzi già assegnati.
 * <p>
 * È immutabile (copia difensiva): una volta prodotto dall'ottimizzatore non si tocca.
 * L'ottimizzatore può generare più piani candidati e scegliere il migliore: la metrica
 * è la {@link #mediaGeometricaSfrido() media geometrica degli sfridi} per barra — più è
 * bassa, meno materiale si spreca, migliore è il piano.
 */
public record PianoDiTaglio(List<BarraTagliata> barre) {

    /**
     * Offset additivo (mm) sommato a ogni sfrido prima della media geometrica: tiene la
     * media pressoché intatta ma evita il caso limite dello sfrido 0 (che la azzererebbe).
     */
    private static final double OFFSET_SFRIDO = 0.1;

    public PianoDiTaglio {
        barre = List.copyOf(barre);
    }

    /** Barre totali usate: nuove comprate più avanzi riutilizzati. */
    public int numeroBarre() {
        return barre.size();
    }

    /** Solo le barre nuove da comprare: il fabbisogno reale di materiale. */
    public int barreNuove() {
        return (int) barre.stream().filter(b -> !b.avanzo()).count();
    }

    /**
     * Media geometrica degli sfridi per barra — la metrica di qualità del piano:
     * a parità di condizioni, il piano con media più bassa spreca meno ed è quello scelto.
     * <p>
     * Calcolata via logaritmi per stabilità numerica, con un piccolo {@link #OFFSET_SFRIDO}
     * additivo che evita il caso limite dello sfrido 0 senza alterarne il valore. Piano vuoto → 0.
     */
    public double mediaGeometricaSfrido() {
        if (barre.isEmpty()) {
            return 0.0;
        }
        double sommaLog = barre.stream()
                .mapToDouble(b -> Math.log(b.sfrido() + OFFSET_SFRIDO))
                .sum();
        return Math.exp(sommaLog / barre.size());
    }

    /** Barre raggruppate per {@link Materiale} (profilo + colore), nell'ordine in cui compaiono. */
    public Map<Materiale, List<BarraTagliata>> perMateriale() {
        return barre.stream().collect(Collectors.groupingBy(
                BarraTagliata::materiale, LinkedHashMap::new, Collectors.toList()));
    }

}
