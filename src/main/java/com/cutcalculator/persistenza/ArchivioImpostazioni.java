package com.cutcalculator.persistenza;

import com.cutcalculator.app.Unita;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Archivio su disco delle preferenze dell'utente — oggi solo l'{@link Unita unità di misura} con
 * cui mostrare e leggere le lunghezze, domani anche soglia di ritaglio e barra standard.
 * <p>
 * Formato {@code properties} (una riga {@code chiave=valore}), UTF-8: leggibile e correggibile a
 * mano. File assente, illeggibile o con un valore sconosciuto non sono un errore: si torna
 * semplicemente al valore predefinito, come per gli altri archivi.
 */
public final class ArchivioImpostazioni {

    private static final String CHIAVE_UNITA = "unita";

    private final Path file;

    public ArchivioImpostazioni(Path file) {
        this.file = file;
    }

    /** L'unità salvata, o {@link Unita#PREDEFINITA} se non c'è o non si capisce. */
    public Unita caricaUnita() {
        if (!Files.exists(file)) {
            return Unita.PREDEFINITA;
        }
        Properties impostazioni = new Properties();
        try (Reader lettore = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            impostazioni.load(lettore);
        } catch (IOException | IllegalArgumentException illeggibile) {
            return Unita.PREDEFINITA;
        }
        return Unita.daNome(impostazioni.getProperty(CHIAVE_UNITA));
    }

    /** Salva l'unità scelta, creando le cartelle mancanti. */
    public void salvaUnita(Unita unita) {
        Properties impostazioni = new Properties();
        impostazioni.setProperty(CHIAVE_UNITA, unita.name());
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
