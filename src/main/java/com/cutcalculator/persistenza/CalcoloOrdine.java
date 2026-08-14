package com.cutcalculator.persistenza;

import java.util.List;

/**
 * I documenti di un ordine <b>già calcolato</b>, riletti da disco: la sua distinta, il suo
 * preventivo, i suoi vetri e quanto costa in tutto.
 * <p>
 * Le righe sono volutamente "piatte" — codici e numeri, non oggetti del dominio. Sono la
 * fotografia di un calcolo fatto in passato: ricostruire {@code Profilo} e {@code Tipologia} dal
 * catalogo di <i>oggi</i> darebbe l'illusione che il documento sia ancora valido, mentre nel
 * frattempo le schede possono essere cambiate. Per rifare i conti si ricalcola l'ordine.
 */
public record CalcoloOrdine(
        String ordine,
        List<VoceDistinta> distinta,
        List<VocePreventivo> preventivo,
        List<VoceVetro> vetri) {

    /** Un gruppo di pezzi uguali da tagliare. */
    public record VoceDistinta(String profilo, String colore, double lunghezza, String taglio,
            int quantita) {
    }

    /** La quota di un materiale: quanto se ne taglia, quanto pesa e quanto costa. */
    public record VocePreventivo(String profilo, String colore, double lunghezza, double peso,
            double costo) {
    }

    /** Le lastre di una misura. */
    public record VoceVetro(double altezza, double larghezza, int quantita, double areaMq,
            double costo) {
    }

    /** {@code true} se di quest'ordine non c'è (ancora) nessun calcolo salvato. */
    public boolean vuoto() {
        return distinta.isEmpty() && preventivo.isEmpty() && vetri.isEmpty();
    }

    public double costoProfili() {
        return preventivo.stream().mapToDouble(VocePreventivo::costo).sum();
    }

    public double costoVetro() {
        return vetri.stream().mapToDouble(VoceVetro::costo).sum();
    }

    /** Il prezzo dell'ordine: la sua quota di alluminio più il suo vetro. */
    public double costoTotale() {
        return costoProfili() + costoVetro();
    }

    public double pesoProfili() {
        return preventivo.stream().mapToDouble(VocePreventivo::peso).sum();
    }

    public int totalePezzi() {
        return distinta.stream().mapToInt(VoceDistinta::quantita).sum();
    }

    public int totaleLastre() {
        return vetri.stream().mapToInt(VoceVetro::quantita).sum();
    }

    public double areaVetroMq() {
        return vetri.stream().mapToDouble(VoceVetro::areaMq).sum();
    }
}
