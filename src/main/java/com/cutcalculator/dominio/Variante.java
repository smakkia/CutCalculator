package com.cutcalculator.dominio;

/**
 * Una variante di profilo per un <b>ruolo</b> del serramento: lo stesso telaio esiste ad L o a Z,
 * piccolo o maggiorato, e la stessa anta piccola o maggiorata.
 * <p>
 * Non è una tipologia diversa — le ricette restano quelle — ma una <b>sostituzione</b>: al posto del
 * profilo base della {@link Tipologia} si taglia il profilo della variante, e le quote di ciò che gli
 * sta dentro si accorciano. Tutto il comportamento sta in due soli numeri:
 * <ul>
 *   <li>{@link Profilo#extraKerf45()} (sul profilo): millimetri di barra in più a ogni estremità
 *       tagliata a 45°, perché la diagonale di una sezione più larga è più lunga;</li>
 *   <li>{@code restringimento} (qui): millimetri che la sezione ruba <b>per lato</b> a tutto ciò che
 *       racchiude — l'anta, i pezzi interni, il vetro.</li>
 * </ul>
 * Le due grandezze si pagano allo stesso modo, <b>per estremità</b>: il kerf per ogni estremità in
 * diagonale, il restringimento per ogni estremità che va a battere sul perimetro
 * ({@link RegolaTaglio#estremitaSulPerimetro()}). Un pezzo che finisce contro un traverso non paga,
 * perché il traverso non ingrossa.
 * <p>
 * Esempio (CX 700): telaio "Z maggiorato" = profilo {@code CX70.106}, {@code extraKerf45 = 46},
 * {@code restringimento = 24} → i montanti di telaio consumano 92 mm di barra in più e ogni pezzo
 * d'anta si accorcia di 48.
 *
 * @param nome           come la chiama l'utente (es. "Z maggiorato"), unico nel suo ruolo
 * @param profilo        il profilo da tagliare al posto di quello base
 * @param restringimento millimetri rubati <b>per lato</b> a ciò che sta dentro; 0 per la variante base
 */
public record Variante(String nome, Profilo profilo, double restringimento) {

    /** La variante che non cambia nulla: è il profilo base della tipologia. */
    public Variante(String nome, Profilo profilo) {
        this(nome, profilo, 0);
    }

    /** Il ruolo su cui questa variante si sceglie: quello del suo profilo. */
    public Categoria ruolo() {
        return profilo.categoria();
    }
}
