package com.cutcalculator.cli;

import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.catalogo.Sistema;
import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Dimensione;
import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Pezzo;
import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.dominio.RegolaTaglio;
import com.cutcalculator.dominio.Serramento;
import com.cutcalculator.dominio.Tipologia;
import com.cutcalculator.dominio.TipoTaglio;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.ottimizzatore.BarraTagliata;
import com.cutcalculator.ottimizzatore.Ottimizzatore;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.pianificazione.EvasioneOrdini;
import com.cutcalculator.preventivo.Preventivo;
import com.cutcalculator.preventivo.RigaProfilo;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Realizzazione testuale di {@link View}: tutto l'I/O da riga di comando — legge gli input,
 * mostra i menu e stampa i risultati. Lo <b>stato</b> non vive qui: sta nel {@link Controller},
 * a cui la view chiede le letture e a cui delega le modifiche.
 * <p>
 * Due sezioni (le stesse che avrà la GUI): <b>Magazzino</b> (avanzi: aggiungi/mostra/rimuovi) e
 * <b>Ordini</b> (nuovo/mostra/apri/rimuovi; aprendo un ordine se ne gestiscono i serramenti e lo
 * si calcola). Input robusto (re-prompt sugli errori), misure con virgola o punto, EOF = uscita
 * pulita (quindi pilotabile via pipe). Output volutamente <b>solo ASCII</b> (niente
 * {@code •}/{@code ×}/{@code ·}): il terminale di Windows non è cp1252 e i caratteri fuori range
 * uscirebbero come {@code �}.
 */
public final class CliView implements View {

    private final Scanner in;
    private Controller controller;

    public CliView(Scanner in) {
        this.in = in;
    }

    // --- Ciclo principale --------------------------------------------------------------

    @Override
    public void avvia(Controller controller) {
        this.controller = controller;
        sezione("CutCalculator - client testuale");
        catalogo(controller.catalogo());
        try {
            while (menuPrincipale()) {
                // continua finche' l'utente non sceglie Esci
            }
            System.out.println("Chiusura. Arrivederci.");
        } catch (NoSuchElementException fineInput) {
            System.out.println("\n(fine input) Chiusura.");
        }
    }

    /** @return {@code false} se l'utente ha scelto di uscire. */
    private boolean menuPrincipale() {
        System.out.println();
        System.out.println("=== MENU PRINCIPALE ===");
        System.out.println("  1) Magazzino  (" + conta(controller.totaleAvanzi(), "pezzo", "pezzi") + ")");
        System.out.println("  2) Ordini     (" + conta(controller.ordini().size(), "ordine", "ordini") + ")");
        System.out.println("  0) Esci");
        switch (leggiIntero("Scelta", 0, 2)) {
            case 1 -> menuMagazzino();
            case 2 -> menuOrdini();
            case 0 -> {
                return false;
            }
        }
        return true;
    }

    // --- Sezione MAGAZZINO -------------------------------------------------------------

    private void menuMagazzino() {
        boolean resta = true;
        while (resta) {
            System.out.println();
            System.out.println("=== MAGAZZINO (" + conta(controller.totaleAvanzi(), "pezzo", "pezzi") + ") ===");
            System.out.println("  1) Aggiungi pezzo");
            System.out.println("  2) Mostra magazzino");
            System.out.println("  3) Rimuovi pezzo");
            System.out.println("  4) Svuota magazzino");
            System.out.println("  0) Indietro");
            switch (leggiIntero("Scelta", 0, 4)) {
                case 1 -> aggiungiAvanzo();
                case 2 -> mostraMagazzino();
                case 3 -> rimuoviAvanzo();
                case 4 -> svuotaMagazzino();
                case 0 -> resta = false;
            }
        }
    }

