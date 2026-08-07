package com.cutcalculator.catalogo;

import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.dominio.RegolaTaglio;
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

    /**
     * I profili distinti del sistema, presi dalle regole di tutte le sue tipologie: sono quelli
     * fra cui l'utente sceglie quando dichiara un avanzo a magazzino.
     */
    public List<Profilo> profili() {
        return tipologie.stream()
                .flatMap(t -> t.regole().stream())
                .map(RegolaTaglio::profilo)
                .distinct()
                .toList();
    }
}
