package com.cutcalculator.pianificazione;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Pezzo;
import com.cutcalculator.dominio.Prezzi;
import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.dominio.Vetro;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.formule.GeneratoreDistinta;
import com.cutcalculator.ottimizzatore.BarraTagliata;
import com.cutcalculator.ottimizzatore.Ottimizzatore;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.ottimizzatore.Strategia;
import com.cutcalculator.preventivo.GeneratorePreventivo;
import com.cutcalculator.preventivo.Preventivo;
import com.cutcalculator.preventivo.RigaProfilo;

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
    public static final double SOGLIA_RITAGLIO_DEFAULT = Avanzo.LUNGHEZZA_MINIMA_RIUSO;

    private final GeneratoreDistinta generatoreDistinta = new GeneratoreDistinta();
    private final Ottimizzatore ottimizzatore;
    private final GeneratorePreventivo generatorePreventivo = new GeneratorePreventivo();

    /** Con l'euristica {@link Strategia#PREDEFINITA predefinita}. */
    public PianificatoreOrdini() {
        this(Strategia.PREDEFINITA.crea());
    }

    /**
     * Con l'euristica scelta dall'utente. Il pianificatore non sa quale sia: gli basta che sappia
     * impacchettare, e il resto del calcolo (distinta, preventivo, quote, ricircolo) non cambia.
     */
    public PianificatoreOrdini(Ottimizzatore ottimizzatore) {
        this.ottimizzatore = ottimizzatore;
    }

    /** Come {@link #pianifica(List, List, double, double)} con soglia e barra standard di default. */
    public EvasioneOrdini pianifica(List<Ordine> ordini, List<Avanzo> magazzino) {
        return pianifica(ordini, magazzino, SOGLIA_RITAGLIO_DEFAULT,
                Ottimizzatore.BARRA_STANDARD_DEFAULT);
    }

    /**
     * I prezzi non sono un parametro: ogni pezzo e ogni lastra si portano dietro il listino del
     * serramento da cui vengono, quindi il preventivo si valorizza da sé.
     *
     * @param ordini        gli ordini da evadere insieme (uniti in un unico piano)
     * @param magazzino     gli avanzi condivisi disponibili (non viene modificato)
     * @param soglia        lunghezza minima perché un ritaglio rientri come nuovo avanzo
     * @param barraStandard lunghezza della barra nuova
     * @throws IllegalArgumentException se un pezzo è più lungo della barra standard
     */
    public EvasioneOrdini pianifica(List<Ordine> ordini, List<Avanzo> magazzino,
            double soglia, double barraStandard) {

        // 1. Tutti i pezzi (e tutte le lastre) di tutti gli ordini in un'unica distinta.
        //    Le singole distinte restano da parte: servono a ripartire il preventivo per ordine.
        List<Pezzo> tutti = new ArrayList<>();
        List<Vetro> tuttiVetri = new ArrayList<>();
        List<Distinta> perOrdine = new ArrayList<>();
        for (Ordine ordine : ordini) {
            Distinta distinta = generatoreDistinta.genera(ordine);
            perOrdine.add(distinta);
            tutti.addAll(distinta.pezzi());
            tuttiVetri.addAll(distinta.vetri());
        }

        // 2. Un solo piano di taglio per l'insieme (riusa gli avanzi condivisi, poi barre nuove).
        //    Il vetro non passa di qui: l'ottimizzatore guarda solo i pezzi, le lastre proseguono
        //    verso il preventivo. La distinta unita resta comunque il documento d'officina.
        Distinta unita = new Distinta(tutti, tuttiVetri);
        PianoDiTaglio piano = ottimizzatore.ottimizza(unita, magazzino, barraStandard);

        // 3. Preventivo aggregato + magazzino aggiornato (avanzi consumati + ritagli sopra soglia).
        Preventivo preventivo = generatorePreventivo.genera(piano, tuttiVetri, soglia);
        List<Avanzo> aggiornato = aggiornaMagazzino(magazzino, piano, soglia);
        List<QuotaOrdine> quote = ripartisci(ordini, perOrdine, preventivo);
        List<DistintaOrdine> distinte = new ArrayList<>();
        for (int i = 0; i < ordini.size(); i++) {
            distinte.add(new DistintaOrdine(ordini.get(i).nome(), perOrdine.get(i)));
        }
        return new EvasioneOrdini(ordini, distinte, piano, preventivo, aggiornato, quote);
    }

    /**
     * Divide il preventivo globale tra gli ordini. Il criterio (e il perché) sta in
     * {@link QuotaOrdine}: per ogni materiale si spartiscono peso e costo in proporzione ai
     * millimetri di pezzi chiesti da ciascun ordine; il vetro invece è esatto, non si condivide.
     */
    private static List<QuotaOrdine> ripartisci(List<Ordine> ordini, List<Distinta> distinte,
            Preventivo preventivo) {

        // Millimetri complessivi per materiale: è il denominatore del riparto.
        Map<Materiale, Double> lunghezzaTotale = new LinkedHashMap<>();
        for (Distinta distinta : distinte) {
            lunghezzePerMateriale(distinta).forEach((materiale, mm) ->
                    lunghezzaTotale.merge(materiale, mm, Double::sum));
        }
        // Peso e costo che il preventivo globale attribuisce a ogni materiale.
        Map<Materiale, RigaProfilo> righe = new LinkedHashMap<>();
        for (RigaProfilo riga : preventivo.righe()) {
            righe.put(new Materiale(riga.profilo(), riga.colore()), riga);
        }

        List<QuotaOrdine> quote = new ArrayList<>();
        for (int i = 0; i < ordini.size(); i++) {
            Distinta distinta = distinte.get(i);
            String nome = ordini.get(i).nome();
            if (distinta.pezzi().isEmpty() && distinta.vetri().isEmpty()) {
                quote.add(QuotaOrdine.vuota(nome));
                continue;
            }
            double lunghezza = 0;
            double peso = 0;
            double costo = 0;
            List<QuotaOrdine.Riga> dettaglio = new ArrayList<>();
            for (Map.Entry<Materiale, Double> voce : lunghezzePerMateriale(distinta).entrySet()) {
                Materiale materiale = voce.getKey();
                RigaProfilo riga = righe.get(materiale);
                double totale = lunghezzaTotale.getOrDefault(materiale, 0.0);
                double quota = riga == null || totale <= 0 ? 0 : voce.getValue() / totale;
                double pesoRiga = riga == null ? 0 : riga.peso() * quota;
                double costoRiga = riga == null ? 0 : riga.costo() * quota;
                lunghezza += voce.getValue();
                peso += pesoRiga;
                costo += costoRiga;
                dettaglio.add(new QuotaOrdine.Riga(materiale.profilo(), materiale.colore(),
                        voce.getValue(), pesoRiga, costoRiga));
            }
            // Il vetro non si divide: le lastre sono dell'ordine, col prezzo del loro serramento.
            double costoVetro = distinta.vetri().stream().mapToDouble(Vetro::costo).sum();
            quote.add(new QuotaOrdine(nome, lunghezza, peso, costo,
                    distinta.totaleLastre(), distinta.areaVetroTotaleMq(), costoVetro, dettaglio));
        }
        return quote;
    }

    /** Millimetri di pezzi per materiale in una distinta. */
    private static Map<Materiale, Double> lunghezzePerMateriale(Distinta distinta) {
        Map<Materiale, Double> lunghezze = new LinkedHashMap<>();
        for (Pezzo pezzo : distinta.pezzi()) {
            lunghezze.merge(pezzo.materiale(), pezzo.lunghezza(), Double::sum);
        }
        return lunghezze;
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
                accumula(unione, barra.profilo(), barra.colore(), arrotonda(barra.sfrido()), 1);
            }
        }
        return new ArrayList<>(unione.values());
    }

    /**
     * Il ritaglio al decimo di millimetro. Gli avanzi si fondono per <b>lunghezza esatta</b>, e uno
     * sfrido è una somma di float: due residui identici in officina possono differire di 1e-13 e
     * diventare due righe di magazzino invece di una {@code x2}. Arrotondare qui li riunisce — e
     * comunque nessuno rimette in rastrelliera uno spezzone quotato al nanometro.
     */
    private static double arrotonda(double mm) {
        return Math.round(mm * 10.0) / 10.0;
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
