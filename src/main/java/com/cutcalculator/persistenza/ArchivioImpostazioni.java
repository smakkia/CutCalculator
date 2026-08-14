package com.cutcalculator.persistenza;

import com.cutcalculator.app.Unita;
import com.cutcalculator.dominio.Prezzi;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Archivio su disco delle preferenze dell'utente: l'{@link Unita unità di misura} con cui mostrare e
 * leggere le lunghezze e il {@link Prezzi listino} (€/kg dell'alluminio, €/mq del vetro); domani anche
 * soglia di ritaglio e barra standard.
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
    private static final String CHIAVE_PREZZO_BARRE = "prezzo.barre.kg";
    private static final String CHIAVE_PREZZO_VETRO = "prezzo.vetro.mq";

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
        Properties impostazioni = leggi();
        impostazioni.setProperty(CHIAVE_UNITA, unita.name());
        scrivi(impostazioni);
    }

    /**
     * Il listino salvato. Valori mancanti, non numerici o negativi valgono <b>zero</b> (prezzo non
     * impostato): un file scritto male non deve far crollare l'avvio né inventare costi.
     */
    public Prezzi caricaPrezzi() {
        Properties impostazioni = leggi();
        return new Prezzi(numero(impostazioni.getProperty(CHIAVE_PREZZO_BARRE)),
                numero(impostazioni.getProperty(CHIAVE_PREZZO_VETRO)));
    }

    /** Salva il listino, creando le cartelle mancanti. */
    public void salvaPrezzi(Prezzi prezzi) {
        Properties impostazioni = leggi();
        impostazioni.setProperty(CHIAVE_PREZZO_BARRE, String.valueOf(prezzi.alChiloBarre()));
        impostazioni.setProperty(CHIAVE_PREZZO_VETRO, String.valueOf(prezzi.alMqVetro()));
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

    /** Un numero scritto a mano nel file: assente, malformato o negativo → 0 (non impostato). */
    private static double numero(String valore) {
        if (valore == null) {
            return 0;
        }
        try {
            double letto = Double.parseDouble(valore.trim().replace(',', '.'));
            return letto > 0 ? letto : 0;
        } catch (NumberFormatException nonUnNumero) {
            return 0;
        }
    }
}
