package com.cutcalculator.persistenza;

import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.catalogo.Sistema;
import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Dimensione;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Serramento;
import com.cutcalculator.dominio.Tipologia;

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

/**
 * Archivio su disco degli ordini: carica e salva la lista di {@link Ordine} in un CSV semplice, una
 * riga per {@link Serramento} nel formato {@code ordine;sistema;tipologia;colore;L;H;HF;quantita}.
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
    private static final char BOM = '﻿';

    private final Path file;
    private final Catalogo catalogo;

    public ArchivioOrdini(Path file, Catalogo catalogo) {
        this.file = file;
        this.catalogo = catalogo;
    }

    /** Carica gli ordini dal file; lista vuota se il file non esiste ancora. */
    public List<Ordine> carica() {
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
                        Double.toString(d.HF()), Integer.toString(serramento.quantita())));
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
        if (campi.length != 8) {
            return;
        }
        Serramento serramento = leggiSerramento(campi);
        if (serramento != null) {
            perNome.computeIfAbsent(nome, Ordine::new).aggiungi(serramento);
        }
    }

    /** Ricostruisce un serramento dai campi; {@code null} se tipologia sconosciuta o dati invalidi. */
    private Serramento leggiSerramento(String[] campi) {
        Optional<Tipologia> tipologia = catalogo.sistema(campi[1].trim())
                .flatMap(sistema -> sistema.tipologia(campi[2].trim()));
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
            return new Serramento(tipologia.get(), colore, new Dimensione(l, h, hf), quantita);
        } catch (IllegalArgumentException malformata) {
            return null;
        }
    }

    /** Il nome del sistema che contiene questa tipologia, o {@code null} se non è nel catalogo. */
    private String nomeSistema(Tipologia tipologia) {
        for (Sistema sistema : catalogo.sistemi()) {
            if (sistema.tipologie().contains(tipologia)) {
                return sistema.nome();
            }
        }
        return null;
    }

    private static String togliBom(String riga) {
        return !riga.isEmpty() && riga.charAt(0) == BOM ? riga.substring(1) : riga;
    }
}
