package com.cutcalculator.persistenza;

import com.cutcalculator.app.Unita;
import com.cutcalculator.ottimizzatore.Strategia;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Archivio su disco delle preferenze dell'utente — l'{@link Unita unità di misura} con cui mostrare e
 * leggere le lunghezze e la {@link Strategia euristica di taglio} da usare nei calcoli; domani anche
 * soglia di ritaglio e barra standard. I <b>prezzi</b>
 * qui non ci sono: non sono una preferenza, si dichiarano su ogni serramento.
 * <p>
 * Formato {@code properties} (una riga {@code chiave=valore}), UTF-8: leggibile e correggibile a
 * mano. File assente, illeggibile o con un valore sconosciuto non sono un errore: si torna
 * semplicemente al valore predefinito, come per gli altri archivi.
 * <p>
 * Ogni {@code salva*} riscrive il file <b>preservando le altre chiavi</b>: le impostazioni sono
 * indipendenti fra loro, cambiare l'unità non deve azzerare i prezzi.
 */
public final class ArchivioImpostazioni {

    private static final String CHIAVE_UNITA = "unita";
    private static final String CHIAVE_STRATEGIA = "strategia";

    private final Path file;

    public ArchivioImpostazioni(Path file) {
        this.file = file;
    }

    /** L'unità salvata, o {@link Unita#PREDEFINITA} se non c'è o non si capisce. */
    public Unita caricaUnita() {
        return Unita.daNome(leggi().getProperty(CHIAVE_UNITA));
    }

    /** Salva l'unità scelta, creando le cartelle mancanti. */
    public void salvaUnita(Unita unita) {
        salvaChiave(CHIAVE_UNITA, unita.name());
    }

    /** L'euristica di taglio salvata, o la {@link Strategia#PREDEFINITA} se non c'è o non si capisce. */
    public Strategia caricaStrategia() {
        return Strategia.daNome(leggi().getProperty(CHIAVE_STRATEGIA));
    }

    /** Salva l'euristica scelta. */
    public void salvaStrategia(Strategia strategia) {
        salvaChiave(CHIAVE_STRATEGIA, strategia.name());
    }

    /** Scrive una preferenza <b>preservando le altre</b>: sono indipendenti fra loro. */
    private void salvaChiave(String chiave, String valore) {
        Properties impostazioni = leggi();
        impostazioni.setProperty(chiave, valore);
        scrivi(impostazioni);
    }

    /** Le impostazioni su disco, o vuote se il file manca o è illeggibile. */
    private Properties leggi() {
        Properties impostazioni = new Properties();
        if (!Files.exists(file)) {
            return impostazioni;
        }
        try (Reader lettore = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            impostazioni.load(lettore);
        } catch (IOException | IllegalArgumentException illeggibile) {
            return new Properties();
        }
        return impostazioni;
    }

    private void scrivi(Properties impostazioni) {
        try {
            Path cartella = file.getParent();
            if (cartella != null) {
                Files.createDirectories(cartella);
            }
            try (Writer scrittore = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                impostazioni.store(scrittore, "Impostazioni di CutCalculator");
            }
        } catch (IOException scrittura) {
            throw new java.io.UncheckedIOException(
                    "Impossibile salvare le impostazioni su " + file, scrittura);
        }
    }

}
