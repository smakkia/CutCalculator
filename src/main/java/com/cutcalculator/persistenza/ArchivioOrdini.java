package com.cutcalculator.persistenza;

import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.catalogo.Sistema;
import com.cutcalculator.dominio.Categoria;
import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Dimensione;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Prezzi;
import com.cutcalculator.dominio.Serramento;
import com.cutcalculator.dominio.Tipologia;
import com.cutcalculator.dominio.Variante;
import com.cutcalculator.dominio.Varianti;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Archivio su disco degli ordini: carica e salva la lista di {@link Ordine} in un CSV semplice, una
 * riga per {@link Serramento} nel formato
 * {@code ordine;sistema;tipologia;colore;L;H;HF;quantita;calcolato;prezzoKg;prezzoMq;varianti}.
 * <p>
 * Le <b>varianti</b> stanno in un <b>campo solo</b>, {@code RUOLO=nome|RUOLO=nome} (vuoto = tutti i
 * profili base): i ruoli con alternative aumenteranno, e un campo per ruolo avrebbe voluto dire
 * cambiare il formato ogni volta. Anche qui si salva il <b>nome</b> e si ri-risolve dal catalogo; se
 * il nome non c'è più, la riga è scartata invece che caricata coi profili base, perché un serramento
 * con le quote sbagliate è peggio di un serramento mancante.
 * <p>
 * L'ultimo campo ({@code 1}/{@code 0}) dice se l'ordine è <b>già stato calcolato</b>: senza di lui,
 * ricaricando il file gli ordini evasi tornerebbero "da calcolare" e il calcolo globale scalerebbe
 * il magazzino una seconda volta. È ripetuto su ogni riga dell'ordine, che è ridondante ma tiene il
 * formato a una riga per serramento; le righe a <b>8 campi</b> (i file salvati prima) valgono "da
 * calcolare". Fa eccezione l'ordine <b>vuoto</b>, che si salva con la sola riga-nome e quindi torna
 * sempre da calcolare: senza serramenti non ha materiale da scalare, quindi non cambia nulla.
 * <p>
 * Come per il magazzino, si memorizzano solo i <b>nomi</b> di sistema e tipologia: al caricamento la
 * {@link Tipologia} viene ri-risolta contro il {@link Catalogo} (le ricette "vere" vengono di lì).
 * Un ordine senza serramenti si salva con una riga <b>solo nome</b>, così non va perso. Gli ordini
 * si raggruppano per <b>nome</b>: due ordini con lo stesso nome si fondono al reload.
 * <p>
 * Righe vuote, commenti ({@code #}), con sistema/tipologia sconosciuti, colore vuoto o numeri
 * malformati vengono ignorate: un file corretto a mano non fa crashare l'app. UTF-8, BOM tolto.
 * A differenza del magazzino non c'è autosave: il salvataggio e il caricamento sono su comando.
 */
public final class ArchivioOrdini {

    private static final String SEP = ";";
    /** Dentro il campo varianti: separatore fra le scelte e fra ruolo e nome. */
    private static final String VARIANTI = "|";
    private static final String VALORE = "=";
    private static final char BOM = '﻿';
    /** 9° campo: l'ordine è già stato evaso da un calcolo globale. */
    private static final String CALCOLATO = "1";
    private static final String DA_CALCOLARE = "0";

    private final Path file;
    private final Catalogo catalogo;
    private int righeScartate;

    public ArchivioOrdini(Path file, Catalogo catalogo) {
        this.file = file;
        this.catalogo = catalogo;
    }

    /** Carica gli ordini dal file; lista vuota se il file non esiste ancora. */
    public List<Ordine> carica() {
        righeScartate = 0;
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        Map<String, Ordine> perNome = new LinkedHashMap<>();
        try {
            for (String riga : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                leggiRiga(riga, perNome);
            }
        } catch (IOException lettura) {
            throw new UncheckedIOException("Impossibile leggere gli ordini da " + file, lettura);
        }
        return new ArrayList<>(perNome.values());
    }

    /**
     * Quante righe con contenuto l'ultimo {@link #carica()} <b>non</b> ha saputo interpretare — di
     * solito una tipologia che nel catalogo non esiste più, o un numero scritto male.
     * <p>
     * Scartarle è voluto (un file corretto a mano non deve far crashare l'app), ma da quando gli
     * ordini si <b>risalvano da soli</b> il silenzio non basta più: la prima modifica riscrive il
     * file senza quelle righe, e il dato è perso per sempre. Le UI lo dicono all'avvio, così c'è il
     * tempo di sistemare il file prima che venga riscritto.
     */
    public int righeScartate() {
        return righeScartate;
    }

    /** Salva tutti gli ordini sovrascrivendo il file (crea le cartelle mancanti). */
    public void salva(List<Ordine> ordini) {
        List<String> righe = new ArrayList<>();
        for (Ordine ordine : ordini) {
            List<Serramento> serramenti = ordine.serramenti();
            if (serramenti.isEmpty()) {
                righe.add(ordine.nome());   // riga-segnaposto: preserva l'ordine vuoto
                continue;
            }
            for (Serramento serramento : serramenti) {
                String sistema = nomeSistema(serramento.tipologia());
                if (sistema == null) {
                    continue;   // tipologia non nel catalogo: non salvabile
                }
                Dimensione d = serramento.dimensione();
                righe.add(String.join(SEP, ordine.nome(), sistema, serramento.tipologia().nome(),
                        serramento.colore().nome(), Double.toString(d.L()), Double.toString(d.H()),
                        Double.toString(d.HF()), Integer.toString(serramento.quantita()),
                        ordine.calcolato() ? CALCOLATO : DA_CALCOLARE,
                        Double.toString(serramento.prezzi().alChiloBarre()),
                        Double.toString(serramento.prezzi().alMqVetro()),
                        scriviVarianti(serramento.varianti())));
            }
        }
        try {
            Path cartella = file.getParent();
            if (cartella != null) {
                Files.createDirectories(cartella);
            }
            Files.write(file, righe, StandardCharsets.UTF_8);
        } catch (IOException scrittura) {
            throw new UncheckedIOException("Impossibile salvare gli ordini su " + file, scrittura);
        }
    }

    /** Interpreta una riga e la accumula nella mappa nome→ordine; ignora le righe non valide. */
    private void leggiRiga(String riga, Map<String, Ordine> perNome) {
        String pulita = togliBom(riga).trim();
        if (pulita.isEmpty() || pulita.startsWith("#")) {
            return;
        }
        String[] campi = pulita.split(SEP, -1);
        String nome = campi[0].trim();
        if (nome.isEmpty()) {
            return;
        }
        if (campi.length == 1) {
            perNome.computeIfAbsent(nome, Ordine::new);   // ordine vuoto
            return;
        }
        if (campi.length != 8 && campi.length != 9 && campi.length != 11 && campi.length != 12) {
            righeScartate++;
            return;
        }
        Serramento serramento = leggiSerramento(campi);
        if (serramento == null) {
            righeScartate++;
            return;
        }
        Ordine ordine = perNome.computeIfAbsent(nome, Ordine::new);
        ordine.aggiungi(serramento);   // rimette l'ordine tra quelli da calcolare...
        // ...quindi lo stato salvato si riapplica dopo. Il flag è il 9° campo sia nel formato a 9
        // sia in quello a 11 (con i prezzi in coda): guardarlo solo nel primo lo perdeva.
        if (campi.length >= 9 && CALCOLATO.equals(campi[8].trim())) {
            ordine.segnaCalcolato();
        }
    }

    /** Ricostruisce un serramento dai campi; {@code null} se tipologia sconosciuta o dati invalidi. */
    private Serramento leggiSerramento(String[] campi) {
        Optional<Sistema> sistema = catalogo.sistema(campi[1].trim());
        Optional<Tipologia> tipologia = sistema.flatMap(s -> s.tipologia(campi[2].trim()));
        if (tipologia.isEmpty() || campi[3].isBlank()) {
            return null;
        }
        try {
            Colore colore = new Colore(campi[3].trim());
            double l = Double.parseDouble(campi[4].trim());
            double h = Double.parseDouble(campi[5].trim());
            double hf = Double.parseDouble(campi[6].trim());
            int quantita = Integer.parseInt(campi[7].trim());
            if (l <= 0 || h <= 0 || hf < 0 || quantita <= 0) {
                return null;
            }
            // I prezzi (campi 9-10) sono arrivati dopo: i file più vecchi non li hanno e valgono 0.
            Prezzi prezzi = campi.length >= 11
                    ? new Prezzi(prezzo(campi[9]), prezzo(campi[10]))
                    : Prezzi.NESSUNO;
            // Le varianti (campo 11) sono arrivate dopo ancora: senza il campo si usano i profili base.
            Varianti varianti = campi.length == 12
                    ? leggiVarianti(campi[11].trim(), sistema.get())
                    : Varianti.NESSUNA;
            if (varianti == null) {
                return null;   // nome di variante non più nel catalogo: meglio scartare che sbagliare
            }
            return new Serramento(tipologia.get(), colore, new Dimensione(l, h, hf), quantita, prezzi,
                    varianti);
        } catch (IllegalArgumentException malformata) {
            return null;
        }
    }

    /** {@code RUOLO=nome|RUOLO=nome}, vuoto se non c'è nessuna variante scelta. */
    private static String scriviVarianti(Varianti varianti) {
        return varianti.scelte().entrySet().stream()
                .map(scelta -> scelta.getKey().name() + VALORE + scelta.getValue().nome())
                .collect(Collectors.joining(VARIANTI));
    }

    /** L'inverso, ri-risolto contro il sistema; {@code null} se un nome non esiste più. */
    private static Varianti leggiVarianti(String campo, Sistema sistema) {
        if (campo.isEmpty()) {
            return Varianti.NESSUNA;
        }
        Varianti varianti = Varianti.NESSUNA;
        for (String scelta : campo.split("\\" + VARIANTI, -1)) {
            String[] parti = scelta.split(VALORE, 2);
            if (parti.length != 2) {
                return null;
            }
            Categoria ruolo;
            try {
                ruolo = Categoria.valueOf(parti[0].trim());
            } catch (IllegalArgumentException ruoloIgnoto) {
                return null;
            }
            Optional<Variante> variante = sistema.variantiDi(ruolo).stream()
                    .filter(v -> v.nome().equals(parti[1].trim()))
                    .findFirst();
            if (variante.isEmpty()) {
                return null;
            }
            varianti = varianti.con(variante.get());
        }
        return varianti;
    }

    /** Un prezzo scritto nel file: malformato o negativo vale 0 (non impostato), non un errore. */
    private static double prezzo(String campo) {
        try {
            double letto = Double.parseDouble(campo.trim().replace(',', '.'));
            return letto > 0 ? letto : 0;
        } catch (NumberFormatException nonUnNumero) {
            return 0;
        }
    }

    /** Il nome del sistema che contiene questa tipologia, o {@code null} se non è nel catalogo. */
    private String nomeSistema(Tipologia tipologia) {
        return catalogo.sistemaDi(tipologia).map(Sistema::nome).orElse(null);
    }

    private static String togliBom(String riga) {
        return !riga.isEmpty() && riga.charAt(0) == BOM ? riga.substring(1) : riga;
    }
}
