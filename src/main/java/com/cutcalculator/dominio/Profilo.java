package com.cutcalculator.dominio;

/**
 * Anagrafica di un profilo: una "scheda" del Gruppo B del catalogo.
 * <p>
 * Tiene solo ciò che serve al calcolo dei tagli: il codice identificativo, una
 * descrizione leggibile e la categoria funzionale con cui raggruppare i pezzi.
 * <p>
 * Volutamente <b>assenti</b>:
 * <ul>
 *   <li><b>lunghezza barra</b> — è un input a runtime dell'utente, non un dato del profilo;</li>
 *   <li><b>angoli di taglio</b> — dipendono dall'uso, quindi stanno in {@code RegolaTaglio};</li>
 *   <li><b>peso e dati strutturali</b> (Jx/Jy/Wx/Wy) — non servono (ancora) al taglio.</li>
 * </ul>
 */
public record Profilo(String codice, String descrizione, Categoria categoria) {
}
