package com.cutcalculator.dominio

/**
 * Un [Profilo] in un [Colore]: l'unita fisica di magazzino e di compatibilita del taglio.
 * Pezzi, avanzi e barre si raggruppano per **materiale**, non per solo profilo: da una barra di un
 * dato profilo *e* colore si possono ricavare solo pezzi dello stesso profilo *e* colore.
 *
 * E' solo la chiave composta (profilo + colore); lunghezze e pezzi vivono altrove
 * ([Avanzo], [Pezzo], `BarraTagliata`).
 */
@JvmRecord
data class Materiale(val profilo: Profilo, val colore: Colore)
