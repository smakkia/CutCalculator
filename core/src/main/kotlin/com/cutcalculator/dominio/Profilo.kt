package com.cutcalculator.dominio

/**
 * Anagrafica di un profilo: una "scheda" del Gruppo B del catalogo.
 *
 * Tiene ciò che serve al calcolo dei tagli — codice, descrizione, categoria funzionale — e i due
 * dati che servono a **pesare e valorizzare** il materiale: il `pesoLineare` (kg/m, preso dalla
 * scheda) e il `prezzoAlChilo` (€/kg, dato commerciale). Da questi due si ricava il peso e il costo
 * di qualunque spezzone, che sia un [Pezzo], un [Avanzo] o una barra intera: basta la lunghezza.
 *
 * Volutamente **assenti**:
 * - **lunghezza barra** — è un input a runtime dell'utente, non un dato del profilo;
 * - **angoli di taglio** — dipendono dall'uso, quindi stanno in [RegolaTaglio];
 * - **dati strutturali** (Jx/Jy/Wx/Wy) — non servono al taglio né al preventivo.
 *
 * Il `prezzoAlChilo` sta qui e non su [Materiale] per non toccare la chiave di raggruppamento
 * (profilo + colore) usata ovunque; se un domani il prezzo dovesse dipendere anche dalla finitura,
 * il posto giusto sarà un listino separato, non questo record.
 *
 * L'`extraKerf45` è il terzo dato "fisico": una sezione più grande, tagliata in diagonale, mangia
 * più barra di una piccola, perché la diagonale è più lunga del fronte. Quanto di più dipende dal
 * **profilo** (sta qui), *quante volte* si paga dipende dal **pezzo** — cioè dal suo [TipoTaglio],
 * che sa quante estremità sono a 45°. Per i telai CX/RX vale `+24 mm` sul maggiorato e `+22 mm`
 * sulla forma a Z, e i due si **sommano** (Z maggiorato = 46).
 *
 * @param codice        il codice del profilo (es. `RX70.101`)
 * @param descrizione   il nome leggibile della scheda (es. "Telaio")
 * @param categoria     a cosa serve il profilo, per raggruppare i pezzi
 * @param pesoLineare   peso al metro in **kg/m**; 0 se non ancora trascritto dalla scheda
 * @param prezzoAlChilo prezzo del materiale in **€/kg**; 0 se non ancora impostato
 * @param extraKerf45   millimetri di barra consumati **in più** a ogni estremità tagliata a 45°,
 *                      rispetto al profilo base del sistema; 0 per il profilo base
 */
@JvmRecord
data class Profilo(
    val codice: String,
    val descrizione: String,
    val categoria: Categoria,
    val pesoLineare: Double,
    val prezzoAlChilo: Double,
    val extraKerf45: Double
) {
    /** Profilo senza dati di peso/prezzo: quelli dei cataloghi non ancora completati. */
    constructor(codice: String, descrizione: String, categoria: Categoria) :
            this(codice, descrizione, categoria, 0.0, 0.0, 0.0)

    /** Profilo con peso e prezzo, ma senza sovrapprezzo di taglio: la forma normale finora. */
    constructor(
        codice: String, descrizione: String, categoria: Categoria,
        pesoLineare: Double, prezzoAlChilo: Double
    ) : this(codice, descrizione, categoria, pesoLineare, prezzoAlChilo, 0.0)

    /**
     * Profilo con il suo **peso lineare** (kg/m) di scheda: è la forma normale nei cataloghi.
     * Il `prezzoAlChilo` resta 0 perché il prezzo **non** è un dato di catalogo — vive nel listino
     * dell'utente ([Prezzi]), che cambia col fornitore; il campo sul profilo serve solo a dare un
     * prezzo particolare a un singolo profilo, quando serve.
     */
    constructor(codice: String, descrizione: String, categoria: Categoria, pesoLineare: Double) :
            this(codice, descrizione, categoria, pesoLineare, 0.0, 0.0)

    /** Peso (kg) di uno spezzone di questo profilo lungo `lunghezza` mm. */
    fun peso(lunghezza: Double): Double = lunghezza / MM_PER_METRO * pesoLineare

    /** Costo (€) di uno spezzone di questo profilo lungo `lunghezza` mm: peso × €/kg. */
    fun prezzo(lunghezza: Double): Double = peso(lunghezza) * prezzoAlChilo

    /** Copia di questo profilo con un altro prezzo al chilo (il listino cambia, l'anagrafica no). */
    fun conPrezzoAlChilo(prezzoAlChilo: Double): Profilo =
        Profilo(codice, descrizione, categoria, pesoLineare, prezzoAlChilo, extraKerf45)

    private companion object {
        /** Millimetri in un metro: le lunghezze sono in mm, il peso lineare in kg/m. */
        const val MM_PER_METRO = 1000.0
    }
}
