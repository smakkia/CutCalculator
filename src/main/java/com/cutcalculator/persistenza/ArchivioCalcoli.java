package com.cutcalculator.persistenza;

import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Pezzo;
import com.cutcalculator.dominio.Vetro;
import com.cutcalculator.pianificazione.DistintaOrdine;
import com.cutcalculator.pianificazione.EvasioneOrdini;
import com.cutcalculator.pianificazione.QuotaOrdine;
import com.cutcalculator.preventivo.RigaVetro;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Archivio dei <b>risultati dei calcoli</b>: quando un ordine viene calcolato, la sua distinta, il
 * suo preventivo e i suoi vetri finiscono su disco e restano consultabili anche dopo la chiusura.
 * <p>
 * Tre file CSV nella stessa cartella degli altri dati, con il <b>nome dell'ordine come prima
 * colonna</b> — come {@code ordini.csv}:
 * <ul>
 *   <li>{@code calcoli-distinta.csv} — {@code ordine;profilo;colore;lunghezza;taglio;quantita}</li>
 *   <li>{@code calcoli-preventivo.csv} — {@code ordine;profilo;colore;lunghezza;peso;costo}</li>
 *   <li>{@code calcoli-vetri.csv} — {@code ordine;altezza;larghezza;quantita;areaMq;costo}</li>
 * </ul>
 * Il preventivo per ordine è una <b>quota</b> (le barre sono condivise, vedi {@link QuotaOrdine}),
 * quindi qui non ci sono barre né sfrido: quelli hanno senso solo sul piano complessivo.
 * <p>
 * Ogni salvataggio <b>aggiorna</b> invece di sovrascrivere: le righe degli ordini appena calcolati
 * sostituiscono le loro precedenti, quelle degli altri ordini restano dov'erano. Così l'archivio
 * cresce una sessione alla volta e ricalcolare una commessa non ne duplica i documenti.
 * <p>
 * I file si possono aprire anche fuori dall'app (Excel, stampa), ma {@link #carica(String)} li
 * rilegge per mostrare i documenti di un ordine già calcolato senza doverlo ricalcolare.
 */
public final class ArchivioCalcoli {

    private static final String SEP = ";";

    private final Path cartella;

    /** @param cartella dove mettere i tre CSV (la stessa di magazzino e ordini) */
    public ArchivioCalcoli(Path cartella) {
        this.cartella = cartella;
    }

    /** Scrive distinta, preventivo e vetri di ogni ordine appena calcolato. */
    public void salva(EvasioneOrdini evasione) {
        Set<String> ordini = evasione.ordini().stream().map(Ordine::nome).collect(Collectors.toSet());
        aggiorna("calcoli-distinta.csv", ordini, righeDistinta(evasione.distinte()));
        aggiorna("calcoli-preventivo.csv", ordini, righePreventivo(evasione.quote()));
        aggiorna("calcoli-vetri.csv", ordini, righeVetri(evasione.distinte()));
    }

    /**
     * Cancella dai tre file i documenti di un ordine: lo si chiama quando l'ordine viene
     * <b>rimosso o rinominato</b>.
     * <p>
     * Senza, le sue righe resterebbero lì per sempre — i file crescerebbero a ogni commessa chiusa,
     * e soprattutto un <b>futuro ordine con lo stesso nome</b> si vedrebbe mostrare la distinta e il
     * preventivo di quello vecchio, che con lui non c'entrano nulla. I file che non esistono ancora
     * non vengono creati apposta per svuotarli.
     */
    public void dimentica(String ordine) {
        for (String nomeFile : List.of("calcoli-distinta.csv", "calcoli-preventivo.csv",
                "calcoli-vetri.csv")) {
            if (Files.exists(cartella.resolve(nomeFile))) {
                aggiorna(nomeFile, Set.of(ordine), List.of());
            }
        }
    }

    /**
     * Rilegge i documenti di un ordine calcolato. Se non è mai stato calcolato (o i file non ci
     * sono) torna un {@link CalcoloOrdine#vuoto() calcolo vuoto}, non un errore.
     */
    public CalcoloOrdine carica(String ordine) {
        List<CalcoloOrdine.VoceDistinta> distinta = new ArrayList<>();
        for (String[] campi : righeDi("calcoli-distinta.csv", ordine, 6)) {
            distinta.add(new CalcoloOrdine.VoceDistinta(campi[1], campi[2], valore(campi[3]),
                    campi[4], (int) valore(campi[5])));
        }
        List<CalcoloOrdine.VocePreventivo> preventivo = new ArrayList<>();
        for (String[] campi : righeDi("calcoli-preventivo.csv", ordine, 6)) {
            preventivo.add(new CalcoloOrdine.VocePreventivo(campi[1], campi[2], valore(campi[3]),
                    valore(campi[4]), valore(campi[5])));
        }
        List<CalcoloOrdine.VoceVetro> vetri = new ArrayList<>();
        for (String[] campi : righeDi("calcoli-vetri.csv", ordine, 6)) {
            vetri.add(new CalcoloOrdine.VoceVetro(valore(campi[1]), valore(campi[2]),
                    (int) valore(campi[3]), valore(campi[4]), valore(campi[5])));
        }
        return new CalcoloOrdine(ordine, distinta, preventivo, vetri);
    }

