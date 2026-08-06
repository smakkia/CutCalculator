package com.cutcalculator.dominio;

/**
 * Un {@link Profilo} in un {@link Colore}: l'unita fisica di magazzino e di compatibilita del
 * taglio. Pezzi, avanzi e barre si raggruppano per <b>materiale</b>, non per solo profilo: da una
 * barra di un dato profilo <i>e</i> colore si possono ricavare solo pezzi dello stesso profilo
 * <i>e</i> colore.
 * <p>
 * E' solo la chiave composta (profilo + colore); lunghezze e pezzi vivono altrove
 * ({@link Avanzo}, {@link Pezzo}, {@code BarraTagliata}).
 */
public record Materiale(Profilo profilo, Colore colore) {
}
