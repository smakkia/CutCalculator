package com.cutcalculator.preventivo;

import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Prezzi;
import com.cutcalculator.dominio.Profilo;

import java.util.List;
import java.util.Optional;

/**
 * L'output della Fase 3: la stima del materiale necessario per un ordine — una
 * {@link RigaProfilo riga} per profilo (barre da comprare, avanzi riusati, sfrido) e una
 * {@link RigaVetro riga} per misura di lastra. Gli accessori entreranno qui quando saranno nel modello.
 * <p>
 * Le due parti si contano in unità diverse e restano quindi separate: le barre in <b>metri lineari</b>
 * (e passano dall'ottimizzatore), il vetro in <b>metri quadri</b> (e no). Anche il <b>costo</b> segue
 * quella divisione: l'alluminio si paga a peso (€/kg), il vetro a superficie (€/mq).
 * <p>
 * Il {@link Prezzi listino} usato viaggia <b>dentro</b> il preventivo: i costi non sono memorizzati
 * ma ricalcolati dalle quantità, e restano legati ai prezzi con cui il preventivo è stato fatto —
 * che è esattamente quello che ci si aspetta da un preventivo. Senza listino i costi sono zero.
 */
public record Preventivo(List<RigaProfilo> righe, List<RigaVetro> righeVetro) {

    public Preventivo {
        righe = List.copyOf(righe);
        righeVetro = List.copyOf(righeVetro);
    }

    /** Preventivo di soli profili: quando la tipologia non ha (ancora) le quote del vetro. */
    public Preventivo(List<RigaProfilo> righe) {
        this(righe, List.of());
    }

    /** Barre nuove da comprare in totale, su tutti i profili. */
    public int totaleBarreNuove() {
        return righe.stream().mapToInt(RigaProfilo::barreNuove).sum();
    }

    /** Spezzoni di magazzino riutilizzati in totale. */
    public int totaleAvanziUsati() {
        return righe.stream().mapToInt(RigaProfilo::avanziUsati).sum();
    }

    /** Metri lineari di barra nuova ordinati in totale. */
    public double lunghezzaNuovaTotale() {
        return righe.stream().mapToDouble(RigaProfilo::lunghezzaNuova).sum();
    }

    /** Sfrido complessivo del piano, su tutte le barre (nuove e avanzi). */
    public double sfridoTotale() {
        return righe.stream().mapToDouble(RigaProfilo::sfrido).sum();
    }

    /** Quanti ritagli sopra soglia rientreranno in magazzino come nuovi avanzi. */
    public int totaleRitagliRecuperabili() {
        return righe.stream().mapToInt(RigaProfilo::ritagliRecuperabili).sum();
    }

    /** Lunghezza totale dei ritagli recuperabili: la parte di sfrido che non va persa. */
    public double lunghezzaRecuperabileTotale() {
        return righe.stream().mapToDouble(RigaProfilo::lunghezzaRecuperabile).sum();
    }

    /** Lo sfrido che resta scarto vero: troppo corto per tornare a magazzino. */
    public double scartoTotale() {
        return sfridoTotale() - lunghezzaRecuperabileTotale();
    }

    /** Quante lastre di vetro in tutto, di qualunque misura. */
    public int totaleLastre() {
        return righeVetro.stream().mapToInt(RigaVetro::quantita).sum();
    }

    /** Superficie vetrata complessiva da ordinare, in metri quadri. */
    public double areaVetroTotaleMq() {
        return righeVetro.stream().mapToDouble(RigaVetro::areaTotaleMq).sum();
    }

    // --- Costi: quantità × listino dell'utente ----------------------------------------

    /** Peso (kg) delle barre nuove da comprare, su tutti i profili. */
    public double pesoTotale() {
        return righe.stream().mapToDouble(RigaProfilo::peso).sum();
    }

    /** Quanto costano le barre nuove da comprare. */
    public double costoBarre() {
        return righe.stream().mapToDouble(RigaProfilo::costo).sum();
    }

    /** Quanto costa il vetro da ordinare. */
    public double costoVetro() {
        return righeVetro.stream().mapToDouble(RigaVetro::costo).sum();
    }

    /** {@code true} se almeno una voce ha un costo: sotto questa condizione i totali hanno senso. */
    public boolean valorizzato() {
        return costoTotale() > 0;
    }

    /** Il costo del materiale dell'ordine: barre nuove + vetro. */
    public double costoTotale() {
        return costoBarre() + costoVetro();
    }

    /** La riga di un dato profilo in un dato colore, se presente nel preventivo. */
    public Optional<RigaProfilo> rigaDi(Profilo profilo, Colore colore) {
        return righe.stream()
                .filter(riga -> riga.profilo().equals(profilo) && riga.colore().equals(colore))
                .findFirst();
    }
}
