package com.cutcalculator.catalogo;

import com.cutcalculator.dominio.Tipologia;

import java.util.HashMap;
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
    /**
     * Indice tipologia → sistema, costruito una volta sola. Senza, {@link #sistemaDi} doveva
     * scorrere i sistemi con un {@code contains}, cioè confrontare la {@link Tipologia} <b>regola
     * per regola e formula per formula</b> con tutte quelle del catalogo — e lo si chiama per ogni
     * serramento a ogni salvataggio automatico e per ogni cella della colonna Sistema.
     */
    private final Map<Tipologia, Sistema> sistemaPerTipologia;

    public Catalogo(List<Sistema> sistemi) {
        this.sistemi = List.copyOf(sistemi);
        Map<Tipologia, Sistema> indice = new HashMap<>();
        for (Sistema sistema : this.sistemi) {
            // putIfAbsent: se due sistemi avessero la stessa identica ricetta vince il primo,
            // esattamente come faceva il findFirst() di prima.
            sistema.tipologie().forEach(tipologia -> indice.putIfAbsent(tipologia, sistema));
        }
        this.sistemaPerTipologia = Map.copyOf(indice);
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

    /**
     * Il sistema a cui appartiene una tipologia: serve a risalire dalla ricetta alla serie
     * commerciale (per mostrarla o per salvarla su file), visto che la {@link Tipologia} non
     * conosce il proprio sistema.
     */
    public Optional<Sistema> sistemaDi(Tipologia tipologia) {
        return Optional.ofNullable(sistemaPerTipologia.get(tipologia));
    }
}