    /** Le righe di un file che appartengono a quell'ordine e hanno il numero giusto di campi. */
    private List<String[]> righeDi(String nomeFile, String ordine, int campiAttesi) {
        Path file = cartella.resolve(nomeFile);
        if (!Files.exists(file)) {
            return List.of();
        }
        List<String[]> righe = new ArrayList<>();
        try {
            for (String riga : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] campi = riga.split(SEP, -1);
                if (campi.length == campiAttesi && campi[0].trim().equals(ordine)) {
                    righe.add(campi);
                }
            }
        } catch (IOException lettura) {
            throw new UncheckedIOException("Impossibile leggere i calcoli da " + file, lettura);
        }
        return righe;
    }

    /** Un numero del file; una riga scritta male vale 0 invece di far saltare tutta la lettura. */
    private static double valore(String campo) {
        try {
            return Double.parseDouble(campo.trim());
        } catch (NumberFormatException nonUnNumero) {
            return 0;
        }
    }

    /** Una riga per gruppo di pezzi uguali: chi taglia non vuole 8 righe identiche. */
    private static List<String> righeDistinta(List<DistintaOrdine> distinte) {
        List<String> righe = new ArrayList<>();
        for (DistintaOrdine voce : distinte) {
            // Pezzi uguali (materiale + lunghezza + taglio) contati insieme, in ordine di comparsa.
            Map<String, Integer> conteggio = new LinkedHashMap<>();
            Map<String, Pezzo> esempio = new LinkedHashMap<>();
            for (Pezzo pezzo : voce.distinta().pezzi()) {
                String chiave = pezzo.profilo().codice() + "|" + pezzo.colore().nome()
                        + "|" + pezzo.lunghezza() + "|" + pezzo.tipoTaglio();
                conteggio.merge(chiave, 1, Integer::sum);
                esempio.putIfAbsent(chiave, pezzo);
            }
            conteggio.forEach((chiave, quantita) -> {
                Pezzo p = esempio.get(chiave);
                righe.add(String.join(SEP, voce.ordine(), p.profilo().codice(), p.colore().nome(),
                        numero(p.lunghezza()), p.tipoTaglio().name(), String.valueOf(quantita)));
            });
        }
        return righe;
    }

    /** Una riga per materiale, con la quota di peso e costo dell'ordine. */
    private static List<String> righePreventivo(List<QuotaOrdine> quote) {
        List<String> righe = new ArrayList<>();
        for (QuotaOrdine quota : quote) {
            for (QuotaOrdine.Riga riga : quota.righe()) {
                righe.add(String.join(SEP, quota.ordine(), riga.profilo().codice(),
                        riga.colore().nome(), numero(riga.lunghezza()), numero(riga.peso()),
                        numero(riga.costo())));
            }
        }
        return righe;
    }

    /** Una riga per misura di lastra: il vetro non si condivide, quindi sono numeri esatti. */
    private static List<String> righeVetri(List<DistintaOrdine> distinte) {
        List<String> righe = new ArrayList<>();
        for (DistintaOrdine voce : distinte) {
            for (RigaVetro riga : aggregaPerMisura(voce.distinta().vetri())) {
                righe.add(String.join(SEP, voce.ordine(), numero(riga.lunghezza()),
                        numero(riga.larghezza()), String.valueOf(riga.quantita()),
                        numero(riga.areaTotaleMq()), numero(riga.costo())));
            }
        }
        return righe;
    }

    /** Lastre della stessa misura (e stesso prezzo) in una riga sola, con quantità e costo sommati. */
    private static List<RigaVetro> aggregaPerMisura(List<Vetro> vetri) {
        Map<String, RigaVetro> perMisura = new LinkedHashMap<>();
        for (Vetro vetro : vetri) {
            perMisura.merge(
                    vetro.lunghezza() + "x" + vetro.larghezza() + "@" + vetro.prezzi().alMqVetro(),
                    new RigaVetro(vetro.lunghezza(), vetro.larghezza(), vetro.quantita(), vetro.costo()),
                    (vecchia, nuova) -> new RigaVetro(vecchia.lunghezza(), vecchia.larghezza(),
                            vecchia.quantita() + nuova.quantita(), vecchia.costo() + nuova.costo()));
        }
        return new ArrayList<>(perMisura.values());
    }

    /**
     * Riscrive il file tenendo le righe degli <b>altri</b> ordini e sostituendo quelle degli ordini
     * appena calcolati. Delle righe già presenti guarda solo il primo campo (il nome dell'ordine):
     * non le interpreta, quindi un file ritoccato a mano non si perde per strada.
     */
    private void aggiorna(String nomeFile, Set<String> ordiniRicalcolati, List<String> nuove) {
        Path file = cartella.resolve(nomeFile);
        List<String> righe = new ArrayList<>();
        try {
            if (Files.exists(file)) {
                for (String riga : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String ordine = riga.split(SEP, -1)[0].trim();
                    if (!riga.isBlank() && !ordiniRicalcolati.contains(ordine)) {
                        righe.add(riga);
                    }
                }
            }
            righe.addAll(nuove);
            Files.createDirectories(cartella);
            Files.write(file, righe, StandardCharsets.UTF_8);
        } catch (IOException errore) {
            throw new UncheckedIOException("Impossibile salvare i calcoli su " + file, errore);
        }
    }

    /** Numeri senza notazione scientifica e con il punto decimale, come negli altri CSV. */
    private static String numero(double valore) {
        return String.format(Locale.ROOT, "%.3f", valore);
    }
}
