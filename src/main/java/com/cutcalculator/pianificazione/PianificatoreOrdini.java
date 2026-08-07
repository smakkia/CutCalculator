package com.cutcalculator.pianificazione;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Pezzo;
import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.formule.GeneratoreDistinta;
import com.cutcalculator.ottimizzatore.BarraTagliata;
import com.cutcalculator.ottimizzatore.BestFitDecreasing;
import com.cutcalculator.ottimizzatore.Ottimizzatore;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.preventivo.GeneratorePreventivo;
import com.cutcalculator.preventivo.Preventivo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pianifica <b>più ordini insieme unendoli come se fossero uno solo</b>: i pezzi di tutti gli ordini
 * finiscono in un'unica {@link Distinta} e vengono ottimizzati in un colpo solo contro il magazzino
 * condiviso. Così pezzi di ordini diversi possono <b>condividere la stessa barra</b> e lo sfrido cala,
 * rispetto all'ottimizzare ogni ordine per conto suo.
 * <p>
 * Algoritmo:
 * <ol>
 *   <li>si genera la distinta di ogni ordine e si mettono tutti i pezzi in una sola distinta;</li>
 *   <li>l'{@link Ottimizzatore} standard impacchetta il tutto: prima riusa gli avanzi di magazzino
 *       (per {@link Materiale}), poi apre barre nuove per i pezzi rimasti;</li>
 *   <li>gli avanzi usati sono <b>consumati</b>; i residui delle barre <b>≥ soglia</b> tornano in
 *       magazzino come nuovi avanzi (per le sessioni future, non per questo stesso calcolo).</li>
 * </ol>
 * L'oggetto è puro: non tocca né stato né disco, restituisce tutto in {@link EvasioneOrdini}.
 */
public final class PianificatoreOrdini {

    /** Lunghezza minima (mm) perché un residuo di taglio valga come nuovo avanzo riusabile. */
    public static final double SOGLIA_RITAGLIO_DEFAULT = 500.0;

    private final GeneratoreDistinta generatoreDistinta = new GeneratoreDistinta();
    private final Ottimizzatore ottimizzatore = new BestFitDecreasing();
    private final GeneratorePreventivo generatorePreventivo = new GeneratorePreventivo();

    /** Come {@link #pianifica(List, List, double, double)} con soglia e barra standard di default. */
    public EvasioneOrdini pianifica(List<Ordine> ordini, List<Avanzo> magazzino) {
        return pianifica(ordini, magazzino, SOGLIA_RITAGLIO_DEFAULT, Ottimizzatore.BARRA_STANDARD_DEFAULT);
    }

    /**
     * @param ordini        gli ordini da evadere insieme (uniti in un unico piano)
     * @param magazzino     gli avanzi condivisi disponibili (non viene modificato)
     * @param soglia        lunghezza minima perché un ritaglio rientri come nuovo avanzo
     * @param barraStandard lunghezza della barra nuova
     * @throws IllegalArgumentException se un pezzo è più lungo della barra standard
     */
    public EvasioneOrdini pianifica(List<Ordine> ordini, List<Avanzo> magazzino,
            double soglia, double barraStandard) {

        // 1. Tutti i pezzi di tutti gli ordini in un'unica distinta.
        List<Pezzo> tutti = new ArrayList<>();
        for (Ordine ordine : ordini) {
            tutti.addAll(generatoreDistinta.genera(ordine).pezzi());
        }

        // 2. Un solo piano di taglio per l'insieme (riusa gli avanzi condivisi, poi barre nuove).
        PianoDiTaglio piano = ottimizzatore.ottimizza(new Distinta(tutti), magazzino, barraStandard);

        // 3. Preventivo aggregato + magazzino aggiornato (avanzi consumati + ritagli sopra soglia).
        Preventivo preventivo = generatorePreventivo.genera(piano);
        List<Avanzo> aggiornato = aggiornaMagazzino(magazzino, piano, soglia);
        return new EvasioneOrdini(ordini, piano, preventivo, aggiornato);
    }

    /** Magazzino post-evasione: avanzi non usati + ritagli ≥ soglia, uniti per materiale+lunghezza. */
    private static List<Avanzo> aggiornaMagazzino(List<Avanzo> magazzino, PianoDiTaglio piano, double soglia) {
        // Quantità di avanzi disponibili per chiave (materiale + lunghezza), con un esempio per ricostruirli.
        Map<String, Integer> disponibili = new LinkedHashMap<>();
        Map<String, Avanzo> esempio = new LinkedHashMap<>();
        for (Avanzo avanzo : magazzino) {
            String chiave = chiave(avanzo.materiale(), avanzo.lunghezza());
            disponibili.merge(chiave, avanzo.quantita(), Integer::sum);
            esempio.putIfAbsent(chiave, avanzo);
        }
        // Sottrai gli avanzi effettivamente usati: una barra-avanzo nel piano = un'unità consumata.
        for (BarraTagliata barra : piano.barre()) {
            if (barra.avanzo()) {
                disponibili.computeIfPresent(chiave(barra.materiale(), barra.lunghezzaBarra()), (k, n) -> n - 1);
            }
        }
        // Unione finale: avanzi rimasti + ritagli (sfrido) delle barre sopra soglia.
        Map<String, Avanzo> unione = new LinkedHashMap<>();
        disponibili.forEach((chiave, quantita) -> {
            if (quantita > 0) {
                Avanzo a = esempio.get(chiave);
                accumula(unione, a.profilo(), a.colore(), a.lunghezza(), quantita);
            }
        });
        for (BarraTagliata barra : piano.barre()) {
            if (barra.sfrido() >= soglia) {
                accumula(unione, barra.profilo(), barra.colore(), barra.sfrido(), 1);
            }
        }
        return new ArrayList<>(unione.values());
    }

    /** Aggiunge {@code quantita} avanzi (profilo, colore, lunghezza), fondendoli con gli identici già presenti. */
    private static void accumula(Map<String, Avanzo> unione, Profilo profilo, Colore colore,
            double lunghezza, int quantita) {
        String chiave = chiave(new Materiale(profilo, colore), lunghezza);
        unione.merge(chiave, new Avanzo(profilo, colore, lunghezza, quantita),
                (vecchio, nuovo) -> new Avanzo(profilo, colore, lunghezza, vecchio.quantita() + quantita));
    }

    private static String chiave(Materiale materiale, double lunghezza) {
        return materiale.profilo().codice() + "|" + materiale.colore().nome() + "@" + lunghezza;
    }
}