    private void aggiungiAvanzo() {
        Sistema sistema = scegliSistema();
        Profilo profilo = scegliDaLista(
                "Profili di " + sistema.nome() + ":", profiliDi(sistema), this::etichetta);
        Colore colore = leggiColore("Colore");
        double lunghezza = leggiMisura("Lunghezza pezzo");
        int quantita = leggiQuantita("Quantita'");
        controller.aggiungiAvanzo(new Avanzo(profilo, colore, lunghezza, quantita));
        System.out.printf("  + pezzo %s [%s]  %s mm  x%d%n",
                etichetta(profilo), colore.nome(), num(lunghezza), quantita);
    }

    private void mostraMagazzino() {
        if (controller.magazzino().isEmpty()) {
            System.out.println("  Magazzino vuoto: aggiungi un pezzo (opzione 1).");
            return;
        }
        magazzino(controller.magazzino());
    }

    private void rimuoviAvanzo() {
        List<Avanzo> avanzi = controller.magazzino();
        if (avanzi.isEmpty()) {
            System.out.println("  Magazzino vuoto: niente da rimuovere.");
            return;
        }
        magazzino(avanzi);
        int scelta = leggiIntero("Quale rimuovere (0 = annulla)", 0, avanzi.size());
        if (scelta == 0) {
            return;
        }
        Avanzo avanzo = avanzi.get(scelta - 1);
        int quantita = avanzo.quantita() == 1
                ? 1
                : leggiIntero("Quanti toglierne", 1, avanzo.quantita());
        Avanzo rimasto = controller.rimuoviAvanzo(scelta - 1, quantita);
        if (rimasto == null) {
            System.out.printf("  Rimosso: %s [%s]  %s mm x%d%n",
                    etichetta(avanzo.profilo()), avanzo.colore().nome(), num(avanzo.lunghezza()), quantita);
        } else {
            System.out.printf("  Tolti x%d: %s [%s]  %s mm  (restano x%d)%n", quantita,
                    etichetta(rimasto.profilo()), rimasto.colore().nome(), num(rimasto.lunghezza()), rimasto.quantita());
        }
    }

    /** Svuota l'intero magazzino, previa conferma (l'operazione riscrive il file su disco). */
    private void svuotaMagazzino() {
        int n = controller.totaleAvanzi();
        if (n == 0) {
            System.out.println("  Magazzino gia' vuoto: niente da svuotare.");
            return;
        }
        if (!prompt("  Svuotare tutto il magazzino (" + conta(n, "pezzo", "pezzi") + ")? [s/N]> ")
                .trim().equalsIgnoreCase("s")) {
            System.out.println("  Annullato: magazzino invariato.");
            return;
        }
        int rimossi = controller.svuotaMagazzino();
        System.out.printf("  Magazzino svuotato: rimossi %s.%n", conta(rimossi, "pezzo", "pezzi"));
    }

    // --- Sezione ORDINI ----------------------------------------------------------------

    private void menuOrdini() {
        boolean resta = true;
        while (resta) {
            System.out.println();
            System.out.println("=== ORDINI (" + conta(controller.ordini().size(), "ordine", "ordini") + ") ===");
            System.out.println("  1) Nuovo ordine");
            System.out.println("  2) Mostra ordini");
            System.out.println("  3) Apri ordine");
            System.out.println("  4) Rimuovi ordine");
            System.out.println("  5) Calcola tutti gli ordini (scarico magazzino)");
            System.out.println("  0) Indietro");
            switch (leggiIntero("Scelta", 0, 5)) {
                case 1 -> nuovoOrdine();
                case 2 -> mostraOrdini();
                case 3 -> apriOrdine();
                case 4 -> rimuoviOrdine();
                case 5 -> evadiOrdini();
                case 0 -> resta = false;
            }
        }
    }

    private void nuovoOrdine() {
        String nome = prompt("Nome del nuovo ordine> ").trim();
        int prossimo = controller.ordini().size() + 1;
        Ordine ordine = controller.nuovoOrdine(nome.isBlank() ? "Ordine " + prossimo : nome);
        System.out.println("  Creato: " + ordine.nome());
        gestisciOrdine(ordine);
    }

