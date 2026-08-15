package com.cutcalculator.cli;

import com.cutcalculator.app.Controller;
import com.cutcalculator.app.Etichette;
import com.cutcalculator.app.Unita;
import com.cutcalculator.app.View;
import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.catalogo.Sistema;
import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Categoria;
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
import com.cutcalculator.dominio.Variante;
import com.cutcalculator.dominio.Varianti;
import com.cutcalculator.dominio.Vetro;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.ottimizzatore.BarraTagliata;
import com.cutcalculator.ottimizzatore.Ottimizzatore;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.ottimizzatore.Strategia;
import com.cutcalculator.pianificazione.DistintaOrdine;
import com.cutcalculator.pianificazione.EvasioneOrdini;
import com.cutcalculator.pianificazione.QuotaOrdine;
import com.cutcalculator.persistenza.CalcoloOrdine;
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
        avvisoRigheScartate();
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
    /**
     * Avvisa se il file ordini conteneva righe illeggibili. Da quando gli ordini si risalvano da
     * soli, tacere non basta piu': la prima modifica riscriverebbe il file senza quelle righe.
     */
    private void avvisoRigheScartate() {
        int scartate = controller.righeOrdiniScartate();
        if (scartate > 0) {
            System.out.printf("%n!! ATTENZIONE: %s del file ordini non %s riconosciut%s"
                            + " (tipologia non piu' nel catalogo, o dati malformati).%n"
                            + "   Andra%s persa alla prima modifica, perche' gli ordini si"
                            + " risalvano da soli: correggi dati/ordini.csv prima di continuare.%n",
                    conta(scartate, "riga", "righe"),
                    scartate == 1 ? "e' stata" : "sono state",
                    scartate == 1 ? "a" : "e",
                    scartate == 1 ? "" : "nno");
        }
    }

    private boolean menuPrincipale() {
        System.out.println();
        System.out.println("=== MENU PRINCIPALE ===");
        System.out.println("  1) Magazzino  (" + conta(controller.totaleAvanzi(), "pezzo", "pezzi") + ")");
        System.out.println("  2) Ordini     (" + conta(controller.ordini().size(), "ordine", "ordini") + ")");
        System.out.println("  3) Unita' di misura  (" + controller.unita().descrizione() + ")");
        System.out.println("  4) Algoritmo di ottimizzazione  (" + controller.strategia().nome() + ")");
        System.out.println("  0) Esci");
        switch (leggiIntero("Scelta", 0, 4)) {
            case 1 -> menuMagazzino();
            case 2 -> menuOrdini();
            case 3 -> scegliUnita();
            case 4 -> scegliStrategia();
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
     * Sceglie l'euristica con cui i pezzi vengono impacchettati nelle barre. Vale dal <b>prossimo</b>
     * calcolo: i piani gia' fatti restano quelli, non si riscrivono da soli.
     */
    private void scegliStrategia() {
        Strategia scelta = scegliDaLista("Algoritmo di ottimizzazione (attuale: "
                        + controller.strategia().nome() + "):",
                List.of(Strategia.values()), Strategia::descrizione);
        controller.impostaStrategia(scelta);
        System.out.println("  I prossimi calcoli useranno: " + scelta.nome() + ".");
    }

    // I prezzi non sono un'impostazione: si dichiarano su ogni serramento, dove si sceglie anche il
    // colore da cui dipendono. Aggiungendone uno si propone il prezzo dell'ultimo inserito.

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
        String proposto = controller.nomeOrdineProposto();
        String nome = leggiNomeOrdine("Nome del nuovo ordine [" + proposto + "]> ", null);
        Ordine ordine = controller.nuovoOrdine(nome.isBlank() ? proposto : nome);
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

    // Salva/carica ordini a comando non ci sono piu': gli ordini si salvano da soli a ogni
    // modifica, come il magazzino, e si ricaricano all'avvio.

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
            System.out.println("  5) Mostra il calcolo (distinta, preventivo, vetri, prezzo)");
            System.out.println("  6) Ripristina (annulla il calcolo, torna da calcolare)");
            System.out.println("  0) Indietro");
            switch (leggiIntero("Scelta", 0, 6)) {
                case 1 -> aggiungiSerramento(ordine);
                case 2 -> ordine(ordine);
                case 3 -> rimuoviSerramento(ordine);
                case 4 -> rinominaOrdine(ordine);
                case 5 -> mostraCalcolo(ordine);
                case 6 -> ripristinaOrdine(ordine);
                case 0 -> resta = false;
            }
        }
    }

    private void aggiungiSerramento(Ordine ordine) {
        if (!confermaModificaDiCalcolato(ordine)) {
            return;
        }
        Sistema sistema = scegliSistema();
        Tipologia tipologia = scegliDaLista(
                "Tipologie di " + sistema.nome() + ":", sistema.tipologie(), Tipologia::nome);
        Varianti varianti = scegliVarianti(sistema);
        Colore colore = leggiColore("Colore");
        double l = leggiMisura("Larghezza L");
        double h = leggiMisura("Altezza H");
        double hf = tipologia.usaHF() ? leggiMisura("Altezza parziale HF") : 0;
        int quantita = leggiQuantita("Quantita'");
        // I prezzi cambiano con la finitura, quindi si dichiarano qui accanto al colore. Si propone
        // quello dell'ultimo serramento dell'ordine: chi continua sullo stesso colore preme Invio.
        Prezzi proposti = prezziProposti(ordine);
        double alKg = leggiPrezzo("Prezzo alluminio (EUR/kg)", proposti.alChiloBarre());
        double alMq = leggiPrezzo("Prezzo vetro (EUR/mq)", proposti.alMqVetro());
        controller.aggiungiSerramento(ordine, new Serramento(tipologia, colore,
                new Dimensione(l, h, hf), quantita, new Prezzi(alKg, alMq), varianti));
        System.out.printf("  + %s / %s [%s]  %s x %s %s%s  x%d%s%n",
                sistema.nome(), tipologia.nome(), colore.nome(), num(l), num(h), simbolo(),
                hf > 0 ? " (HF " + mis(hf) + ")" : "", quantita, Etichette.varianti(varianti));
    }

    /**
     * Chiede una variante per ogni ruolo che ne ha davvero piu' di una (telaio, anta...). I sistemi
     * che non ne dichiarano non chiedono nulla, quindi per gli scorrevoli la procedura resta identica
     * a prima. La prima voce di ogni elenco e' il profilo base della tipologia.
     */
    private Varianti scegliVarianti(Sistema sistema) {
        Varianti scelte = Varianti.NESSUNA;
        for (Categoria ruolo : sistema.ruoliConScelta()) {
            scelte = scelte.con(scegliDaLista(Etichette.ruolo(ruolo) + ":",
                    sistema.variantiDi(ruolo), Variante::nome));
        }
        return scelte;
    }

    /**
     * Avvisa prima di toccare i serramenti di un ordine <b>gia' calcolato</b>. Modificarlo lo rimette
     * fra quelli da calcolare, ma il prossimo calcolo lo ritaglia <b>da capo</b>: il materiale gia'
     * scalato viene contato una seconda volta, e il magazzino si allontana dalla realta' in silenzio.
     * La via giusta e' il ripristino, che quel calcolo lo annulla davvero.
     *
     * @return {@code false} se l'utente ha preferito non modificare
     */
    private boolean confermaModificaDiCalcolato(Ordine ordine) {
        if (!ordine.calcolato()) {
            return true;
        }
        System.out.printf("%n  !! \"%s\" e' gia' stato calcolato: il suo materiale e' gia' stato"
                + " scalato dal magazzino.%n", ordine.nome());
        System.out.println("     Modificandolo tornera' tra quelli da calcolare, ma il prossimo"
                + " calcolo lo ritagliera' DA CAPO,");
        System.out.println("     contando una seconda volta il materiale gia' evaso.");
        if (controller.ripristinabile(ordine)) {
            System.out.println("     Meglio prima \"Ripristina\" (opzione 6): annulla quel calcolo e"
                    + " rimette a posto il magazzino.");
        }
        return prompt("  Modificare comunque? [s/N]> ").trim().equalsIgnoreCase("s");
    }

    /** I prezzi dell'ultimo serramento dell'ordine, o nessuno se e' il primo. */
    private static Prezzi prezziProposti(Ordine ordine) {
        List<Serramento> serramenti = ordine.serramenti();
        return serramenti.isEmpty()
                ? Prezzi.NESSUNO
                : serramenti.get(serramenti.size() - 1).prezzi();
    }

    private void rimuoviSerramento(Ordine ordine) {
        if (ordine.serramenti().isEmpty()) {
            System.out.println("  Ordine vuoto: niente da rimuovere.");
            return;
        }
        if (!confermaModificaDiCalcolato(ordine)) {
            return;
        }
        ordine(ordine);
        int scelta = leggiIntero("Quale rimuovere (0 = annulla)", 0, ordine.serramenti().size());
        if (scelta == 0) {
            return;
        }
        controller.rimuoviSerramento(ordine, scelta - 1);
        System.out.println("  Rimosso.");
    }

    private void rinominaOrdine(Ordine ordine) {
        String nuovo = leggiNomeOrdine("Nuovo nome (vuoto = invariato)> ", ordine);
        if (nuovo.isBlank()) {
            System.out.println("  Nome invariato: " + ordine.nome());
            return;
        }
        controller.rinominaOrdine(ordine, nuovo);
        System.out.println("  Rinominato: " + ordine.nome());
    }

    // Il "calcolo provvisorio" del singolo ordine (anteprima non distruttiva) e' stato tolto:
    // mostrava un piano che il calcolo globale poi rifaceva diverso, perche' li' gli ordini si
    // uniscono e i pezzi condividono le barre. Un solo calcolo, quello che conta.

    /**
     * I documenti dell'ordine come sono stati salvati all'ultimo calcolo: distinta, preventivo,
     * vetri e prezzo. Non ricalcola nulla — rilegge da disco, quindi si vedono anche i calcoli
     * fatti in sessioni precedenti.
     */
    private void mostraCalcolo(Ordine ordine) {
        CalcoloOrdine calcolo = controller.calcoloDi(ordine);
        if (calcolo.vuoto()) {
            System.out.println(ordine.calcolato()
                    ? "  Nessun documento salvato per quest'ordine (calcolato prima che li salvassimo?)."
                    : "  Ordine non ancora calcolato: usa \"Calcola gli ordini da calcolare\".");
            return;
        }
        sezione("CALCOLO DI " + ordine.nome().toUpperCase()
                + (ordine.calcolato() ? "" : "  (ATTENZIONE: l'ordine e' stato modificato dopo)"));

        System.out.printf("Distinta - %s in %s%n", conta(calcolo.totalePezzi(), "pezzo", "pezzi"),
                conta(calcolo.distinta().size(), "misura", "misure"));
        String rigaDistinta = "  %-12s %-12s %12s %-12s %8s%n";
        System.out.printf(rigaDistinta, "Profilo", "Colore", "Lunghezza", "Taglio", "Quantita'");
        for (CalcoloOrdine.VoceDistinta v : calcolo.distinta()) {
            System.out.printf(rigaDistinta, v.profilo(), tronca(v.colore(), 12), num(v.lunghezza()),
                    v.taglio().replace("TAGLIO_", "").replace('_', '/'), "x" + v.quantita());
        }

        System.out.printf("%nPreventivo - quota profili%n");
        String rigaPrev = "  %-12s %-12s %12s %10s %13s%n";
        System.out.printf(rigaPrev, "Profilo", "Colore", "Profili", "Peso (kg)", "Costo");
        for (CalcoloOrdine.VocePreventivo v : calcolo.preventivo()) {
            System.out.printf(rigaPrev, v.profilo(), tronca(v.colore(), 12), num(v.lunghezza()),
                    kg(v.peso()), euro(v.costo()));
        }
        System.out.printf(rigaPrev, "TOTALE", "", "", kg(calcolo.pesoProfili()),
                euro(calcolo.costoProfili()));

        if (!calcolo.vetri().isEmpty()) {
            System.out.printf("%nVetri - %s%n", conta(calcolo.totaleLastre(), "lastra", "lastre"));
            String rigaVetro = "  %12s %12s %10s %12s %13s%n";
            System.out.printf(rigaVetro, "Altezza (H)", "Larghezza (L)", "Quantita'", "Area", "Costo");
            for (CalcoloOrdine.VoceVetro v : calcolo.vetri()) {
                System.out.printf(rigaVetro, num(v.altezza()), num(v.larghezza()), v.quantita(),
                        mq(v.areaMq()), euro(v.costo()));
            }
            System.out.printf(rigaVetro, "TOTALE", "", calcolo.totaleLastre(),
                    mq(calcolo.areaVetroMq()), euro(calcolo.costoVetro()));
        }

        System.out.println("\n  " + "-".repeat(43));
        System.out.printf("  %-28s %14s%n", "PREZZO DELL'ORDINE", euro(calcolo.costoTotale()));
    }

    /**
     * Annulla l'ultimo calcolo: il magazzino torna com'era prima e gli ordini di quel calcolo
     * tornano da calcolare. Riguarda <b>tutto il gruppo</b>, perche' le barre erano condivise, e vale
     * solo per il calcolo piu' recente.
     */
    private void ripristinaOrdine(Ordine ordine) {
        if (!controller.ripristinabile(ordine)) {
            List<String> ultimo = controller.ordiniRipristinabili();
            System.out.println(ultimo.isEmpty()
                    ? "  Niente da ripristinare: non c'e' nessun calcolo da annullare."
                    : "  Non si puo' ripristinare \"" + ordine.nome() + "\": si annulla solo"
                            + " l'ultimo calcolo, che riguardava " + String.join(", ", ultimo)
                            + ". Degli altri non si sa piu' com'era il magazzino di partenza.");
            return;
        }
        List<String> gruppo = controller.ordiniRipristinabili();
        System.out.println("  Verranno rimessi tra gli ordini da calcolare:");
        gruppo.forEach(nome -> System.out.println("    - " + nome));
        System.out.println("  Il magazzino torna esattamente com'era prima di quel calcolo: gli"
                + " avanzi usati tornano disponibili e i ritagli rientrati spariscono.");
        System.out.println("  ATTENZIONE: si perdono le modifiche fatte a mano al magazzino dopo il"
                + " calcolo, e i documenti (distinta, preventivo, vetri) di quegli ordini.");
        if (!prompt("  Confermi? [s/N]> ").trim().equalsIgnoreCase("s")) {
            System.out.println("  Annullato: niente e' cambiato.");
            return;
        }
        List<String> tornati = controller.ripristina(ordine);
        System.out.printf("  Ripristinati: %s.%n", String.join(", ", tornati));
        System.out.printf("  Magazzino riportato a %s.%n",
                conta(controller.totaleAvanzi(), "pezzo", "pezzi"));
    }

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
     * {@code ';'}, che è il separatore del file degli ordini e ne spezzerebbe la riga, e i nomi
     * <b>già usati</b>: gli ordini si ricaricano da disco per nome, quindi due omonimi si
     * fonderebbero in uno solo al prossimo avvio.
     *
     * @param tranne l'ordine che quel nome ce l'ha già (nel rinomina), oppure {@code null}
     */
    private String leggiNomeOrdine(String etichetta, Ordine tranne) {
        while (true) {
            String riga = prompt(etichetta).trim();
            if (riga.indexOf(';') >= 0) {
                System.out.println("  Il nome non puo' contenere ';' (separatore del file).");
            } else if (!riga.isBlank() && !controller.nomeLibero(riga, tranne)) {
                System.out.println("  C'e' gia' un ordine con questo nome: due omonimi si"
                        + " fonderebbero al prossimo avvio. Scegline un altro.");
            } else {
                return riga;
            }
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
            System.out.printf(" %2d. %-32s %-10s %8s x %-8s %-2s   x%d%s%n",
                    i++, s.tipologia().nome(), s.colore().nome(),
                    num(d.L()), num(d.H()), simbolo(), s.quantita(),
                    Etichette.varianti(s.varianti()));
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

    /**
     * I pezzi da tagliare, <b>un blocco per ordine</b> (e dentro, come sempre, raggruppati per
     * materiale): il piano li mescolera' sulle barre, ma in officina serve sapere di chi e' ognuno.
     */
    @Override
    public void distinta(List<DistintaOrdine> distinte) {
        sezione("DISTINTA DI TAGLIO");
        int totale = distinte.stream().mapToInt(d -> d.distinta().totalePezzi()).sum();
        System.out.printf("Totale pezzi da tagliare: %d, in %s%n",
                totale, conta(distinte.size(), "ordine", "ordini"));
        for (DistintaOrdine voce : distinte) {
            System.out.printf("%n=> %s  (%d pezzi)%n", voce.ordine(), voce.distinta().totalePezzi());
            if (voce.distinta().totalePezzi() == 0) {
                System.out.println("     (ordine vuoto)");
            }
            voce.distinta().perMateriale().forEach((materiale, pezzi) -> {
                System.out.printf("   %s %-34s (%s): %d pezzi%n",
                        "-", etichetta(materiale), materiale.profilo().categoria(), pezzi.size());
                System.out.printf("       %s%n", riepilogoPezzi(pezzi));
            });
            vetriDistinta(voce.distinta());
        }
    }

    /**
     * Il vetro nella distinta e' solo un riepilogo: le misure lastra per lastra, con i costi, stanno
     * poco piu' sotto nel preventivo vetro, e ripeterle qui sarebbe rumore.
     */
    private void vetriDistinta(Distinta distinta) {
        if (distinta.vetri().isEmpty()) {
            return;
        }
        System.out.printf("     Vetri: %s, %s (misure e costi nel preventivo vetro).%n",
                conta(distinta.totaleLastre(), "lastra", "lastre"), mq(distinta.areaVetroTotaleMq()));
    }

    @Override
    public void piano(PianoDiTaglio piano) {
        sezione("PIANO DI TAGLIO");
        int avanziUsati = piano.numeroBarre() - piano.barreNuove();
        System.out.printf("Barra standard: %s  |  kerf %s/taglio  |  algoritmo: %s%n",
                mis(Ottimizzatore.BARRA_STANDARD_DEFAULT), mis(BarraTagliata.KERF),
                controller.strategia().nome());
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
        preventivo(preventivo, List.of());
    }

    /**
     * Il preventivo, <b>diviso per ordine</b> se ce n'e' piu' d'uno: prima un blocco per ciascuno
     * (la sua quota di profili e le sue lastre), poi il totale con barre, sfrido e ritagli, che sono
     * grandezze del piano condiviso e non si dividono in numeri interi.
     */
    private void preventivo(Preventivo preventivo, List<QuotaOrdine> quote) {
        preventivoProfili(preventivo, quote);
        preventivoVetri(preventivo, quote);
        costiPreventivo(preventivo);
    }

    /**
     * Il totale di ogni ordine, una riga per ciascuno, piu' il totale di tutti. Compare solo con
     * <b>piu' di un ordine</b>: con uno solo ripeterebbe la cifra gia' scritta sopra.
     */
    private void perOrdine(List<QuotaOrdine> quote, java.util.function.ToDoubleFunction<QuotaOrdine> costo,
            String nota) {
        if (quote.size() <= 1) {
            return;
        }
        System.out.printf("%nPer ordine:%n");
        String riga = "  %-32s %14s%n";
        for (QuotaOrdine quota : quote) {
            System.out.printf(riga, tronca(quota.ordine(), 32), euro(costo.applyAsDouble(quota)));
        }
        System.out.println("  " + "-".repeat(47));
        System.out.printf(riga, "TOTALE", euro(quote.stream().mapToDouble(costo).sum()));
        if (!nota.isEmpty()) {
            System.out.println("  (" + nota + ")");
        }
    }

    /** La tabella per materiale: barre da comprare, avanzi, sfrido, ritagli, peso e costo. */
    private void preventivoProfili(Preventivo preventivo, List<QuotaOrdine> quote) {
        sezione("PREVENTIVO (materiale profili)  -  misure in " + simbolo());
        String riga = "%-28s %-10s %11s %7s %13s %11s %8s %13s %10s %12s%n";
        System.out.printf(riga, "Profilo", "Colore", "Barre nuove", "Avanzi", "Lungh. nuova",
                "Sfrido", "Ritagli", "Da riusare", "Peso (kg)", "Costo");
        System.out.println("-".repeat(131));
        for (RigaProfilo r : preventivo.righe()) {
            System.out.printf(riga, tronca(etichetta(r.profilo()), 28), r.colore().nome(), r.barreNuove(),
                    r.avanziUsati(), num(r.lunghezzaNuova()), num(r.sfrido()),
                    r.ritagliRecuperabili(), num(r.lunghezzaRecuperabile()),
                    kg(r.peso()), euro(r.costo()));
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
        perOrdine(quote, QuotaOrdine::costoProfili,
                "le barre sono condivise: la quota di ciascuno e' proporzionale ai mm tagliati,"
                        + " sfrido compreso");
    }

    /**
     * Le lastre da ordinare al vetraio, aggregate per misura: quantita' e metri quadri. Con piu'
     * ordini si divide anche questa, e qui la divisione e' <b>esatta</b> — le lastre non si
     * condividono, quindi ogni riga appartiene a un ordine solo.
     */
    private void preventivoVetri(Preventivo preventivo, List<QuotaOrdine> quote) {
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
                    mq(r.areaMq()), mq(r.areaTotaleMq()), euro(r.costo()));
        }
        System.out.println("-".repeat(79));
        System.out.printf(riga, "TOTALE", "", preventivo.totaleLastre(), "",
                mq(preventivo.areaVetroTotaleMq()), euro(preventivo.costoVetro()));
        System.out.printf("%nDa ordinare al vetraio: %s, %s di superficie.%n",
                conta(preventivo.totaleLastre(), "lastra", "lastre"), mq(preventivo.areaVetroTotaleMq()));
        perOrdine(quote, QuotaOrdine::costoVetro, "");   // le lastre non si condividono: qui e' esatto
    }

    /**
     * Il conto dell'ordine: barre nuove + vetro. Se manca un prezzo lo dice invece di mostrare uno
     * zero che sembrerebbe un calcolo riuscito; il peso a zero significa che i profili del catalogo
     * non hanno ancora i kg/m di scheda.
     */
    private void costiPreventivo(Preventivo preventivo) {
        sezione("COSTO DEL MATERIALE");
        System.out.println("(i prezzi sono quelli dichiarati su ogni serramento)");
        String riga = "  %-28s %14s%n";
        System.out.printf(riga, "Barre nuove (" + kg(preventivo.pesoTotale()) + " kg)",
                euro(preventivo.costoBarre()));
        System.out.printf(riga, "Vetro (" + mq(preventivo.areaVetroTotaleMq()) + ")",
                euro(preventivo.costoVetro()));
        System.out.println("  " + "-".repeat(43));
        System.out.printf(riga, "TOTALE ORDINE", euro(preventivo.costoTotale()));

        if (!preventivo.valorizzato()) {
            System.out.println("\n  (nessun prezzo dichiarato sui serramenti: i costi restano a zero)");
        } else if (preventivo.costoBarre() == 0 && preventivo.pesoTotale() == 0) {
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
        distinta(evasione.distinte());
        piano(evasione.piano());
        sfridi(evasione.piano());
        preventivo(evasione.preventivoTotale(), evasione.quote());
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
