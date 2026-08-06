package com.cutcalculator.pianificazione;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Pezzo;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.formule.GeneratoreDistinta;
import com.cutcalculator.ottimizzatore.BarraTagliata;
import com.cutcalculator.ottimizzatore.BestFitDecreasing;
import com.cutcalculator.ottimizzatore.Ottimizzatore;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.preventivo.GeneratorePreventivo;
import com.cutcalculator.preventivo.Preventivo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pianifica <b>più ordini insieme</b> con un magazzino di avanzi <b>condiviso</b>: ogni avanzo
 * fisico va a un solo ordine, così lo stesso pezzo non finisce in due ordini. Ci sta <i>sopra</i>
 * l'ottimizzatore per-ordine, senza modificarlo.
 * <p>
 * Algoritmo — <b>"asta degli avanzi"</b> (greedy globale):
 * <ol>
 *   <li>per ogni ordine si genera la distinta e si tengono i pezzi ancora da piazzare (per profilo);
 *       il magazzino si espande in singole unità-avanzo (una per pezzo di quantità);</li>
 *   <li>a turni: fra tutte le coppie (ordine, avanzo) si prende quella in cui l'avanzo, riempito al
 *       meglio con i pezzi ancora liberi di quell'ordine, lascia <b>meno sfrido</b>; l'avanzo esce dal
 *       magazzino, i pezzi piazzati escono dai "da fare", la barra entra nel piano di quell'ordine.
 *       Così l'avanzo conteso va a chi lo sfrutta meglio, e chi perde ripiega a cascata;</li>
 *   <li>i pezzi rimasti di ogni ordine finiscono in barre <b>nuove</b> (l'ottimizzatore standard);</li>
 *   <li>gli avanzi usati sono <b>consumati</b>; i residui delle barre <b>≥ soglia</b> tornano in
 *       magazzino come nuovi avanzi (per le sessioni future, non per questo stesso calcolo).</li>
 * </ol>
 * L'oggetto è puro: non tocca né stato né disco, restituisce tutto in {@link EvasioneOrdini}.
 */
public final class PianificatoreOrdini {

    /** Lunghezza minima (mm) perché un residuo di taglio valga come nuovo avanzo riusabile. */
    public static final double SOGLIA_RITAGLIO_DEFAULT = 500.0;

    private static final double EPS = 1e-9;

    private final GeneratoreDistinta generatoreDistinta = new GeneratoreDistinta();
    private final Ottimizzatore ottimizzatore = new BestFitDecreasing();
    private final GeneratorePreventivo generatorePreventivo = new GeneratorePreventivo();

    /** Come {@link #pianifica(List, List, double, double)} con soglia e barra standard di default. */
    public EvasioneOrdini pianifica(List<Ordine> ordini, List<Avanzo> magazzino) {
        return pianifica(ordini, magazzino, SOGLIA_RITAGLIO_DEFAULT, Ottimizzatore.BARRA_STANDARD_DEFAULT);
    }

    /**
     * @param ordini        gli ordini da evadere insieme
     * @param magazzino     gli avanzi condivisi disponibili (non viene modificato)
     * @param soglia        lunghezza minima perché un ritaglio rientri come nuovo avanzo
     * @param barraStandard lunghezza della barra nuova
     * @throws IllegalArgumentException se un pezzo è più lungo della barra standard
     */
    public EvasioneOrdini pianifica(List<Ordine> ordini, List<Avanzo> magazzino,
            double soglia, double barraStandard) {

        // 1. Stato per ordine: i pezzi ancora da piazzare (per materiale) e le barre-avanzo vinte.
        List<Map<Materiale, List<Pezzo>>> daPiazzare = new ArrayList<>();
        List<List<BarraTagliata>> barreVinte = new ArrayList<>();
        for (Ordine ordine : ordini) {
            Map<Materiale, List<Pezzo>> perMateriale = new LinkedHashMap<>();
            generatoreDistinta.genera(ordine).perMateriale()
                    .forEach((materiale, pezzi) -> perMateriale.put(materiale, new ArrayList<>(pezzi)));
            daPiazzare.add(perMateriale);
            barreVinte.add(new ArrayList<>());
        }

        // 2. Avanzi disponibili, espansi in singole unità (lunghezze) per materiale.
        Map<Materiale, List<Double>> avanziLiberi = espandiAvanzi(magazzino);

        // 3. Asta: un avanzo alla volta all'ordine che lo riempie con meno sfrido.
        for (Assegnazione scelta = migliore(daPiazzare, avanziLiberi);
                scelta != null;
                scelta = migliore(daPiazzare, avanziLiberi)) {
            avanziLiberi.get(scelta.materiale()).remove(scelta.lunghezzaAvanzo());
            List<Pezzo> rimanenti = daPiazzare.get(scelta.ordine()).get(scelta.materiale());
            for (Pezzo usato : scelta.barra().pezzi()) {
                rimanenti.remove(usato);
            }
            barreVinte.get(scelta.ordine()).add(scelta.barra());
        }

        // 4-5. Pezzi rimasti → barre nuove; piano per ordine + raccolta di tutte le barre.
        List<EvasioneOrdini.RisultatoOrdine> perOrdine = new ArrayList<>();
        List<BarraTagliata> tutteLeBarre = new ArrayList<>();
        for (int i = 0; i < ordini.size(); i++) {
            List<Pezzo> rimasti = new ArrayList<>();
            daPiazzare.get(i).values().forEach(rimasti::addAll);
            List<BarraTagliata> barre = new ArrayList<>(barreVinte.get(i));
            if (!rimasti.isEmpty()) {
                barre.addAll(ottimizzatore.ottimizza(new Distinta(rimasti), List.of(), barraStandard).barre());
            }
            PianoDiTaglio piano = new PianoDiTaglio(barre);
            perOrdine.add(new EvasioneOrdini.RisultatoOrdine(ordini.get(i), piano));
            tutteLeBarre.addAll(barre);
        }

        // 6-7. Preventivo totale + magazzino aggiornato (non usati + ritagli sopra soglia).
        Preventivo preventivoTotale = generatorePreventivo.genera(new PianoDiTaglio(tutteLeBarre));
        List<Avanzo> aggiornato = aggiornaMagazzino(avanziLiberi, tutteLeBarre, soglia);
        return new EvasioneOrdini(perOrdine, preventivoTotale, aggiornato);
    }

    /** La coppia (ordine, avanzo) col riempimento a sfrido minimo; {@code null} se nessuno entra. */
    private Assegnazione migliore(List<Map<Materiale, List<Pezzo>>> daPiazzare,
            Map<Materiale, List<Double>> avanziLiberi) {
        Assegnazione migliore = null;
        for (int o = 0; o < daPiazzare.size(); o++) {
            for (Map.Entry<Materiale, List<Pezzo>> gruppo : daPiazzare.get(o).entrySet()) {
                Materiale materiale = gruppo.getKey();
                List<Pezzo> pezzi = gruppo.getValue();
                List<Double> avanzi = avanziLiberi.get(materiale);
                if (pezzi.isEmpty() || avanzi == null || avanzi.isEmpty()) {
                    continue;
                }
                for (double lunghezza : avanzi.stream().distinct().sorted().toList()) {
                    BarraTagliata prova = riempi(materiale, lunghezza, pezzi);
                    if (prova.pezzi().isEmpty()) {
                        continue;
                    }
                    if (migliore == null || prova.sfrido() < migliore.barra().sfrido() - EPS) {
                        migliore = new Assegnazione(o, materiale, lunghezza, prova);
                    }
                }
            }
        }
        return migliore;
    }

    /** Riempie una barra della lunghezza data coi pezzi (Best-Fit-Decreasing su singola barra). */
    private static BarraTagliata riempi(Materiale materiale, double lunghezza, List<Pezzo> pezzi) {
        BarraTagliata barra = new BarraTagliata(materiale.profilo(), materiale.colore(), lunghezza, true);
        List<Pezzo> ordinati = new ArrayList<>(pezzi);
        ordinati.sort(Comparator.comparingDouble(Pezzo::lunghezza).reversed());
        for (Pezzo pezzo : ordinati) {
            if (barra.entra(pezzo)) {
                barra.aggiungi(pezzo);
            }
        }
        return barra;
    }

    private static Map<Materiale, List<Double>> espandiAvanzi(List<Avanzo> magazzino) {
        Map<Materiale, List<Double>> perMateriale = new LinkedHashMap<>();
        for (Avanzo avanzo : magazzino) {
            List<Double> unita = perMateriale.computeIfAbsent(avanzo.materiale(), m -> new ArrayList<>());
            for (int i = 0; i < avanzo.quantita(); i++) {
                unita.add(avanzo.lunghezza());
            }
        }
        return perMateriale;
    }

    /** Magazzino post-evasione: gli avanzi non usati più i ritagli ≥ soglia, uniti per materiale+lunghezza. */
    private static List<Avanzo> aggiornaMagazzino(Map<Materiale, List<Double>> avanziLiberi,
            List<BarraTagliata> tutteLeBarre, double soglia) {
        Map<String, Avanzo> unione = new LinkedHashMap<>();
        avanziLiberi.forEach((materiale, lunghezze) ->
                lunghezze.forEach(lunghezza -> accumula(unione, materiale, lunghezza)));
        for (BarraTagliata barra : tutteLeBarre) {
            if (barra.sfrido() >= soglia) {
                accumula(unione, barra.materiale(), barra.sfrido());
            }
        }
        return new ArrayList<>(unione.values());
    }

    /** Aggiunge un'unità dell'avanzo (materiale, lunghezza), fondendola con le identiche già presenti. */
    private static void accumula(Map<String, Avanzo> unione, Materiale materiale, double lunghezza) {
        String chiave = materiale.profilo().codice() + "|" + materiale.colore().nome() + "@" + lunghezza;
        unione.merge(chiave, new Avanzo(materiale.profilo(), materiale.colore(), lunghezza, 1),
                (vecchio, nuovo) -> new Avanzo(materiale.profilo(), materiale.colore(),
                        lunghezza, vecchio.quantita() + 1));
    }

    /** Una candidata assegnazione dell'asta: la barra è già riempita di prova. */
    private record Assegnazione(int ordine, Materiale materiale, double lunghezzaAvanzo, BarraTagliata barra) {
    }
}
