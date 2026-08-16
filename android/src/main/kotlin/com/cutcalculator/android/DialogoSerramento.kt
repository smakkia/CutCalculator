package com.cutcalculator.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MenuAnchorType
import com.cutcalculator.app.Etichette
import com.cutcalculator.dominio.Categoria
import com.cutcalculator.dominio.Serramento
import com.cutcalculator.dominio.Tipologia
import com.cutcalculator.dominio.Variante
import com.cutcalculator.dominio.Varianti

/**
 * Il form con cui si aggiunge un serramento a un ordine — e con cui si **corregge** uno già
 * inserito, passandolo come [iniziale]: i campi partono dai suoi valori e chi conferma senza
 * toccare niente riottiene lo stesso serramento. Un form solo, così le regole non divergono.
 *
 * Tre regole prese di peso dal desktop, perché sono del dominio e non della UI:
 * - le tipologie dipendono dal sistema scelto (menu a cascata);
 * - il campo **HF** (altezza parziale) compare **solo** se la tipologia lo usa — cioè per le porte
 *   col traverso. Chiederlo sempre confonderebbe, e per le finestre vale comunque 0.
 * - le **varianti** si chiedono un ruolo alla volta (telaio, anta...), solo per i ruoli che hanno
 *   davvero un'alternativa **e** che la tipologia usa: un elemento fisso non ha ante, e sceglierne
 *   una lì accorcerebbe fermavetro e vetro per un profilo mai tagliato. Gli scorrevoli, che di
 *   varianti non ne dichiarano, non mostrano niente.
 *
 * Le misure si scrivono nell'unità scelta nelle impostazioni, con la virgola o il punto, e vengono
 * riportate in millimetri prima di entrare nel modello — che è **sempre** in millimetri.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoSerramento(
    vm: CutCalculatorViewModel,
    annulla: () -> Unit,
    iniziale: Serramento? = null,
    conferma: (Tipologia, Varianti, String, Double, Double, Double, Int, Double, Double) -> Unit
) {
    val sistemi = vm.sistemi
    // Il serramento porta la tipologia ma non il sistema: lo si chiede al catalogo.
    var sistema by remember {
        mutableStateOf(iniziale?.let { vm.sistemaDi(it.tipologia) } ?: sistemi.first())
    }
    var tipologia by remember { mutableStateOf(iniziale?.tipologia ?: sistema.tipologie().first()) }
    // Le scelte fatte finora, per ruolo. Si tengono in una mappa e non in una Varianti perché qui
    // serve anche sapere *quale* voce evidenziare nel menu, e le scelte vanno ripulite quando
    // cambia il sistema (le varianti sono sue) o la tipologia (che può non usare quel ruolo).
    var scelte by remember { mutableStateOf(iniziale?.varianti?.scelte() ?: emptyMap()) }
    var colore by remember { mutableStateOf(iniziale?.colore?.nome() ?: "") }
    // Le misure del modello sono in mm: nei campi vanno nell'unità scelta dall'utente, che è quella
    // con cui le rileggerà `versoMm` alla conferma.
    var larghezza by remember { mutableStateOf(vm.testoMisura(iniziale?.dimensione?.L)) }
    var altezza by remember { mutableStateOf(vm.testoMisura(iniziale?.dimensione?.H)) }
    var altezzaParziale by remember { mutableStateOf(vm.testoMisura(iniziale?.dimensione?.HF)) }
    var quantita by remember { mutableStateOf(iniziale?.quantita?.toString() ?: "1") }
    var prezzoKg by remember { mutableStateOf(testoPrezzo(iniziale?.prezzi?.alChiloBarre)) }
    var prezzoMq by remember { mutableStateOf(testoPrezzo(iniziale?.prezzi?.alMqVetro)) }

    // I ruoli da chiedere per questa coppia sistema+tipologia, e la variante corrente di ognuno: se
    // l'utente non l'ha toccata vale la prima, cioè il profilo base già cablato nella tipologia.
    val ruoli = sistema.ruoliConScelta(tipologia)
    fun varianteDi(ruolo: Categoria): Variante =
        scelte[ruolo] ?: sistema.variantiDi(ruolo).first()
    val varianti = ruoli.fold(Varianti.NESSUNA) { scelta, ruolo -> scelta.con(varianteDi(ruolo)) }

    val l = larghezza.misura()
    val h = altezza.misura()
    val hf = if (tipologia.usaHF()) altezzaParziale.misura() else 0.0
    val q = quantita.trim().toIntOrNull() ?: 0
    val valido = l != null && l > 0 && h != null && h > 0 && q > 0 && colore.isNotBlank() &&
            (!tipologia.usaHF() || (hf != null && hf > 0))

    AlertDialog(
        onDismissRequest = annulla,
        title = { Text(if (iniziale == null) "Nuovo serramento" else "Modifica serramento") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Menu("Sistema", sistemi, sistema, { it.nome() }) { scelto ->
                    sistema = scelto
                    tipologia = scelto.tipologie().first()   // le tipologie sono del sistema
                    scelte = emptyMap()                      // e le varianti pure
                }
                Menu("Tipologia", sistema.tipologie(), tipologia, { it.nome() }) { scelta ->
                    tipologia = scelta
                    // Si tiene quel che resta possibile: chi ha appena scelto il telaio a Z non deve
                    // ritrovarselo azzerato per aver cambiato tipologia.
                    val ammessi = sistema.ruoliConScelta(scelta)
                    scelte = scelte.filterKeys { it in ammessi }
                }
                ruoli.forEach { ruolo ->
                    Menu(Etichette.ruolo(ruolo), sistema.variantiDi(ruolo), varianteDi(ruolo),
                        { it.nome }) { scelta -> scelte = scelte + (ruolo to scelta) }
                }

                val simbolo = vm.unita.simbolo()
                Numero("Larghezza L ($simbolo)", larghezza) { larghezza = it }
                Numero("Altezza H ($simbolo)", altezza) { altezza = it }
                if (tipologia.usaHF()) {
                    Numero("Altezza parziale HF ($simbolo)", altezzaParziale) { altezzaParziale = it }
                    Text(
                        "Questa tipologia ha il traverso: HF e' l'altezza della parte sotto.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Numero("Quantita'", quantita) { quantita = it }
                OutlinedTextField(
                    value = colore,
                    onValueChange = { colore = it },
                    label = { Text("Colore") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
                Row {
                    Numero("EUR/kg", prezzoKg, Modifier.weight(1f)) { prezzoKg = it }
                    Numero("EUR/mq", prezzoMq, Modifier.weight(1f)) { prezzoMq = it }
                }
                Text(
                    "Prezzi vuoti: i costi verranno zero, il taglio si calcola lo stesso.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = {
                    // Solo le misure si convertono: i prezzi sono EUR/kg e EUR/mq, non lunghezze.
                    conferma(
                        tipologia,
                        varianti,
                        colore.trim(),
                        vm.versoMm(l ?: 0.0),
                        vm.versoMm(h ?: 0.0),
                        vm.versoMm(hf ?: 0.0),
                        q,
                        prezzoKg.misura() ?: 0.0,
                        prezzoMq.misura() ?: 0.0
                    )
                }
            ) { Text(if (iniziale == null) "Aggiungi" else "Salva") }
        },
        dismissButton = { TextButton(onClick = annulla) { Text("Annulla") } }
    )
}

/** Un prezzo da riscrivere in un campo: vuoto se non era impostato (zero = "non impostato"). */
private fun testoPrezzo(euro: Double?): String =
    euro?.takeIf { it > 0 }?.toString() ?: ""

/** Menu a tendina generico: mostra `etichetta(elemento)` e restituisce l'elemento scelto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Menu(
    titolo: String,
    elementi: List<T>,
    scelto: T,
    etichetta: (T) -> String,
    onScelta: (T) -> Unit
) {
    var aperto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = aperto,
        onExpandedChange = { aperto = it },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = etichetta(scelto),
            onValueChange = {},
            readOnly = true,
            label = { Text(titolo) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aperto) },
            modifier = Modifier.fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = aperto, onDismissRequest = { aperto = false }) {
            elementi.forEach { elemento ->
                DropdownMenuItem(
                    text = { Text(etichetta(elemento)) },
                    onClick = {
                        onScelta(elemento)
                        aperto = false
                    }
                )
            }
        }
    }
}

@Composable
private fun Numero(
    etichetta: String,
    valore: String,
    modifier: Modifier = Modifier,
    onCambio: (String) -> Unit
) {
    OutlinedTextField(
        value = valore,
        onValueChange = onCambio,
        label = { Text(etichetta) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

/** Una misura scritta dall'utente: si accetta la virgola come il punto, come sul desktop. */
private fun String.misura(): Double? = trim().replace(',', '.').toDoubleOrNull()
