package com.cutcalculator.persistenza;

import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.catalogo.Sistema;
import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.dominio.RegolaTaglio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Archivio su disco del magazzino: carica e salva la lista di {@link Avanzo} in un CSV
 * semplice, una riga per avanzo nel formato {@code codiceProfilo;colore;lunghezza;quantita}.
 * <p>
 * Del profilo si memorizza solo il <b>codice</b>: al caricamento viene ri-risolto contro il
 * {@link Catalogo} (i profili "veri" vengono già di lì), così il file resta corto e i profili
 * restano canonici. Il colore è testo libero (viene normalizzato dal {@link Colore}). Una riga con
 * codice non nel catalogo, colore vuoto, malformata, vuota o che inizia con {@code #} (commento)
 * viene semplicemente ignorata: un file corretto a mano non fa crashare l'app (le vecchie righe a 3
 * campi, senza colore, ricadono qui). Il file è UTF-8 e un eventuale BOM in testa viene tolto.
 */
public final class ArchivioMagazzino {

    private static final String SEP = ";";
    private static final char BOM = '\uFEFF';

    private final Path file;
    private final Map<String, Profilo> profiliPerCodice;

    public ArchivioMagazzino(Path file, Catalogo catalogo) {
        this.file = file;
        this.profiliPerCodice = indicizzaProfili(catalogo);
    }

    /** Carica gli avanzi dal file; lista vuota se il file non esiste ancora (primo avvio). */
    public List<Avanzo> carica() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        List<Avanzo> avanzi = new ArrayList<>();
        try {
            for (String riga : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                Avanzo avanzo = leggiRiga(riga);
                if (avanzo != null) {
                    avanzi.add(avanzo);
                }
            }
        } catch (IOException lettura) {
            throw new UncheckedIOException("Impossibile leggere il magazzino da " + file, lettura);
        }
        return avanzi;
    }

    /** Salva l'intero magazzino sovrascrivendo il file (crea le cartelle mancanti). */
    public void salva(List<Avanzo> magazzino) {
        List<String> righe = new ArrayList<>();
        for (Avanzo a : magazzino) {
            righe.add(a.profilo().codice() + SEP + a.colore().nome() + SEP + a.lunghezza() + SEP + a.quantita());
        }
        try {
            Path cartella = file.getParent();
            if (cartella != null) {
                Files.createDirectories(cartella);
            }
            Files.write(file, righe, StandardCharsets.UTF_8);
        } catch (IOException scrittura) {
            throw new UncheckedIOException("Impossibile salvare il magazzino su " + file, scrittura);
        }
    }

    /** Interpreta una riga; {@code null} se vuota, commento, malformata, profilo o colore mancante. */
    private Avanzo leggiRiga(String riga) {
        String pulita = togliBom(riga).trim();
        if (pulita.isEmpty() || pulita.startsWith("#")) {
            return null;
        }
        String[] campi = pulita.split(SEP);
        if (campi.length != 4) {
            return null;
        }
        Profilo profilo = profiliPerCodice.get(campi[0].trim());
        if (profilo == null || campi[1].isBlank()) {
            return null;
        }
        try {
            Colore colore = new Colore(campi[1].trim());
            double lunghezza = Double.parseDouble(campi[2].trim());
            int quantita = Integer.parseInt(campi[3].trim());
            return lunghezza > 0 && quantita > 0 ? new Avanzo(profilo, colore, lunghezza, quantita) : null;
        } catch (NumberFormatException malformata) {
            return null;
        }
    }

    private static String togliBom(String riga) {
        return !riga.isEmpty() && riga.charAt(0) == BOM ? riga.substring(1) : riga;
    }

    /** Mappa codice → profilo con tutti i profili distinti del catalogo (primo che vince). */
    private static Map<String, Profilo> indicizzaProfili(Catalogo catalogo) {
        Map<String, Profilo> perCodice = new LinkedHashMap<>();
        for (Sistema sistema : catalogo.sistemi()) {
            sistema.tipologie().stream()
                    .flatMap(t -> t.regole().stream())
                    .map(RegolaTaglio::profilo)
                    .forEach(p -> perCodice.putIfAbsent(p.codice(), p));
        }
        return perCodice;
    }
}
