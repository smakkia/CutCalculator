package com.cutcalculator.app;

import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Prezzi;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.formule.GeneratoreDistinta;
import com.cutcalculator.ottimizzatore.BestFitDecreasing;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.persistenza.ArchivioImpostazioni;
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
    private final List<Avanzo> magazzino = new ArrayList<>();
    private final List<Ordine> ordini = new ArrayList<>();
    private Unita unita = Unita.PREDEFINITA;
    private Prezzi prezzi = Prezzi.NESSUNO;

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
     * Collega gli archivi su disco. Il <b>magazzino</b> viene caricato all'avvio e risalvato a ogni
     * modifica ({@code archivio}). Gli <b>ordini</b> non si caricano da soli: il salvataggio e il
     * caricamento sono <b>su comando</b> ({@link #salvaOrdini()} / {@link #caricaOrdini()}). Le
     * <b>impostazioni</b> (oggi la sola {@link Unita unità di misura}) si caricano all'avvio e si
     * salvano a ogni cambio. Con un archivio null la rispettiva persistenza è disattivata.
     */
    public Controller(Catalogo catalogo, ArchivioMagazzino archivio, ArchivioOrdini archivioOrdini,
            ArchivioImpostazioni archivioImpostazioni) {
        this.catalogo = catalogo;
        this.archivio = archivio;
        this.archivioOrdini = archivioOrdini;
        this.archivioImpostazioni = archivioImpostazioni;
        if (archivio != null) {
            magazzino.addAll(archivio.carica());
        }
        if (archivioImpostazioni != null) {
            unita = archivioImpostazioni.caricaUnita();
            prezzi = archivioImpostazioni.caricaPrezzi();
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

    /**
     * Il listino con cui valorizzare il preventivo (€/kg dell'alluminio, €/mq del vetro). È un dato
     * dell'utente, non del catalogo: finché non lo imposta vale {@link Prezzi#NESSUNO} e i costi
     * vengono zero.
     */
    public Prezzi prezzi() {
        return prezzi;
    }

    /** Cambia il listino e lo persiste, se c'è un archivio impostazioni collegato. */
    public void impostaPrezzi(Prezzi prezzi) {
        this.prezzi = prezzi;
        if (archivioImpostazioni != null) {
            archivioImpostazioni.salvaPrezzi(prezzi);
        }
    }

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
        return ordine;
    }

    public void rimuoviOrdine(Ordine ordine) {
        ordini.remove(ordine);
    }

    /** {@code true} se c'è un archivio ordini collegato (salvataggio/caricamento disponibili). */
    public boolean persistenzaOrdiniAttiva() {
        return archivioOrdini != null;
    }

    /** Salva su disco tutti gli ordini correnti; no-op se non c'è un archivio collegato. */
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
                .genera(piano, distinta.vetri(), sogliaRitaglio(), prezzi);
        return new Risultato(distinta, piano, preventivo);
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
        EvasioneOrdini evasione = new PianificatoreOrdini().pianifica(daCalcolare, magazzino, prezzi);
        magazzino.clear();
        magazzino.addAll(evasione.magazzinoAggiornato());
        salva();
        daCalcolare.forEach(Ordine::segnaCalcolato);
        return evasione;
    }

    /** I tre output della pipeline per un ordine. Vedi {@link #calcola}: non più usato dalle UI. */
    public record Risultato(Distinta distinta, PianoDiTaglio piano, Preventivo preventivo) {
    }
}
