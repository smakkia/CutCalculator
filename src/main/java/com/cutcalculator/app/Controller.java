package com.cutcalculator.app;

import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Prezzi;
import com.cutcalculator.dominio.Serramento;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.formule.GeneratoreDistinta;
import com.cutcalculator.ottimizzatore.BestFitDecreasing;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.persistenza.ArchivioCalcoli;
import com.cutcalculator.persistenza.ArchivioImpostazioni;
import com.cutcalculator.persistenza.CalcoloOrdine;
import com.cutcalculator.persistenza.ArchivioMagazzino;
import com.cutcalculator.persistenza.ArchivioOrdini;
import com.cutcalculator.pianificazione.EvasioneOrdini;
import com.cutcalculator.pianificazione.PianificatoreOrdini;
import com.cutcalculator.preventivo.GeneratorePreventivo;
import com.cutcalculator.preventivo.Preventivo;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo <b>stato</b> dell'applicazione e le operazioni su di esso, del tutto indipendenti dalla
 * UI (non stampa e non legge nulla). Tiene il {@link Catalogo}, il magazzino degli
 * {@link Avanzo avanzi} e la lista degli {@link Ordine ordini}, ed espone le azioni per
 * modificarli e per far scorrere la pipeline ({@link #calcola}).
 * <p>
 * Una view — oggi {@code CliView}, domani {@code GuiFx} — ci si appoggia sopra: legge lo stato e
 * invoca le operazioni, senza duplicarne la gestione. Il magazzino è condiviso da tutti gli
 * ordini; {@link #calcola} riusa gli avanzi correnti senza consumarli.
 */
public final class Controller {

    private final Catalogo catalogo;
    private final ArchivioMagazzino archivio;
    private final ArchivioOrdini archivioOrdini;
    private final ArchivioImpostazioni archivioImpostazioni;
    private final ArchivioCalcoli archivioCalcoli;
    private final List<Avanzo> magazzino = new ArrayList<>();
    private final List<Ordine> ordini = new ArrayList<>();
    private Unita unita = Unita.PREDEFINITA;

    /** Solo in memoria, senza persistenza: comodo per test o usi effimeri. */
    public Controller(Catalogo catalogo) {
        this(catalogo, null, null, null);
    }

    /** Con la sola persistenza del magazzino (gli ordini restano in memoria). */
    public Controller(Catalogo catalogo, ArchivioMagazzino archivio) {
        this(catalogo, archivio, null, null);
    }

    /** Magazzino e ordini su disco, impostazioni solo in memoria. */
    public Controller(Catalogo catalogo, ArchivioMagazzino archivio, ArchivioOrdini archivioOrdini) {
        this(catalogo, archivio, archivioOrdini, null);
    }

    /**
     * Collega gli archivi su disco. <b>Magazzino</b>, <b>ordini</b> e <b>impostazioni</b> si caricano
     * all'avvio e si risalvano a ogni modifica: chiudere l'applicazione non perde niente. Con un
     * archivio null la rispettiva persistenza è disattivata e quei dati restano solo in memoria.
     */
    public Controller(Catalogo catalogo, ArchivioMagazzino archivio, ArchivioOrdini archivioOrdini,
            ArchivioImpostazioni archivioImpostazioni) {
        this(catalogo, archivio, archivioOrdini, archivioImpostazioni, null);
    }

    /**
     * Come sopra, più l'{@link ArchivioCalcoli}: a ogni calcolo globale, distinta, preventivo e vetri
     * di ogni ordine evaso vengono scritti su disco e restano consultabili anche dopo la chiusura.
     */
    public Controller(Catalogo catalogo, ArchivioMagazzino archivio, ArchivioOrdini archivioOrdini,
            ArchivioImpostazioni archivioImpostazioni, ArchivioCalcoli archivioCalcoli) {
        this.catalogo = catalogo;
        this.archivio = archivio;
        this.archivioOrdini = archivioOrdini;
        this.archivioImpostazioni = archivioImpostazioni;
        this.archivioCalcoli = archivioCalcoli;
        if (archivio != null) {
            magazzino.addAll(archivio.carica());
        }
        if (archivioOrdini != null) {
            ordini.addAll(archivioOrdini.carica());
        }
        if (archivioImpostazioni != null) {
            unita = archivioImpostazioni.caricaUnita();
        }
    }

    // --- Impostazioni ------------------------------------------------------------------

    /**
     * L'unità con cui la UI mostra e legge le misure. Il modello resta sempre in millimetri:
     * questa è solo la lente con cui l'utente ci guarda attraverso.
     */
    public Unita unita() {
        return unita;
    }

    /** Cambia l'unità di misura e la persiste, se c'è un archivio impostazioni collegato. */
    public void impostaUnita(Unita unita) {
        this.unita = unita;
        if (archivioImpostazioni != null) {
            archivioImpostazioni.salvaUnita(unita);
        }
    }

    // I prezzi non sono un'impostazione: si dichiarano su ogni Serramento, dove si sceglie anche il
    // colore da cui dipendono. Le UI propongono quelli dell'ultimo serramento inserito.

    // --- Lettura dello stato -----------------------------------------------------------

    public Catalogo catalogo() {
        return catalogo;
    }

    /** Vista in sola lettura del magazzino (l'ordine è quello di inserimento). */
    public List<Avanzo> magazzino() {
        return List.copyOf(magazzino);
    }

    /** Numero totale di spezzoni a magazzino: la somma delle quantità di ogni {@link Avanzo}. */
    public int totaleAvanzi() {
        return magazzino.stream().mapToInt(Avanzo::quantita).sum();
    }

    /** Vista in sola lettura degli ordini (l'ordine è quello di inserimento). */
    public List<Ordine> ordini() {
        return List.copyOf(ordini);
    }

    /**
     * Gli ordini <b>da calcolare</b>: quelli che il prossimo calcolo globale prenderà in carico.
     * Un ordine ci resta finché non viene evaso, e ci <b>ritorna</b> se lo si modifica.
     */
    public List<Ordine> ordiniDaCalcolare() {
        return ordini.stream().filter(ordine -> !ordine.calcolato()).toList();
    }

    /** Gli ordini <b>già calcolati</b>: il loro materiale è stato scalato dal magazzino. */
    public List<Ordine> ordiniCalcolati() {
        return ordini.stream().filter(Ordine::calcolato).toList();
    }

    // --- Operazioni sul magazzino ------------------------------------------------------

    public void aggiungiAvanzo(Avanzo avanzo) {
        magazzino.add(avanzo);
        salva();
    }

    /**
     * Toglie {@code quantita} spezzoni dall'avanzo alla posizione data. Se sono tutti (o più),
     * rimuove l'intera riga; altrimenti la sostituisce con una copia dalla quantità ridotta
     * (l'{@link Avanzo} è immutabile).
     *
     * @return l'avanzo aggiornato che resta a magazzino, oppure {@code null} se la riga è sparita
     */
    public Avanzo rimuoviAvanzo(int indice, int quantita) {
        Avanzo avanzo = magazzino.get(indice);
        Avanzo rimasto;
        if (quantita >= avanzo.quantita()) {
            magazzino.remove(indice);
            rimasto = null;
        } else {
            rimasto = new Avanzo(avanzo.profilo(), avanzo.colore(),
                    avanzo.lunghezza(), avanzo.quantita() - quantita);
            magazzino.set(indice, rimasto);
        }
        salva();
        return rimasto;
    }

    /**
     * Svuota il magazzino: rimuove <b>tutti</b> gli spezzoni presenti e persiste.
     *
     * @return quanti spezzoni sono stati rimossi in totale
     */
    public int svuotaMagazzino() {
        int totale = totaleAvanzi();
        magazzino.clear();
        salva();
        return totale;
    }

    /** Persiste il magazzino se c'è un archivio collegato (altrimenti resta solo in memoria). */
    private void salva() {
        if (archivio != null) {
            archivio.salva(magazzino);
        }
    }

    // --- Operazioni sugli ordini -------------------------------------------------------

    /**
     * Crea un ordine con questo nome, lo aggiunge e lo restituisce (l'{@link Ordine} è modificabile).
     * Il colore si sceglie sul singolo {@link com.cutcalculator.dominio.Serramento}, non sull'ordine.
     */
    public Ordine nuovoOrdine(String nome) {
        Ordine ordine = new Ordine(nome);
        ordini.add(ordine);
        salvaOrdini();
        return ordine;
    }

    public void rimuoviOrdine(Ordine ordine) {
        ordini.remove(ordine);
        salvaOrdini();
    }

    /**
     * Aggiunge un serramento a un ordine e <b>persiste</b>.
     * <p>
     * Le modifiche ai serramenti passano di qui e non direttamente dall'{@link Ordine}, che pure
     * sarebbe capace di farle: è l'unico modo perché il salvataggio automatico se ne accorga. La
     * regola è la stessa del magazzino — lo stato lo possiede il controller, le view glielo chiedono
     * e gli delegano le modifiche.
     */
    public void aggiungiSerramento(Ordine ordine, Serramento serramento) {
        ordine.aggiungi(serramento);
        salvaOrdini();
    }

    /** Toglie il serramento alla posizione data e persiste. */
    public void rimuoviSerramento(Ordine ordine, int indice) {
        ordine.rimuovi(indice);
        salvaOrdini();
    }

    /** Sostituisce un serramento con la versione corretta e persiste. */
    public void sostituisciSerramento(Ordine ordine, int indice, Serramento serramento) {
        ordine.sostituisci(indice, serramento);
        salvaOrdini();
    }

    /** Rinomina l'ordine e persiste (il nome non cambia lo stato di calcolo). */
    public void rinominaOrdine(Ordine ordine, String nome) {
        ordine.rinomina(nome);
        salvaOrdini();
    }

    /** {@code true} se c'è un archivio ordini collegato (salvataggio/caricamento disponibili). */
    public boolean persistenzaOrdiniAttiva() {
        return archivioOrdini != null;
    }

    /**
     * Quante righe del file ordini l'ultimo caricamento non ha saputo interpretare: righe che il
     * prossimo salvataggio automatico farebbe sparire. Le UI lo dicono all'avvio — zero significa
     * che il file è stato letto per intero.
     */
    public int righeOrdiniScartate() {
        return archivioOrdini == null ? 0 : archivioOrdini.righeScartate();
    }

    /**
     * Salva su disco tutti gli ordini correnti; no-op se non c'è un archivio collegato. Lo chiamano
     * da sole tutte le operazioni che toccano gli ordini: non è un comando dell'utente.
     */
    public void salvaOrdini() {
        if (archivioOrdini != null) {
            archivioOrdini.salva(ordini);
        }
    }

    /**
     * Rimpiazza gli ordini in memoria con quelli caricati da disco; no-op (ritorna 0) se non c'è
     * un archivio collegato.
     *
     * @return quanti ordini sono stati caricati
     */
    public int caricaOrdini() {
        if (archivioOrdini == null) {
            return 0;
        }
        List<Ordine> caricati = archivioOrdini.carica();
        ordini.clear();
        ordini.addAll(caricati);
        return caricati.size();
    }

    // --- Pipeline ----------------------------------------------------------------------

    /**
     * Fa scorrere l'ordine lungo le tre fasi (distinta → piano → preventivo) riusando gli
     * avanzi correnti del magazzino <b>senza consumarli</b>, e restituisce i tre risultati insieme.
     * <p>
     * <b>Non è più esposto dalle UI</b> (2026-08-14): il "calcolo provvisorio" del singolo ordine
     * confondeva, perché mostrava un piano che il calcolo globale poi rifaceva diverso — lì gli
     * ordini si uniscono e condividono le barre. Resta qui solo perché lo usano i test: quando li
     * rivedremo, questo metodo e il record {@link Risultato} se ne vanno insieme a loro.
     *
     * @throws IllegalArgumentException se un pezzo è più lungo della barra standard
     */
    public Risultato calcola(Ordine ordine) {
        Distinta distinta = new GeneratoreDistinta().genera(ordine);
        PianoDiTaglio piano = new BestFitDecreasing().ottimizza(distinta, magazzino);
        Preventivo preventivo = new GeneratorePreventivo()
                .genera(piano, distinta.vetri(), sogliaRitaglio());
        return new Risultato(distinta, piano, preventivo);
    }

    /**
     * I documenti dell'ultimo calcolo di quest'ordine — distinta, preventivo, vetri e costo — riletti
     * da disco. Vuoto se l'ordine non è mai stato calcolato o se non c'è un archivio collegato.
     */
    public CalcoloOrdine calcoloDi(Ordine ordine) {
        return archivioCalcoli == null
                ? new CalcoloOrdine(ordine.nome(), List.of(), List.of(), List.of())
                : archivioCalcoli.carica(ordine.nome());
    }

    /** La soglia (mm) sopra cui un ritaglio rientra in magazzino come nuovo avanzo. */
    public double sogliaRitaglio() {
        return PianificatoreOrdini.SOGLIA_RITAGLIO_DEFAULT;
    }

    /**
     * Calcolo <b>globale</b>: pianifica insieme gli ordini <b>da calcolare</b> (non quelli già
     * evasi) sul magazzino condiviso, così i loro pezzi condividono le barre, e poi <b>applica</b>
     * il risultato — sostituisce il magazzino con quello aggiornato (avanzi usati consumati, ritagli
     * sopra soglia rientrati), lo persiste e segna gli ordini come calcolati.
     * <p>
     * Gli ordini già calcolati restano fuori: il loro materiale è stato scalato una volta e
     * ripassarci sopra lo conterebbe due volte.
     *
     * @throws IllegalArgumentException se un pezzo è più lungo della barra standard
     */
    public EvasioneOrdini evadiOrdini() {
        List<Ordine> daCalcolare = ordiniDaCalcolare();
        EvasioneOrdini evasione = new PianificatoreOrdini().pianifica(daCalcolare, magazzino);
        magazzino.clear();
        magazzino.addAll(evasione.magazzinoAggiornato());
        salva();
        daCalcolare.forEach(Ordine::segnaCalcolato);
        salvaOrdini();   // e' cambiato lo stato: sono passati tra i calcolati
        if (archivioCalcoli != null) {
            archivioCalcoli.salva(evasione);   // distinta, preventivo e vetri di ogni ordine evaso
        }
        return evasione;
    }

    /** I tre output della pipeline per un ordine. Vedi {@link #calcola}: non più usato dalle UI. */
    public record Risultato(Distinta distinta, PianoDiTaglio piano, Preventivo preventivo) {
    }
}