    private void mostraOrdini() {
        List<Ordine> ordini = controller.ordini();
        if (ordini.isEmpty()) {
            System.out.println("  Nessun ordine: creane uno (opzione 1).");
            return;
        }
        System.out.println("Ordini:");
        for (int i = 0; i < ordini.size(); i++) {
            Ordine o = ordini.get(i);
            System.out.printf("  %d) %-30s (%s)%n", i + 1, o.nome(),
                    conta(o.totaleSerramenti(), "serramento", "serramenti"));
        }
    }

    private void apriOrdine() {
        Ordine ordine = scegliOrdine("Quale ordine aprire");
        if (ordine != null) {
            gestisciOrdine(ordine);
        }
    }

    private void rimuoviOrdine() {
        Ordine ordine = scegliOrdine("Quale ordine rimuovere");
        if (ordine != null) {
            controller.rimuoviOrdine(ordine);
            System.out.println("  Rimosso: " + ordine.nome());
        }
    }

    /** Mostra gli ordini e ne fa scegliere uno; {@code null} = nessuno / annullato. */
    private Ordine scegliOrdine(String prompt) {
        List<Ordine> ordini = controller.ordini();
        if (ordini.isEmpty()) {
            System.out.println("  Nessun ordine: creane uno (opzione 1).");
            return null;
        }
        mostraOrdini();
        int scelta = leggiIntero(prompt + " (0 = annulla)", 0, ordini.size());
        return scelta == 0 ? null : ordini.get(scelta - 1);
    }

    /**
     * Calcolo globale su tutti gli ordini: chiede conferma (è distruttivo per il magazzino), poi
     * delega al controller che pianifica, <b>scarica</b> gli avanzi usati, fa rientrare i ritagli
     * sopra soglia e salva. Mostra i piani per ordine e il preventivo totale.
     */
    private void evadiOrdini() {
        boolean qualcosaDaFare = controller.ordini().stream().anyMatch(o -> !o.serramenti().isEmpty());
        if (!qualcosaDaFare) {
            System.out.println("  Nessun ordine con serramenti da calcolare.");
            return;
        }
        System.out.printf("  Il calcolo globale scarica il magazzino: gli avanzi usati vengono consumati e"
                + " i ritagli >= %s mm rientrano. L'operazione salva su disco.%n", num(controller.sogliaRitaglio()));
        if (!prompt("  Confermi? [s/N]> ").trim().equalsIgnoreCase("s")) {
            System.out.println("  Annullato: magazzino invariato.");
            return;
        }
        try {
            evasione(controller.evadiOrdini());
            System.out.printf("%n  Magazzino aggiornato e salvato (%s).%n",
                    conta(controller.totaleAvanzi(), "pezzo", "pezzi"));
        } catch (IllegalArgumentException pezzoTroppoLungo) {
            System.out.println("  Impossibile calcolare: " + pezzoTroppoLungo.getMessage());
        }
    }

    // --- Gestione di un singolo ordine -------------------------------------------------

    private void gestisciOrdine(Ordine ordine) {
        boolean resta = true;
        while (resta) {
            System.out.println();
            System.out.println("=== ORDINE: " + ordine.nome() + "  ("
                    + conta(ordine.totaleSerramenti(), "serramento", "serramenti") + ") ===");
            System.out.println("  1) Aggiungi serramento");
            System.out.println("  2) Mostra serramenti");
            System.out.println("  3) Rimuovi serramento");
            System.out.println("  4) Calcola (distinta + piano + preventivo)");
            System.out.println("  5) Rinomina ordine");
            System.out.println("  0) Indietro");
            switch (leggiIntero("Scelta", 0, 5)) {
                case 1 -> aggiungiSerramento(ordine);
                case 2 -> ordine(ordine);
                case 3 -> rimuoviSerramento(ordine);
                case 4 -> calcola(ordine);
                case 5 -> rinominaOrdine(ordine);
                case 0 -> resta = false;
            }
        }
    }

