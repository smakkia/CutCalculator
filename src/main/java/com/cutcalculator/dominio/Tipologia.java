package com.cutcalculator.dominio;

import java.util.List;

/**
 * La "ricetta" di un tipo di serramento: un nome (es. "Finestra a 2 ante") e l'elenco
 * completo delle righe di taglio (la scheda del Gruppo E).
 * <p>
 * È pura descrizione: <b>non</b> genera i pezzi. La logica "applica le formule a una
 * misura L×H → lista di pezzi" vivrà nel pacchetto {@code formule} (GeneratoreDistinta).
 * <p>
 * Vetro e accessori verranno aggiunti più avanti.
 */
public record Tipologia(String nome, List<RegolaTaglio> regole) {

    /** Copia difensiva: le regole diventano immutabili una volta costruita la tipologia. */
    public Tipologia {
        regole = List.copyOf(regole);
    }

    /**
     * {@code true} se qualche regola usa l'altezza parziale HF (coefficiente {@code cHF} non nullo),
     * come nelle porte: la UI deve allora chiedere anche HF, non solo L e H.
     */
    public boolean usaHF() {
        return regole.stream().anyMatch(r -> r.formula().cHF() != 0);
    }
}
