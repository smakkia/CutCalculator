package com.cutcalculator.dominio;

import java.util.Locale;

/**
 * Il colore/finitura di un profilo: parte dell'identita fisica di ogni barra e di ogni pezzo.
 * Non e' un dettaglio cosmetico ma un <b>vincolo di taglio</b>: da una barra bianca non si ricava
 * un pezzo bronzo. Per questo il colore fa da <b>co-chiave</b> accanto al {@link Profilo}, e la
 * coppia (profilo, colore) e' il {@link Materiale} con cui si raggruppa e si incastra.
 * <p>
 * Il nome e' testo libero (un nome commerciale o un codice RAL), ma viene <b>normalizzato</b>
 * (spazi ridotti, MAIUSCOLO) cosi' {@code "bianco"}, {@code "Bianco"} e {@code " BIANCO "} sono lo
 * stesso colore: niente doppioni per una svista di battitura.
 */
public record Colore(String nome) {

    public Colore {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Il colore non puo' essere vuoto");
        }
        nome = nome.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
