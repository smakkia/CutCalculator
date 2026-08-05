package com.cutcalculator.catalogo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Il registro di tutti i sistemi di profili disponibili, con il raggruppamento per
 * {@link FamigliaSistema famiglia}. Oggi i sistemi sono classi scritte a mano
 * ({@code CatalogoRX700}, ...); in futuro questi dati arriveranno da file/DB.
 */
public class Catalogo {

    private final List<Sistema> sistemi;

    public Catalogo(List<Sistema> sistemi) {
        this.sistemi = List.copyOf(sistemi);
    }

    /** Il catalogo completo con tutti i sistemi finora trascritti. */
    public static Catalogo completo() {
        return new Catalogo(List.of(
                CatalogoRX700.sistema(),
                CatalogoCX700.sistema(),
                CatalogoSX110.sistema(),
                CatalogoSX120.sistema()));
    }

    public List<Sistema> sistemi() {
        return sistemi;
    }

    /** I sistemi raggruppati per famiglia, nell'ordine in cui compaiono. */
    public Map<FamigliaSistema, List<Sistema>> perFamiglia() {
        return sistemi.stream().collect(Collectors.groupingBy(
                Sistema::famiglia, () -> new java.util.EnumMap<>(FamigliaSistema.class), Collectors.toList()));
    }

    /** Il sistema con questo nome, se presente nel catalogo. */
    public Optional<Sistema> sistema(String nome) {
        return sistemi.stream().filter(s -> s.nome().equals(nome)).findFirst();
    }
}