    private void aggiungiSerramento(Ordine ordine) {
        Sistema sistema = scegliSistema();
        Tipologia tipologia = scegliDaLista(
                "Tipologie di " + sistema.nome() + ":", sistema.tipologie(), Tipologia::nome);
        Colore colore = leggiColore("Colore");
        double l = leggiMisura("Larghezza L");
        double h = leggiMisura("Altezza H");
        int quantita = leggiQuantita("Quantita'");
        ordine.aggiungi(new Serramento(tipologia, colore, l, h, quantita));
        System.out.printf("  + %s / %s [%s]  %s x %s mm  x%d%n",
                sistema.nome(), tipologia.nome(), colore.nome(), num(l), num(h), quantita);
    }

    private void rimuoviSerramento(Ordine ordine) {
        if (ordine.serramenti().isEmpty()) {
            System.out.println("  Ordine vuoto: niente da rimuovere.");
            return;
        }
        ordine(ordine);
        int scelta = leggiIntero("Quale rimuovere (0 = annulla)", 0, ordine.serramenti().size());
        if (scelta == 0) {
            return;
        }
        ordine.rimuovi(scelta - 1);
        System.out.println("  Rimosso.");
    }

    private void rinominaOrdine(Ordine ordine) {
        String nuovo = prompt("Nuovo nome (vuoto = invariato)> ").trim();
        if (nuovo.isBlank()) {
            System.out.println("  Nome invariato: " + ordine.nome());
            return;
        }
        ordine.rinomina(nuovo);
        System.out.println("  Rinominato: " + ordine.nome());
    }

    private void calcola(Ordine ordine) {
        if (ordine.serramenti().isEmpty()) {
            System.out.println("  Ordine vuoto: aggiungi almeno un serramento (opzione 1).");
            return;
        }
        try {
            Controller.Risultato risultato = controller.calcola(ordine);
            distinta(risultato.distinta());
            piano(risultato.piano());
            sfridi(risultato.piano());
            preventivo(risultato.preventivo());
        } catch (IllegalArgumentException pezzoTroppoLungo) {
            // Es.: un pezzo piu' lungo della barra standard da 6500 mm.
            System.out.println("  Impossibile ottimizzare: " + pezzoTroppoLungo.getMessage());
        }
    }

    // --- Scelte dal catalogo -----------------------------------------------------------

    private Sistema scegliSistema() {
        return scegliDaLista("Sistemi disponibili:", controller.catalogo().sistemi(),
                s -> s.nome() + " (" + s.famiglia() + ")");
    }

    /** I profili distinti di un sistema, presi dalle regole di tutte le sue tipologie. */
    private static List<Profilo> profiliDi(Sistema sistema) {
        return sistema.tipologie().stream()
                .flatMap(t -> t.regole().stream())
                .map(RegolaTaglio::profilo)
                .distinct()
                .toList();
    }

    private <T> T scegliDaLista(String titolo, List<T> opzioni, Function<T, String> etichetta) {
        System.out.println(titolo);
        for (int i = 0; i < opzioni.size(); i++) {
            System.out.printf("  %d) %s%n", i + 1, etichetta.apply(opzioni.get(i)));
        }
        return opzioni.get(leggiIntero("Scelta", 1, opzioni.size()) - 1);
    }

    // --- Lettura input robusta ---------------------------------------------------------

    private int leggiIntero(String etichetta, int min, int max) {
        while (true) {
            String riga = prompt(etichetta + " [" + min + "-" + max + "]> ").trim();
            try {
                int valore = Integer.parseInt(riga);
                if (valore >= min && valore <= max) {
                    return valore;
                }
            } catch (NumberFormatException ignored) {
                // ricade nel messaggio sotto
            }
            System.out.println("  Inserisci un numero tra " + min + " e " + max + ".");
        }
    }

