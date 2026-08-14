package com.cutcalculator.cli;

import com.cutcalculator.app.Controller;
import com.cutcalculator.app.Etichette;
import com.cutcalculator.app.Unita;
import com.cutcalculator.app.View;
import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.catalogo.Sistema;
import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Dimensione;
import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Pezzo;
import com.cutcalculator.dominio.Prezzi;
import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.dominio.Serramento;
import com.cutcalculator.dominio.Tipologia;
import com.cutcalculator.dominio.TipoTaglio;
import com.cutcalculator.dominio.Vetro;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.ottimizzatore.BarraTagliata;
import com.cutcalculator.ottimizzatore.Ottimizzatore;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.pianificazione.EvasioneOrdini;
import com.cutcalculator.preventivo.Preventivo;
import com.cutcalculator.preventivo.RigaProfilo;
import com.cutcalculator.preventivo.RigaVetro;

import java.util.Comparator;
import java.util.List;
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
        System.out.println("  3) Unita' di misura  (" + controller.unita().descrizione() + ")");
        System.out.println("  4) Prezzi del materiale  (" + descrizionePrezzi() + ")");
        System.out.println("  0) Esci");
        switch (leggiIntero("Scelta", 0, 4)) {
            case 1 -> menuMagazzino();
            case 2 -> menuOrdini();
            case 3 -> scegliUnita();
            case 4 -> impostaPrezzi();
            case 0 -> {
                return false;
            }
        }
        return true;
    }

    /**
     * Cambia l'unita' con cui misure e sfridi vengono mostrati <b>e</b> letti. I dati non si
     * toccano: il modello resta in millimetri, cambia solo la lente.
     */
    private void scegliUnita() {
        Unita scelta = scegliDaLista("Unita' di misura:", List.of(Unita.values()), Unita::descrizione);
        controller.impostaUnita(scelta);
        System.out.println("  Misure in " + scelta.descrizione() + ".");
    }

    /**
     * Il listino con cui valorizzare il preventivo: l'alluminio si paga a peso, il vetro a
     * superficie. Sono dati dell'utente (variano col fornitore), quindi si inseriscono a mano; a
     * zero valgono "non impostato" e i costi restano zero invece di uscire sbagliati.
     */
    private void impostaPrezzi() {
        Prezzi attuali = controller.prezzi();
        System.out.println();
        System.out.println("Prezzi del materiale (0 = non impostato, lascia vuoto per non cambiare).");
        double barre = leggiPrezzo("Prezzo alluminio (EUR/kg)", attuali.alChiloBarre());
        double vetro = leggiPrezzo("Prezzo vetro (EUR/mq)", attuali.alMqVetro());
        controller.impostaPrezzi(new Prezzi(barre, vetro));
        System.out.println("  " + descrizionePrezzi() + ".");
    }

    /** Il listino in una riga, per il menu. */
    private String descrizionePrezzi() {
        Prezzi prezzi = controller.prezzi();
        if (!prezzi.impostati()) {
            return "non impostati";
        }
        return euro(prezzi.alChiloBarre()) + "/kg, " + euro(prezzi.alMqVetro()) + "/mq";
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
                "Profili di " + sistema.nome() + ":", sistema.profili(), this::etichetta);
        Colore colore = leggiColore("Colore");
        double lunghezza = leggiMisura("Lunghezza pezzo");
        int quantita = leggiQuantita("Quantita'");
        controller.aggiungiAvanzo(new Avanzo(profilo, colore, lunghezza, quantita));
        System.out.printf("  + pezzo %s [%s]  %s  x%d%n",
                etichetta(profilo), colore.nome(), mis(lunghezza), quantita);
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
            System.out.printf("  Rimosso: %s [%s]  %s x%d%n",
                    etichetta(avanzo.profilo()), avanzo.colore().nome(), mis(avanzo.lunghezza()), quantita);
        } else {
            System.out.printf("  Tolti x%d: %s [%s]  %s  (restano x%d)%n", quantita,
                    etichetta(rimasto.profilo()), rimasto.colore().nome(), mis(rimasto.lunghezza()), rimasto.quantita());
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
            System.out.printf("=== ORDINI (%s: %d da calcolare, %d calcolati) ===%n",
                    conta(controller.ordini().size(), "ordine", "ordini"),
                    controller.ordiniDaCalcolare().size(), controller.ordiniCalcolati().size());
            System.out.println("  1) Nuovo ordine");
            System.out.println("  2) Mostra ordini");
            System.out.println("  3) Apri ordine");
            System.out.println("  4) Rimuovi ordine");
            System.out.println("  5) Calcola gli ordini da calcolare (scarico magazzino)");
            System.out.println("  6) Salva ordini su file");
            System.out.println("  7) Carica ordini da file");
            System.out.println("  0) Indietro");
            switch (leggiIntero("Scelta", 0, 7)) {
                case 1 -> nuovoOrdine();
                case 2 -> mostraOrdini();
                case 3 -> apriOrdine();
                case 4 -> rimuoviOrdine();
                case 5 -> evadiOrdini();
                case 6 -> salvaOrdiniSuFile();
                case 7 -> caricaOrdiniDaFile();
                case 0 -> resta = false;
            }
        }
    }

    private void nuovoOrdine() {
        String nome = leggiNomeOrdine("Nome del nuovo ordine> ");
        int prossimo = controller.ordini().size() + 1;
        Ordine ordine = controller.nuovoOrdine(nome.isBlank() ? "Ordine " + prossimo : nome);
        System.out.println("  Creato: " + ordine.nome());
        gestisciOrdine(ordine);
    }

    /**
     * Gli ordini divisi in <b>da calcolare</b> e <b>calcolati</b>. I numeri restano quelli della
     * lista unica: le due sezioni sono solo un modo di leggerla, non due elenchi diversi, cosi'
     * "apri" e "rimuovi" continuano a funzionare con lo stesso numero che si vede qui.
     */
    private void mostraOrdini() {
        List<Ordine> ordini = controller.ordini();
        if (ordini.isEmpty()) {
            System.out.println("  Nessun ordine: creane uno (opzione 1).");
            return;
        }
        elencaOrdini("DA CALCOLARE", ordini, false);
        elencaOrdini("CALCOLATI", ordini, true);
    }

    /** Una delle due sezioni; niente intestazione se non c'e' nessun ordine in quello stato. */
    private void elencaOrdini(String titolo, List<Ordine> ordini, boolean calcolati) {
        if (ordini.stream().noneMatch(o -> o.calcolato() == calcolati)) {
            return;
        }
        System.out.println(titolo + ":");
        for (int i = 0; i < ordini.size(); i++) {
            Ordine o = ordini.get(i);
            if (o.calcolato() == calcolati) {
                System.out.printf("  %d) %-30s (%s)%n", i + 1, o.nome(),
                        conta(o.totaleSerramenti(), "serramento", "serramenti"));
            }
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
     * Calcolo globale: prende gli ordini <b>da calcolare</b> (quelli gia' calcolati restano fuori,
     * il loro materiale e' gia' stato scalato), chiede conferma perche' e' distruttivo per il
     * magazzino, poi delega al controller che pianifica, <b>scarica</b> gli avanzi usati, fa
     * rientrare i ritagli sopra soglia, salva e segna gli ordini come calcolati.
     */
    private void evadiOrdini() {
        List<Ordine> daCalcolare = controller.ordiniDaCalcolare();
        if (daCalcolare.stream().allMatch(o -> o.serramenti().isEmpty())) {
            System.out.println(daCalcolare.isEmpty()
                    ? "  Nessun ordine da calcolare: sono gia' tutti calcolati."
                    : "  Gli ordini da calcolare sono vuoti: aggiungi almeno un serramento.");
            return;
        }
        System.out.println("  Verranno calcolati insieme:");
        daCalcolare.forEach(o -> System.out.printf("    - %-30s (%s)%n", o.nome(),
                conta(o.totaleSerramenti(), "serramento", "serramenti")));
        System.out.printf("  Il calcolo scarica il magazzino: gli avanzi usati vengono consumati e"
                + " i ritagli >= %s rientrano. L'operazione salva su disco.%n", mis(controller.sogliaRitaglio()));
        if (!prompt("  Confermi? [s/N]> ").trim().equalsIgnoreCase("s")) {
            System.out.println("  Annullato: magazzino invariato.");
            return;
        }
        try {
            evasione(controller.evadiOrdini());
            System.out.printf("%n  Magazzino aggiornato e salvato (%s).%n",
                    conta(controller.totaleAvanzi(), "pezzo", "pezzi"));
            System.out.printf("  %s ora tra i calcolati.%n",
                    conta(daCalcolare.size(), "ordine passato", "ordini passati"));
        } catch (IllegalArgumentException pezzoTroppoLungo) {
            System.out.println("  Impossibile calcolare: " + pezzoTroppoLungo.getMessage());
        }
    }

    /** Salva su disco tutti gli ordini correnti (comando esplicito: nessun autosave). */
    private void salvaOrdiniSuFile() {
        if (!controller.persistenzaOrdiniAttiva()) {
            System.out.println("  Persistenza ordini non disponibile.");
            return;
        }
        controller.salvaOrdini();
        System.out.printf("  Salvati su disco: %s.%n",
                conta(controller.ordini().size(), "ordine", "ordini"));
    }

    /** Ricarica gli ordini da disco, sostituendo quelli in memoria (previa conferma se ce ne sono). */
    private void caricaOrdiniDaFile() {
        if (!controller.persistenzaOrdiniAttiva()) {
            System.out.println("  Persistenza ordini non disponibile.");
            return;
        }
        int inMemoria = controller.ordini().size();
        if (inMemoria > 0 && !prompt("  Il caricamento sostituisce gli ordini in memoria ("
                + conta(inMemoria, "ordine", "ordini") + "). Confermi? [s/N]> ")
                .trim().equalsIgnoreCase("s")) {
            System.out.println("  Annullato: ordini invariati.");
            return;
        }
        int caricati = controller.caricaOrdini();
        System.out.printf("  Caricati da disco: %s.%n", conta(caricati, "ordine", "ordini"));
    }

    // --- Gestione di un singolo ordine -------------------------------------------------

    private void gestisciOrdine(Ordine ordine) {
        boolean resta = true;
        while (resta) {
            System.out.println();
            System.out.println("=== ORDINE: " + ordine.nome() + "  ("
                    + conta(ordine.totaleSerramenti(), "serramento", "serramenti") + ")  ["
                    + (ordine.calcolato() ? "calcolato" : "da calcolare") + "] ===");
            System.out.println("  1) Aggiungi serramento");
            System.out.println("  2) Mostra serramenti");
            System.out.println("  3) Rimuovi serramento");
            System.out.println("  4) Rinomina ordine");
            System.out.println("  0) Indietro");
            switch (leggiIntero("Scelta", 0, 4)) {
                case 1 -> aggiungiSerramento(ordine);
                case 2 -> ordine(ordine);
                case 3 -> rimuoviSerramento(ordine);
                case 4 -> rinominaOrdine(ordine);
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
        double hf = tipologia.usaHF() ? leggiMisura("Altezza parziale HF") : 0;
        int quantita = leggiQuantita("Quantita'");
        ordine.aggiungi(new Serramento(tipologia, colore, new Dimensione(l, h, hf), quantita));
        System.out.printf("  + %s / %s [%s]  %s x %s %s%s  x%d%n",
                sistema.nome(), tipologia.nome(), colore.nome(), num(l), num(h), simbolo(),
                hf > 0 ? " (HF " + mis(hf) + ")" : "", quantita);
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
        String nuovo = leggiNomeOrdine("Nuovo nome (vuoto = invariato)> ");
        if (nuovo.isBlank()) {
            System.out.println("  Nome invariato: " + ordine.nome());
            return;
        }
        ordine.rinomina(nuovo);
        System.out.println("  Rinominato: " + ordine.nome());
    }

    // Il "calcolo provvisorio" del singolo ordine (anteprima non distruttiva) e' stato tolto:
    // mostrava un piano che il calcolo globale poi rifaceva diverso, perche' li' gli ordini si
    // uniscono e i pezzi condividono le barre. Un solo calcolo, quello che conta.

    // --- Scelte dal catalogo -----------------------------------------------------------

    private Sistema scegliSistema() {
        return scegliDaLista("Sistemi disponibili:", controller.catalogo().sistemi(),
                s -> s.nome() + " (" + s.famiglia() + ")");
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

    /**
     * Legge il nome di un ordine (può essere vuoto: chi chiama decide il default) vietando il
     * {@code ';'}, che è il separatore del file degli ordini e ne spezzerebbe la riga.
     */
    private String leggiNomeOrdine(String etichetta) {
        while (true) {
            String riga = prompt(etichetta).trim();
            if (riga.indexOf(';') < 0) {
                return riga;
            }
            System.out.println("  Il nome non puo' contenere ';' (separatore del file).");
        }
    }

    /** Legge un colore come testo libero (nome commerciale o codice RAL); non può essere vuoto. */
    private Colore leggiColore(String etichetta) {
        while (true) {
            String riga = prompt(etichetta + "> ").trim();
            if (!riga.isBlank()) {
                try {
                    return new Colore(riga);
                } catch (IllegalArgumentException nonValido) {
                    System.out.println("  " + nonValido.getMessage());
                    continue;
                }
            }
            System.out.println("  Inserisci un colore (es. bianco, bronzo, RAL9010).");
        }
    }

    /**
     * Legge una misura nell'unita' scelta dall'utente e la restituisce <b>in mm</b> (il modello
     * lavora sempre in millimetri). Accetta sia la virgola sia il punto come separatore decimale.
     */
    private double leggiMisura(String etichetta) {
        while (true) {
            String riga = prompt(etichetta + " (" + simbolo() + ")> ").trim().replace(',', '.');
            try {
                double valore = Double.parseDouble(riga);
                if (valore > 0) {
                    return controller.unita().versoMm(valore);
                }
            } catch (NumberFormatException ignored) {
                // ricade nel messaggio sotto
            }
            System.out.println("  Inserisci una misura positiva in " + simbolo()
                    + " (es. " + esempioMisura() + ").");
        }
    }

    /** Un esempio di misura sensato nell'unita' corrente, per il messaggio d'errore. */
    private String esempioMisura() {
        return num(1500) + " o " + num(1476.5);
    }

    /**
     * Un prezzo in euro: virgola o punto, mai negativo. Riga vuota = tieni il valore attuale, cosi'
     * chi vuole cambiare un solo prezzo non deve ridigitare l'altro.
     */
    private double leggiPrezzo(String etichetta, double attuale) {
        while (true) {
            String riga = prompt(etichetta + " [" + euro(attuale) + "]> ").trim().replace(',', '.');
            if (riga.isEmpty()) {
                return attuale;
            }
            try {
                double valore = Double.parseDouble(riga);
                if (valore >= 0) {
                    return valore;
                }
            } catch (NumberFormatException ignored) {
                // ricade nel messaggio sotto
            }
            System.out.println("  Inserisci un prezzo non negativo (es. 6,50) oppure 0.");
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
            System.out.printf(" %2d. %-32s %-10s %8s x %-8s %-2s   x%d%n",
                    i++, s.tipologia().nome(), s.colore().nome(),
                    num(d.L()), num(d.H()), simbolo(), s.quantita());
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
            System.out.printf(" %2d. %-30s %-10s %10s x%d%n",
                    i++, etichetta(a.profilo()), a.colore().nome(), mis(a.lunghezza()), a.quantita());
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
        vetriDistinta(distinta);
    }

    /**
     * Le lastre della distinta. Le misure seguono l'unita' scelta dall'utente, l'area no: il vetro
     * si compra a metro quadro, e in mm quadri i numeri sarebbero illeggibili.
     */
    private void vetriDistinta(Distinta distinta) {
        if (distinta.vetri().isEmpty()) {
            System.out.printf("%n(nessuna quota vetro per queste tipologie)%n");
            return;
        }
        System.out.printf("%nVETRI - %s, %s in totale%n",
                conta(distinta.totaleLastre(), "lastra", "lastre"), mq(distinta.areaVetroTotaleMq()));
        for (Vetro v : distinta.vetri()) {
            System.out.printf("  - %9s x %-9s x%-3d  %s%n",
                    mis(v.lunghezza()), mis(v.larghezza()), v.quantita(), mq(v.areaTotaleMq()));
        }
    }

    @Override
    public void piano(PianoDiTaglio piano) {
        sezione("PIANO DI TAGLIO");
        int avanziUsati = piano.numeroBarre() - piano.barreNuove();
        System.out.printf("Barra standard: %s  |  kerf %s/taglio%n",
                mis(Ottimizzatore.BARRA_STANDARD_DEFAULT), mis(BarraTagliata.KERF));
        System.out.printf("Barre totali: %d  (%d nuove, %d avanzi)  |  media geom. sfrido %s%n%n",
                piano.numeroBarre(), piano.barreNuove(), avanziUsati, mis(piano.mediaGeometricaSfrido()));

        piano.perMateriale().forEach((materiale, barre) -> {
            long avanzi = barre.stream().filter(BarraTagliata::avanzo).count();
            System.out.printf("%s %-34s %d barre (%d nuove, %d avanzi)%n",
                    "-", etichetta(materiale), barre.size(), barre.size() - avanzi, avanzi);
            int i = 1;
            for (BarraTagliata barra : barre) {
                String tipo = (barra.avanzo() ? "AVANZO " : "NUOVA ") + mis(barra.lunghezzaBarra());
                System.out.printf("    #%-2d [%-16s]  occupato %9s  |  sfrido %9s%n",
                        i++, tipo, mis(barra.occupato()), mis(barra.sfrido()));
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
        String riga = "%-40s %-8s %12s %s%n";
        System.out.printf(riga, "Profilo / colore", "Barra", "Sfrido", "");
        System.out.println("-".repeat(72));
        piano.barre().stream()
                .sorted(Comparator.comparingDouble(BarraTagliata::sfrido).reversed())
                .forEach(barra -> System.out.printf(riga, etichetta(barra.materiale()),
                        barra.avanzo() ? "avanzo" : "nuova", mis(barra.sfrido()),
                        barra.sfrido() >= controller.sogliaRitaglio() ? "-> torna a magazzino" : ""));
    }

    @Override
    public void preventivo(Preventivo preventivo) {
        sezione("PREVENTIVO (materiale profili)  -  misure in " + simbolo());
        String riga = "%-28s %-10s %11s %7s %13s %11s %8s %13s %10s %12s%n";
        System.out.printf(riga, "Profilo", "Colore", "Barre nuove", "Avanzi", "Lungh. nuova",
                "Sfrido", "Ritagli", "Da riusare", "Peso (kg)", "Costo");
        System.out.println("-".repeat(131));
        for (RigaProfilo r : preventivo.righe()) {
            System.out.printf(riga, tronca(etichetta(r.profilo()), 28), r.colore().nome(), r.barreNuove(),
                    r.avanziUsati(), num(r.lunghezzaNuova()), num(r.sfrido()),
                    r.ritagliRecuperabili(), num(r.lunghezzaRecuperabile()),
                    kg(r.peso()), euro(r.costo(preventivo.prezzi())));
        }
        System.out.println("-".repeat(131));
        System.out.printf(riga, "TOTALE", "", preventivo.totaleBarreNuove(), preventivo.totaleAvanziUsati(),
                num(preventivo.lunghezzaNuovaTotale()), num(preventivo.sfridoTotale()),
                preventivo.totaleRitagliRecuperabili(), num(preventivo.lunghezzaRecuperabileTotale()),
                kg(preventivo.pesoTotale()), euro(preventivo.costoBarre()));

        System.out.printf("%nMateriale d'acquisto: %d barre da %s  =  %s lineari.%n",
                preventivo.totaleBarreNuove(), mis(Ottimizzatore.BARRA_STANDARD_DEFAULT),
                mis(preventivo.lunghezzaNuovaTotale()));
        System.out.printf("Torneranno a magazzino %s (>= %s), per un totale di %s;"
                        + " scarto effettivo %s.%n",
                conta(preventivo.totaleRitagliRecuperabili(), "ritaglio", "ritagli"),
                mis(controller.sogliaRitaglio()), mis(preventivo.lunghezzaRecuperabileTotale()),
                mis(preventivo.scartoTotale()));
        preventivoVetri(preventivo);
        costiPreventivo(preventivo);
    }

    /** Le lastre da ordinare al vetraio, aggregate per misura: quantita' e metri quadri. */
    private void preventivoVetri(Preventivo preventivo) {
        if (preventivo.righeVetro().isEmpty()) {
            return;
        }
        sezione("PREVENTIVO (vetro)  -  misure in " + simbolo() + ", aree in mq");
        String riga = "%14s %14s %10s %12s %12s %12s%n";
        System.out.printf(riga, "Altezza (H)", "Larghezza (L)", "Quantita'", "Area lastra",
                "Area totale", "Costo");
        System.out.println("-".repeat(79));
        for (RigaVetro r : preventivo.righeVetro()) {
            System.out.printf(riga, num(r.lunghezza()), num(r.larghezza()), r.quantita(),
                    mq(r.areaMq()), mq(r.areaTotaleMq()), euro(r.costo(preventivo.prezzi())));
        }
        System.out.println("-".repeat(79));
        System.out.printf(riga, "TOTALE", "", preventivo.totaleLastre(), "",
                mq(preventivo.areaVetroTotaleMq()), euro(preventivo.costoVetro()));
        System.out.printf("%nDa ordinare al vetraio: %s, %s di superficie.%n",
                conta(preventivo.totaleLastre(), "lastra", "lastre"), mq(preventivo.areaVetroTotaleMq()));
    }

    /**
     * Il conto dell'ordine: barre nuove + vetro. Se manca un prezzo lo dice invece di mostrare uno
     * zero che sembrerebbe un calcolo riuscito; il peso a zero significa che i profili del catalogo
     * non hanno ancora i kg/m di scheda.
     */
    private void costiPreventivo(Preventivo preventivo) {
        sezione("COSTO DEL MATERIALE");
        Prezzi prezzi = preventivo.prezzi();
        System.out.printf("Listino: alluminio %s/kg  |  vetro %s/mq%n",
                euro(prezzi.alChiloBarre()), euro(prezzi.alMqVetro()));
        String riga = "  %-28s %14s%n";
        System.out.printf(riga, "Barre nuove (" + kg(preventivo.pesoTotale()) + " kg)",
                euro(preventivo.costoBarre()));
        System.out.printf(riga, "Vetro (" + mq(preventivo.areaVetroTotaleMq()) + ")",
                euro(preventivo.costoVetro()));
        System.out.println("  " + "-".repeat(43));
        System.out.printf(riga, "TOTALE ORDINE", euro(preventivo.costoTotale()));

        if (!prezzi.impostati()) {
            System.out.println("\n  (prezzi non impostati: menu principale -> Prezzi del materiale)");
        }
        if (prezzi.alChiloBarre() > 0 && preventivo.pesoTotale() == 0) {
            System.out.println("\n  (peso a zero: i profili del catalogo non hanno ancora i kg/m,"
                    + " quindi le barre non si possono valorizzare)");
        }
    }

    /**
     * Il calcolo globale: tutti gli ordini <b>uniti in un piano solo</b> (così i pezzi condividono le
     * barre e lo sfrido cala), con il suo dettaglio per barra e il preventivo. Lo scarico del magazzino
     * lo committa e lo conferma il chiamante, non questo render.
     */
    @Override
    public void evasione(EvasioneOrdini evasione) {
        sezione("CALCOLO GLOBALE - " + conta(evasione.ordini().size(), "ordine", "ordini") + " uniti in un piano");
        for (Ordine ordine : evasione.ordini()) {
            System.out.printf("  - %-30s (%s)%n", ordine.nome(),
                    conta(ordine.totaleSerramenti(), "serramento", "serramenti"));
        }
        if (evasione.piano().numeroBarre() == 0) {
            System.out.println("  (nessun pezzo da tagliare)");
            return;
        }
        piano(evasione.piano());
        sfridi(evasione.piano());
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

    // Scorciatoie sulle etichette condivise con la GUI (vedi Etichette): qui restano solo
    // perche' compaiono in decine di printf, la formattazione vera sta in un posto solo.

    private String etichetta(Profilo p) {
        return Etichette.profilo(p);
    }

    private String etichetta(Materiale m) {
        return Etichette.materiale(m);
    }

    /** Una misura (sempre mm nel modello) nell'unita' scelta dall'utente, senza simbolo. */
    private String num(double v) {
        return Etichette.misura(v, controller.unita());
    }

    /** Una misura con il simbolo dell'unita' scelta: {@code "6.5 m"}. */
    private String mis(double v) {
        return Etichette.misuraConSimbolo(v, controller.unita());
    }

    /** Il simbolo dell'unita' corrente, per le intestazioni e i prompt. */
    private String simbolo() {
        return controller.unita().simbolo();
    }

    /**
     * Un'area in metri quadri. Il vetro si compra a mq qualunque sia l'unita' scelta per le
     * lunghezze, quindi qui non si converte nulla. Scritto "mq" e non con l'esponente: l'output
     * di questa view resta puro ASCII.
     */
    private static String mq(double areaMq) {
        return String.format("%.3f mq", areaMq);
    }

    /** Un importo in euro. Scritto "EUR" e non con il simbolo: questa view resta puro ASCII. */
    private static String euro(double importo) {
        return String.format("%.2f EUR", importo);
    }

    /** Un peso in chilogrammi (senza unita': sta nell'intestazione di colonna). */
    private static String kg(double chili) {
        return String.format("%.2f", chili);
    }

    /** Accorcia un'etichetta troppo lunga per la sua colonna, cosi' la tabella resta allineata. */
    private static String tronca(String testo, int larghezza) {
        return testo.length() <= larghezza ? testo : testo.substring(0, larghezza - 1) + ".";
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

    private static String etichettaTaglio(TipoTaglio taglio) {
        return Etichette.taglio(taglio);
    }
}
