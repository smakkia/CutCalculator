package com.cutcalculator.preventivo;

import com.cutcalculator.dominio.Profilo;

import java.util.List;
import java.util.Optional;

/**
 * L'output della Fase 3: la stima del materiale necessario per un ordine, una
 * {@link RigaProfilo riga} per profilo. È di sola quantità (nessun prezzo: il profilo non
 * ne ha). Vetro e accessori entreranno qui quando saranno nel modello.
 */
public record Preventivo(List<RigaProfilo> righe) {

    public Preventivo {
        righe = List.copyOf(righe);
    }

    /** Barre nuove da comprare in totale, su tutti i profili. */
    public int totaleBarreNuove() {
        return righe.stream().mapToInt(RigaProfilo::barreNuove).sum();
    }

    /** Spezzoni di magazzino riutilizzati in totale. */
    public int totaleAvanziUsati() {
        return righe.stream().mapToInt(RigaProfilo::avanziUsati).sum();
    }

    /** Metri lineari di barra nuova ordinati in totale. */
    public double lunghezzaNuovaTotale() {
        return righe.stream().mapToDouble(RigaProfilo::lunghezzaNuova).sum();
    }

    /** Sfrido complessivo del piano, su tutte le barre (nuove e avanzi). */
    public double sfridoTotale() {
        return righe.stream().mapToDouble(RigaProfilo::sfrido).sum();
    }

    /** La riga di un dato profilo, se presente nel preventivo. */
    public Optional<RigaProfilo> rigaDi(Profilo profilo) {
        return righe.stream().filter(riga -> riga.profilo().equals(profilo)).findFirst();
    }
}