    private int leggiQuantita(String etichetta) {
        while (true) {
            String riga = prompt(etichetta + "> ").trim();
            try {
                int valore = Integer.parseInt(riga);
                if (valore > 0) {
                    return valore;
                }
            } catch (NumberFormatException ignored) {
                // ricade nel messaggio sotto
            }
            System.out.println("  Inserisci un intero positivo.");
        }
    }

    /** Legge un colore come testo libero (nome commerciale o codice RAL); non può essere vuoto. */
    private Colore leggiColore(String etichetta) {
        while (true) {
            String riga = prompt(etichetta + "> ").trim();
            if (!riga.isBlank()) {
                return new Colore(riga);
            }
            System.out.println("  Inserisci un colore (es. bianco, bronzo, RAL9010).");
        }
    }

    /** Legge una misura in mm; accetta sia la virgola sia il punto come separatore decimale. */
    private double leggiMisura(String etichetta) {
        while (true) {
            String riga = prompt(etichetta + " (mm)> ").trim().replace(',', '.');
            try {
                double valore = Double.parseDouble(riga);
                if (valore > 0) {
                    return valore;
                }
            } catch (NumberFormatException ignored) {
                // ricade nel messaggio sotto
            }
            System.out.println("  Inserisci una misura positiva in mm (es. 1500 o 1476,5).");
        }
    }

    /** Stampa il prompt e legge una riga; {@link NoSuchElementException} a fine input = uscita. */
    private String prompt(String testo) {
        System.out.print(testo);
        String riga = in.nextLine();
        // Toglie un eventuale BOM (U+FEFF) in testa quando l'input arriva da file/pipe con BOM.
        return riga.isEmpty() || riga.charAt(0) != 0xFEFF ? riga : riga.substring(1);
    }

    /** Conta con il numero e la forma giusta: {@code 1 serramento} / {@code 3 serramenti}. */
    private static String conta(int n, String singolare, String plurale) {
        return n + " " + (n == 1 ? singolare : plurale);
    }

    // --- Rendering (stampa dei risultati) ----------------------------------------------

    @Override
    public void catalogo(Catalogo catalogo) {
        sezione("CATALOGO (" + catalogo.sistemi().size() + " sistemi)");
        catalogo.perFamiglia().forEach((famiglia, sistemi) -> {
            System.out.println(famiglia + ":");
            for (Sistema s : sistemi) {
                String tipologie = s.tipologie().stream()
                        .map(Tipologia::nome).collect(Collectors.joining(", "));
                System.out.printf("   %s %-8s  %s%n", "-", s.nome(), tipologie);
            }
        });
    }

    /** I serramenti di un ordine, numerati da 1 (l'indice serve anche per rimuoverli). */
    @Override
    public void ordine(Ordine ordine) {
        sezione("ORDINE: " + ordine.nome());
        if (ordine.serramenti().isEmpty()) {
            System.out.println("(nessun serramento)");
            return;
        }
        int i = 1;
        for (Serramento s : ordine.serramenti()) {
            Dimensione d = s.dimensione();
            System.out.printf(" %2d. %-32s %-10s %6s x %-6s mm   x%d%n",
                    i++, s.tipologia().nome(), s.colore().nome(), num(d.L()), num(d.H()), s.quantita());
        }
    }

    /** Gli avanzi a magazzino, numerati da 1 (l'indice serve anche per rimuoverli). */
    @Override
    public void magazzino(List<Avanzo> avanzi) {
        sezione("MAGAZZINO");
        if (avanzi.isEmpty()) {
            System.out.println("(vuoto)");
            return;
        }
        int i = 1;
        for (Avanzo a : avanzi) {
            System.out.printf(" %2d. %-30s %-10s %6s mm x%d%n",
                    i++, etichetta(a.profilo()), a.colore().nome(), num(a.lunghezza()), a.quantita());
        }
    }

