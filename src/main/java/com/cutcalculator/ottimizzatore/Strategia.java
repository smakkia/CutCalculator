package com.cutcalculator.ottimizzatore;

import java.util.function.Supplier;

/**
 * Le euristiche di taglio fra cui l'utente può scegliere, con quella <b>predefinita</b>
 * ({@link #MULTI_START}). È il registro che mancava: le implementazioni di {@link Ottimizzatore}
 * esistevano già tutte, ma nessuno le istanziava e si finiva sempre e solo su
 * {@link BestFitDecreasing}.
 * <p>
 * Sta qui e non in {@code app} perché conosce le classi concrete, che sono sue vicine di package:
 * chi la usa (controller, UI, impostazioni) ne vede solo il nome e {@link #crea()}.
 * <p>
 * È un'<b>impostazione</b>, come l'unità di misura: si sceglie una volta, si persiste e vale per i
 * calcoli successivi. Cambiarla non tocca né i dati né i calcoli già fatti — cambia il piano del
 * <i>prossimo</i>.
 */
public enum Strategia {

    MULTI_START("Multi-start casuale",
            "prova 50 piani con i pezzi in ordine diverso e tiene il migliore (meno barre nuove,"
                    + " poi meno sfrido sparso). Il piu' lento, ma non puo' venire peggio degli altri",
            MultiStartCasuale::new),

    MIGLIOR_INCASTRO("Miglior incastro (best-fit)",
            "un piano solo, senza tentativi: ogni pezzo va nella barra che resta piu' piena."
                    + " Gli avanzi, essendo corti, tendono a essere riusati per primi",
            BestFitDecreasing::new),

    PRIMA_CHE_ENTRA("Prima barra che entra (first-fit)",
            "piu' semplice e prevedibile: il pezzo va nella prima barra aperta in cui ci sta."
                    + " Di solito spreca un po' di piu'",
            FirstFitDecreasing::new),

    SVUOTA_MAGAZZINO("Svuota magazzino",
            "completa prima gli avanzi: apre una barra nuova solo quando nessuno spezzone basta."
                    + " Libera la rastrelliera, al prezzo di qualche mm di sfrido in piu'",
            SvuotaMagazzino::new);

    /**
     * L'euristica usata se l'utente non ne sceglie una: il <b>multi-start</b> (scelta dell'utente,
     * 2026-08-15). Non può fare peggio del best-fit — parte proprio da quel piano e lo tiene se
     * nessun tentativo casuale lo batte — e sull'ordine reale l'ha battuto. Costa ~4× il tempo, che
     * su commesse di questa taglia non si vede.
     */
    public static final Strategia PREDEFINITA = MULTI_START;

    private final String nome;
    private final String spiegazione;
    private final Supplier<Ottimizzatore> costruttore;

    Strategia(String nome, String spiegazione, Supplier<Ottimizzatore> costruttore) {
        this.nome = nome;
        this.spiegazione = spiegazione;
        this.costruttore = costruttore;
    }

    /** Il nome da mostrare in un menu: {@code "Miglior incastro (best-fit)"}. */
    public String nome() {
        return nome;
    }

    /** Una riga che dice cosa cambia a sceglierla, per chi non conosce le euristiche di taglio. */
    public String spiegazione() {
        return spiegazione;
    }

    /** Nome e spiegazione insieme, per gli elenchi che hanno spazio (il menu della CLI). */
    public String descrizione() {
        return nome + " - " + spiegazione;
    }

    /** Un ottimizzatore pronto all'uso. Sono senza stato: se ne crea uno per calcolo. */
    public Ottimizzatore crea() {
        return costruttore.get();
    }

    /**
     * La strategia con questo nome ({@code MIGLIOR_INCASTRO}, ...), o la {@link #PREDEFINITA} se il
     * testo è assente o sconosciuto: un file di impostazioni corretto a mano non deve far crashare
     * l'app (stessa regola di {@code Unita.daNome}).
     */
    public static Strategia daNome(String nome) {
        if (nome == null) {
            return PREDEFINITA;
        }
        for (Strategia strategia : values()) {
            if (strategia.name().equalsIgnoreCase(nome.trim())) {
                return strategia;
            }
        }
        return PREDEFINITA;
    }
}
