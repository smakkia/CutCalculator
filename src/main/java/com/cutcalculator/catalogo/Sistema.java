package com.cutcalculator.catalogo;

import com.cutcalculator.dominio.Tipologia;

import java.util.List;
import java.util.Optional;

/**
 * Un sistema di profili (es. "RX 700"): una serie commerciale con la sua
 * {@link FamigliaSistema famiglia} e l'elenco delle {@link Tipologia tipologie}
 * (finestre/porte) che ci si possono costruire, ognuna con le sue formule di taglio.
 */
public record Sistema(String nome, FamigliaSistema famiglia, List<Tipologia> tipologie) {

    public Sistema {
        tipologie = List.copyOf(tipologie);
    }

    /** La tipologia con questo nome, se il sistema la prevede. */
    public Optional<Tipologia> tipologia(String nome) {
        return tipologie.stream().filter(t -> t.nome().equals(nome)).findFirst();
    }
}
