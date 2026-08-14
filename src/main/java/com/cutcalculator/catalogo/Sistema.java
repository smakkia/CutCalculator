package com.cutcalculator.catalogo;

import com.cutcalculator.dominio.Categoria;
import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.dominio.RegolaTaglio;
import com.cutcalculator.dominio.Tipologia;
import com.cutcalculator.dominio.Variante;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Un sistema di profili (es. "RX 700"): una serie commerciale con la sua
 * {@link FamigliaSistema famiglia} e l'elenco delle {@link Tipologia tipologie}
 * (finestre/porte) che ci si possono costruire, ognuna con le sue formule di taglio.
 */
public record Sistema(String nome, FamigliaSistema famiglia, List<Tipologia> tipologie,
        Map<Categoria, List<Variante>> varianti) {

    public Sistema {
        tipologie = List.copyOf(tipologie);
        // EnumMap: i ruoli escono nell'ordine dell'enum (dal telaio verso l'interno), che è anche
        // l'ordine in cui ha senso mostrarli e sceglierli. Si riempie a mano perché il costruttore
        // EnumMap(Map) rifiuta le mappe vuote (non saprebbe dedurre il tipo delle chiavi).
        Map<Categoria, List<Variante>> perRuolo = new EnumMap<>(Categoria.class);
        varianti.forEach((ruolo, elenco) -> perRuolo.put(ruolo, List.copyOf(elenco)));
        varianti = Collections.unmodifiableMap(perRuolo);
    }

    /** Sistema senza varianti dichiarate: si taglia sempre il profilo base delle tipologie. */
    public Sistema(String nome, FamigliaSistema famiglia, List<Tipologia> tipologie) {
        this(nome, famiglia, tipologie, Map.of());
    }

    /**
     * Le varianti fra cui si può scegliere per un ruolo, la prima delle quali è la <b>base</b>
     * (quella già cablata nelle tipologie). Vuota = quel ruolo non ha alternative, e la UI non deve
     * mostrare nulla.
     */
    public List<Variante> variantiDi(Categoria ruolo) {
        return varianti.getOrDefault(ruolo, List.of());
    }

    /** I ruoli che hanno davvero più di una variante: quelli da chiedere all'utente. */
    public List<Categoria> ruoliConScelta() {
        return varianti.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .toList();
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
        return Stream.concat(
                        tipologie.stream().flatMap(t -> t.regole().stream()).map(RegolaTaglio::profilo),
                        varianti.values().stream().flatMap(List::stream).map(Variante::profilo))
                .distinct()
                .toList();
    }
}
