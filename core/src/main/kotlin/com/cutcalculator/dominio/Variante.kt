package com.cutcalculator.dominio

/**
 * Una variante di profilo per un **ruolo** del serramento: lo stesso telaio esiste ad L o a Z,
 * piccolo o maggiorato, e la stessa anta piccola o maggiorata.
 *
 * Non è una tipologia diversa — le ricette restano quelle — ma una **sostituzione**: al posto del
 * profilo base della [Tipologia] si taglia il profilo della variante, e le quote di ciò che gli sta
 * dentro si accorciano. Tutto il comportamento sta in due soli numeri:
 * - [Profilo.extraKerf45] (sul profilo): millimetri di barra in più a ogni estremità tagliata a 45°,
 *   perché la diagonale di una sezione più larga è più lunga;
 * - `restringimento` (qui): millimetri che la sezione ruba **per lato** a tutto ciò che racchiude —
 *   l'anta, i pezzi interni, il vetro.
 *
 * Le due grandezze si pagano allo stesso modo, **per estremità**: il kerf per ogni estremità in
 * diagonale, il restringimento per ogni estremità che va a battere sul perimetro
 * ([RegolaTaglio.estremitaSulPerimetro]). Un pezzo che finisce contro un traverso non paga, perché
 * il traverso non ingrossa.
 *
 * Esempio (CX 700): telaio "Z maggiorato" = profilo `CX70.106`, `extraKerf45 = 46`,
 * `restringimento = 24` → i montanti di telaio consumano 92 mm di barra in più e ogni pezzo d'anta
 * si accorcia di 48.
 *
 * @param nome           come la chiama l'utente (es. "Z maggiorato"), unico nel suo ruolo
 * @param profilo        il profilo da tagliare al posto di quello base
 * @param restringimento millimetri rubati **per lato** a ciò che sta dentro; 0 per la variante base
 */
@JvmRecord
data class Variante(val nome: String, val profilo: Profilo, val restringimento: Double) {

    /** La variante che non cambia nulla: è il profilo base della tipologia. */
    constructor(nome: String, profilo: Profilo) : this(nome, profilo, 0.0)

    /** Il ruolo su cui questa variante si sceglie: quello del suo profilo. */
    fun ruolo(): Categoria = profilo.categoria
}