    @Override
    public void distinta(Distinta distinta) {
        sezione("DISTINTA DI TAGLIO");
        System.out.printf("Totale pezzi da tagliare: %d%n%n", distinta.totalePezzi());
        distinta.perMateriale().forEach((materiale, pezzi) -> {
            System.out.printf("%s %-34s (%s): %d pezzi%n",
                    "-", etichetta(materiale), materiale.profilo().categoria(), pezzi.size());
            System.out.printf("     %s%n", riepilogoPezzi(pezzi));
        });
    }

    @Override
    public void piano(PianoDiTaglio piano) {
        sezione("PIANO DI TAGLIO");
        int avanziUsati = piano.numeroBarre() - piano.barreNuove();
        System.out.printf("Barra standard: %s mm  |  kerf 4 mm/taglio%n",
                num(Ottimizzatore.BARRA_STANDARD_DEFAULT));
        System.out.printf("Barre totali: %d  (%d nuove, %d avanzi)  |  media geom. sfrido %s mm%n%n",
                piano.numeroBarre(), piano.barreNuove(), avanziUsati, num(piano.mediaGeometricaSfrido()));

        piano.perMateriale().forEach((materiale, barre) -> {
            long avanzi = barre.stream().filter(BarraTagliata::avanzo).count();
            System.out.printf("%s %-34s %d barre (%d nuove, %d avanzi)%n",
                    "-", etichetta(materiale), barre.size(), barre.size() - avanzi, avanzi);
            int i = 1;
            for (BarraTagliata barra : barre) {
                String tipo = (barra.avanzo() ? "AVANZO " : "NUOVA ") + num(barra.lunghezzaBarra()) + " mm";
                System.out.printf("    #%-2d [%-14s]  occupato %6s mm  |  sfrido %6s mm%n",
                        i++, tipo, num(barra.occupato()), num(barra.sfrido()));
                System.out.printf("        -> %s%n", riepilogoPezzi(barra.pezzi()));
            }
            System.out.println();
        });
    }

    /**
     * Lo sfrido di <b>ogni</b> barra, ordinato dal maggiore al minore: una vista "a colpo d'occhio"
     * delle barre più sprecone (nel piano di taglio lo sfrido c'è già, ma intercalato ai pezzi).
     */
    @Override
    public void sfridi(PianoDiTaglio piano) {
        sezione("SFRIDO PER BARRA (dal maggiore al minore)");
        String riga = "%-40s %-8s %10s%n";
        System.out.printf(riga, "Profilo / colore", "Barra", "Sfrido");
        System.out.println("-".repeat(60));
        piano.barre().stream()
                .sorted(Comparator.comparingDouble(BarraTagliata::sfrido).reversed())
                .forEach(barra -> System.out.printf(riga, etichetta(barra.materiale()),
                        barra.avanzo() ? "avanzo" : "nuova", num(barra.sfrido()) + " mm"));
    }

    @Override
    public void preventivo(Preventivo preventivo) {
        sezione("PREVENTIVO (materiale profili)");
        String riga = "%-30s %-10s %11s %8s %14s %12s%n";
        System.out.printf(riga, "Profilo", "Colore", "Barre nuove", "Avanzi", "Lungh. nuova", "Sfrido");
        System.out.println("-".repeat(90));
        for (RigaProfilo r : preventivo.righe()) {
            System.out.printf(riga, etichetta(r.profilo()), r.colore().nome(), r.barreNuove(),
                    r.avanziUsati(), num(r.lunghezzaNuova()) + " mm", num(r.sfrido()) + " mm");
        }
        System.out.println("-".repeat(90));
        System.out.printf(riga, "TOTALE", "", preventivo.totaleBarreNuove(), preventivo.totaleAvanziUsati(),
                num(preventivo.lunghezzaNuovaTotale()) + " mm", num(preventivo.sfridoTotale()) + " mm");

        System.out.printf("%nMateriale d'acquisto: %d barre da %s mm  =  %s m lineari.%n",
                preventivo.totaleBarreNuove(), num(Ottimizzatore.BARRA_STANDARD_DEFAULT),
                num(preventivo.lunghezzaNuovaTotale() / 1000));
    }

