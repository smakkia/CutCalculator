package com.cutcalculator.dominio;

/**
 * Anagrafica di un profilo: una "scheda" del Gruppo B del catalogo.
 * <p>
 * Tiene ciò che serve al calcolo dei tagli — codice, descrizione, categoria funzionale — e i due
 * dati che servono a <b>pesare e valorizzare</b> il materiale: il {@code pesoLineare} (kg/m, preso
 * dalla scheda) e il {@code prezzoAlChilo} (€/kg, dato commerciale). Da questi due si ricava il
 * peso e il costo di qualunque spezzone, che sia un {@link Pezzo}, un {@link Avanzo} o una barra
 * intera: basta la lunghezza.
 * <p>
 * Volutamente <b>assenti</b>:
 * <ul>
 *   <li><b>lunghezza barra</b> — è un input a runtime dell'utente, non un dato del profilo;</li>
 *   <li><b>angoli di taglio</b> — dipendono dall'uso, quindi stanno in {@code RegolaTaglio};</li>
 *   <li><b>dati strutturali</b> (Jx/Jy/Wx/Wy) — non servono al taglio né al preventivo.</li>
 * </ul>
 * Il {@code prezzoAlChilo} sta qui e non su {@link Materiale} per non toccare la chiave di
 * raggruppamento (profilo + colore) usata ovunque; se un domani il prezzo dovesse dipendere anche
 * dalla finitura, il posto giusto sarà un listino separato, non questo record.
 * <p>
 * L'{@code extraKerf45} è il terzo dato "fisico": una sezione più grande, tagliata in diagonale,
 * mangia più barra di una piccola, perché la diagonale è più lunga del fronte. Quanto di più dipende
 * dal <b>profilo</b> (sta qui), <i>quante volte</i> si paga dipende dal <b>pezzo</b> — cioè dal suo
 * {@link TipoTaglio}, che sa quante estremità sono a 45°. Per i telai CX/RX vale
 * {@code +24 mm} sul maggiorato e {@code +22 mm} sulla forma a Z, e i due si <b>sommano</b>
 * (Z maggiorato = 46).
 *
 * @param codice        il codice del profilo (es. {@code RX70.101})
 * @param descrizione   il nome leggibile della scheda (es. "Telaio")
 * @param categoria     a cosa serve il profilo, per raggruppare i pezzi
 * @param pesoLineare   peso al metro in <b>kg/m</b>; 0 se non ancora trascritto dalla scheda
 * @param prezzoAlChilo prezzo del materiale in <b>€/kg</b>; 0 se non ancora impostato
 * @param extraKerf45   millimetri di barra consumati <b>in più</b> a ogni estremità tagliata a 45°,
 *                      rispetto al profilo base del sistema; 0 per il profilo base
 */
public record Profilo(String codice, String descrizione, Categoria categoria,
        double pesoLineare, double prezzoAlChilo, double extraKerf45) {

    /** Millimetri in un metro: le lunghezze sono in mm, il peso lineare in kg/m. */
    private static final double MM_PER_METRO = 1000.0;

    /** Profilo senza dati di peso/prezzo: quelli dei cataloghi non ancora completati. */
    public Profilo(String codice, String descrizione, Categoria categoria) {
        this(codice, descrizione, categoria, 0, 0, 0);
    }

    /** Profilo con peso e prezzo, ma senza sovrapprezzo di taglio: la forma normale finora. */
    public Profilo(String codice, String descrizione, Categoria categoria,
            double pesoLineare, double prezzoAlChilo) {
        this(codice, descrizione, categoria, pesoLineare, prezzoAlChilo, 0);
    }

    /**
     * Profilo con il suo <b>peso lineare</b> (kg/m) di scheda: è la forma normale nei cataloghi.
     * Il {@code prezzoAlChilo} resta 0 perché il prezzo <b>non</b> è un dato di catalogo — vive nel
     * listino dell'utente ({@link Prezzi}), che cambia col fornitore; il campo sul profilo serve solo
     * a dare un prezzo particolare a un singolo profilo, quando serve.
     */
    public Profilo(String codice, String descrizione, Categoria categoria, double pesoLineare) {
        this(codice, descrizione, categoria, pesoLineare, 0, 0);
    }

    /** Peso (kg) di uno spezzone di questo profilo lungo {@code lunghezza} mm. */
    public double peso(double lunghezza) {
        return lunghezza / MM_PER_METRO * pesoLineare;
    }

    /** Costo (€) di uno spezzone di questo profilo lungo {@code lunghezza} mm: peso × €/kg. */
    public double prezzo(double lunghezza) {
        return peso(lunghezza) * prezzoAlChilo;
    }

    /** Copia di questo profilo con un altro prezzo al chilo (il listino cambia, l'anagrafica no). */
    public Profilo conPrezzoAlChilo(double prezzoAlChilo) {
        return new Profilo(codice, descrizione, categoria, pesoLineare, prezzoAlChilo, extraKerf45);
    }
}
