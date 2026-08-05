package com.cutcalculator.dominio;

/**
 * Un avanzo (spezzone) già in magazzino: una barra corta, di proprietà, da cui
 * l'ottimizzatore può ricavare pezzi <b>prima</b> di aprire una barra nuova.
 * <p>
 * È legato a un {@link Profilo}: da un avanzo del profilo X si tagliano solo pezzi
 * del profilo X. La {@code quantita} permette di dichiarare più avanzi identici in
 * un colpo solo (es. "3 spezzoni del telaio da 1200 mm").
 */
public record Avanzo(Profilo profilo, double lunghezza, int quantita) {
}