    /**
     * Il calcolo globale: per ogni ordine il suo piano di taglio e i suoi sfridi, poi il
     * <b>preventivo totale</b> (il materiale per tutti gli ordini insieme). Lo scarico del
     * magazzino lo committa e lo conferma il chiamante, non questo render.
     */
    @Override
    public void evasione(EvasioneOrdini evasione) {
        sezione("CALCOLO GLOBALE - " + conta(evasione.perOrdine().size(), "ordine", "ordini"));
        for (EvasioneOrdini.RisultatoOrdine risultato : evasione.perOrdine()) {
            System.out.println();
            System.out.println(">>> ORDINE: " + risultato.ordine().nome());
            if (risultato.piano().numeroBarre() == 0) {
                System.out.println("    (nessun pezzo da tagliare)");
                continue;
            }
            piano(risultato.piano());
            sfridi(risultato.piano());
        }
        preventivo(evasione.preventivoTotale());
    }

    // --- Formatter e helper di stampa --------------------------------------------------

    private void sezione(String titolo) {
        String linea = "=".repeat(74);
        System.out.println();
        System.out.println(linea);
        System.out.println("  " + titolo);
        System.out.println(linea);
    }

    /** Etichetta breve di un profilo: {@code "RX70.101 Telaio"}. */
    private String etichetta(Profilo p) {
        return p.codice() + " " + p.descrizione();
    }

    /** Etichetta di un materiale: profilo + colore, {@code "RX70.101 Telaio [BIANCO]"}. */
    private String etichetta(Materiale m) {
        return etichetta(m.profilo()) + " [" + m.colore().nome() + "]";
    }

    /** Numero compatto: senza decimali se intero, altrimenti con una cifra (mm con mezzo taglio). */
    private String num(double v) {
        return Math.rint(v) == v
                ? String.format(Locale.ROOT, "%.0f", v)
                : String.format(Locale.ROOT, "%.1f", v);
    }

    /**
     * Pezzi raggruppati per lunghezza e tipo di taglio, dal piu' lungo al piu' corto:
     * "2100 x1 [90/45 DX]  |  2100 x1 [90/45 SX]  |  2000 x1 [45/45]".
     */
    private String riepilogoPezzi(List<Pezzo> pezzi) {
        record Chiave(double lunghezza, TipoTaglio taglio) {
        }
        Map<Chiave, Long> conteggio = pezzi.stream()
                .collect(Collectors.groupingBy(p -> new Chiave(p.lunghezza(), p.tipoTaglio()),
                        Collectors.counting()));
        return conteggio.entrySet().stream()
                .sorted(Comparator
                        .comparingDouble((Map.Entry<Chiave, Long> e) -> e.getKey().lunghezza()).reversed()
                        .thenComparing(e -> e.getKey().taglio()))
                .map(e -> num(e.getKey().lunghezza()) + " x" + e.getValue()
                        + " [" + etichettaTaglio(e.getKey().taglio()) + "]")
                .collect(Collectors.joining("  |  "));
    }

    /** Etichetta compatta dei due tagli alle estremita' di un pezzo. */
    private static String etichettaTaglio(TipoTaglio taglio) {
        return switch (taglio) {
            case TAGLIO_45_45 -> "45/45";
            case TAGLIO_90_90 -> "90/90";
            case TAGLIO_90_45_DX -> "90/45 DX";
            case TAGLIO_90_45_SX -> "90/45 SX";
        };
    }
}
