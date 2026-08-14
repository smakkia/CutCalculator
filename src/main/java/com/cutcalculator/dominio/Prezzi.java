package com.cutcalculator.dominio;

/**
 * Il <b>listino</b> dell'utente: quanto costa il materiale, nelle due unità con cui lo si compra
 * davvero — l'alluminio <b>a peso</b> (€/kg) e il vetro <b>a superficie</b> (€/mq).
 * <p>
 * Non è un dato di catalogo ma un'impostazione: cambia col fornitore e col mercato, quindi la
 * inserisce l'utente e viene persistita insieme alle altre preferenze. Il {@link Profilo} può però
 * portare un proprio {@code prezzoAlChilo}: se c'è, vince su questo listino generale (vedi
 * {@link #alChiloDi(Profilo)}), così un profilo particolarmente caro può avere il suo prezzo senza
 * costringere a riscrivere tutto il resto.
 * <p>
 * Un prezzo a <b>zero</b> significa "non impostato": i costi verranno zero, non sbagliati.
 *
 * @param alChiloBarre prezzo dell'alluminio in €/kg, valido per i profili che non ne hanno uno proprio
 * @param alMqVetro    prezzo del vetro in €/mq
 */
public record Prezzi(double alChiloBarre, double alMqVetro) {

    /** Listino vuoto: nessun prezzo impostato, tutti i costi vengono zero. */
    public static final Prezzi NESSUNO = new Prezzi(0, 0);

    public Prezzi {
        if (alChiloBarre < 0 || alMqVetro < 0) {
            throw new IllegalArgumentException("I prezzi non possono essere negativi: "
                    + alChiloBarre + " EUR/kg, " + alMqVetro + " EUR/mq");
        }
    }

    /** {@code true} se c'è almeno un prezzo impostato: sotto questa condizione i costi hanno senso. */
    public boolean impostati() {
        return alChiloBarre > 0 || alMqVetro > 0;
    }

    /** Il €/kg da applicare a un profilo: il suo, se ce l'ha, altrimenti quello generale. */
    public double alChiloDi(Profilo profilo) {
        return profilo.prezzoAlChilo() > 0 ? profilo.prezzoAlChilo() : alChiloBarre;
    }

    /** Il costo (€) di {@code kg} chilogrammi di un dato profilo. */
    public double costoBarre(Profilo profilo, double kg) {
        return kg * alChiloDi(profilo);
    }

    /** Il costo (€) di {@code mq} metri quadri di vetro. */
    public double costoVetro(double mq) {
        return mq * alMqVetro;
    }
}
