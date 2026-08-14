package com.cutcalculator.dominio;

import java.util.List;

/**
 * La "ricetta" di un tipo di serramento: un nome (es. "Finestra a 2 ante"), l'elenco completo
 * delle righe di taglio (la scheda del Gruppo E) e le righe del <b>vetro</b>.
 * <p>
 * È pura descrizione: <b>non</b> genera i pezzi. La logica "applica le formule a una
 * misura L×H → lista di pezzi" vive nel pacchetto {@code formule} (GeneratoreDistinta).
 * <p>
 * Le {@link RegolaVetro} sono separate dalle {@link RegolaTaglio} perché il vetro non è un'asta:
 * non si taglia da una barra e non passa dall'ottimizzatore 1D, si dimensiona e si somma per area.
 * Molte tipologie non ne hanno ancora (le schede non riportano la quota del vetro): in quel caso
 * la lista è vuota e si usa il costruttore a due argomenti.
 * <p>
 * Gli accessori verranno aggiunti più avanti.
 */
public record Tipologia(String nome, List<RegolaTaglio> regole, List<RegolaVetro> regoleVetro) {

    /** Copia difensiva: le regole diventano immutabili una volta costruita la tipologia. */
    public Tipologia {
        regole = List.copyOf(regole);
        regoleVetro = List.copyOf(regoleVetro);
    }

    /** Tipologia di cui non si conoscono (ancora) le quote del vetro: solo righe di taglio. */
    public Tipologia(String nome, List<RegolaTaglio> regole) {
        this(nome, regole, List.of());
    }

    /**
     * {@code true} se qualche regola usa l'altezza parziale HF (coefficiente {@code cHF} non nullo),
     * come nelle porte: la UI deve allora chiedere anche HF, non solo L e H.
     */
    public boolean usaHF() {
        return regole.stream().anyMatch(r -> r.formula().cHF() != 0)
                || regoleVetro.stream().anyMatch(r -> r.formulaLunghezza().cHF() != 0
                        || r.formulaLarghezza().cHF() != 0);
    }

    /** {@code true} se della tipologia si conoscono le quote del vetro. */
    public boolean haVetro() {
        return !regoleVetro.isEmpty();
    }
}
