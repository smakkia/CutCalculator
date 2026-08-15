package com.cutcalculator.persistenza;

import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.dominio.Avanzo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * La fotografia dell'<b>ultimo calcolo globale</b>, tenuta da parte per poterlo annullare: quali
 * ordini vi hanno partecipato e <b>com'era il magazzino prima</b>.
 * <p>
 * Serve al "ripristina ordine". Il calcolo consuma gli avanzi usati e fa rientrare i ritagli sopra
 * soglia: senza sapere da dove si era partiti, quell'effetto non è ricostruibile — i ritagli nascono
 * dal piano <b>complessivo</b>, non da un singolo ordine, e tornare indietro "a occhio" lascerebbe il
 * magazzino diverso dal vero. Qui invece il ripristino è esatto: si riscrive lo stato di prima.
 * <p>
 * Tre scelte, tutte volute:
 * <ul>
 *   <li><b>Solo l'ultimo calcolo.</b> Ogni calcolo sovrascrive la fotografia precedente. Annullare
 *       un calcolo vecchio non avrebbe senso: il magazzino nel frattempo è stato mosso da quelli
 *       dopo, e riscriverci sopra uno stato antico cancellerebbe il loro effetto.</li>
 *   <li><b>Tutto il gruppo.</b> Si salvano <i>tutti</i> gli ordini del calcolo, perché le barre erano
 *       condivise: non esiste "la parte di magazzino" di un ordine solo, e ripristinarne uno solo
 *       sarebbe una stima, non un annullamento.</li>
 *   <li><b>Il magazzino nel formato di {@link ArchivioMagazzino}</b>, di cui questa classe si serve
 *       davvero invece di ricopiarne il parsing: stesso CSV, stesse regole, stessa gestione delle
 *       righe malformate.</li>
 * </ul>
 * L'elenco degli ordini sta in un file a parte, un nome per riga. È lui a dire se c'è qualcosa da
 * annullare: un calcolo ha sempre almeno un ordine, mentre il magazzino di prima può essere
 * legittimamente vuoto.
 */
public final class ArchivioRipristino {

    private static final String FILE_ORDINI = "ripristino-ordini.csv";
    private static final String FILE_MAGAZZINO = "ripristino-magazzino.csv";
    private static final char BOM = '\uFEFF';

    private final Path fileOrdini;
    private final ArchivioMagazzino magazzinoPrima;

    /** @param cartella dove mettere i due file (la stessa degli altri dati) */
    public ArchivioRipristino(Path cartella, Catalogo catalogo) {
        this.fileOrdini = cartella.resolve(FILE_ORDINI);
        this.magazzinoPrima = new ArchivioMagazzino(cartella.resolve(FILE_MAGAZZINO), catalogo);
    }

    /**
     * Registra un calcolo appena fatto, <b>sostituendo</b> la fotografia precedente: da qui in avanti
     * è questo il calcolo annullabile.
     *
     * @param ordini    i nomi degli ordini che vi hanno partecipato
     * @param magazzino il magazzino <b>com'era prima</b> che il calcolo lo consumasse
     */
    public void salva(List<String> ordini, List<Avanzo> magazzino) {
        try {
            Path cartella = fileOrdini.getParent();
            if (cartella != null) {
                Files.createDirectories(cartella);
            }
            Files.write(fileOrdini, ordini, StandardCharsets.UTF_8);
        } catch (IOException scrittura) {
            throw new UncheckedIOException(
                    "Impossibile salvare il punto di ripristino su " + fileOrdini, scrittura);
        }
        magazzinoPrima.salva(magazzino);
    }

    /** Il calcolo annullabile, o vuoto se non ce n'è nessuno (mai calcolato, o già ripristinato). */
    public Optional<Ripristino> carica() {
        if (!Files.exists(fileOrdini)) {
            return Optional.empty();
        }
        List<String> ordini = new ArrayList<>();
        try {
            for (String riga : Files.readAllLines(fileOrdini, StandardCharsets.UTF_8)) {
                String nome = togliBom(riga).trim();
                if (!nome.isEmpty() && !nome.startsWith("#")) {
                    ordini.add(nome);
                }
            }
        } catch (IOException lettura) {
            throw new UncheckedIOException(
                    "Impossibile leggere il punto di ripristino da " + fileOrdini, lettura);
        }
        return ordini.isEmpty() ? Optional.empty()
                : Optional.of(new Ripristino(ordini, magazzinoPrima.carica()));
    }

    /**
     * Dimentica il calcolo annullabile: lo si chiama <b>dopo</b> averlo ripristinato, perché a quel
     * punto non c'è più niente da annullare e lasciare i file inviterebbe a farlo due volte.
     */
    public void cancella() {
        try {
            Files.deleteIfExists(fileOrdini);
            Files.deleteIfExists(fileOrdini.resolveSibling(FILE_MAGAZZINO));
        } catch (IOException cancellazione) {
            throw new UncheckedIOException(
                    "Impossibile cancellare il punto di ripristino " + fileOrdini, cancellazione);
        }
    }

    private static String togliBom(String riga) {
        return !riga.isEmpty() && riga.charAt(0) == BOM ? riga.substring(1) : riga;
    }

    /**
     * Un calcolo che si può annullare: chi c'era dentro e a cosa va riportato il magazzino.
     *
     * @param ordini    i nomi degli ordini del calcolo (tornano tutti "da calcolare")
     * @param magazzino lo stato a cui riportare il magazzino
     */
    public record Ripristino(List<String> ordini, List<Avanzo> magazzino) {

        public Ripristino {
            ordini = List.copyOf(ordini);
            magazzino = List.copyOf(magazzino);
        }

        /** {@code true} se quest'ordine ha partecipato al calcolo annullabile. */
        public boolean contiene(String ordine) {
            return ordini.contains(ordine);
        }
    }
}
