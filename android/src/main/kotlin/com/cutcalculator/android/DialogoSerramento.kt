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
import com.cutcalculator.catalogo.Sistema
import com.cutcalculator.dominio.Tipologia

/**
 * Il form con cui si aggiunge un serramento a un ordine.
 *
 * Due regole prese di peso dal desktop, perché sono del dominio e non della UI:
 * - le tipologie dipendono dal sistema scelto (menu a cascata);
 * - il campo **HF** (altezza parziale) compare **solo** se la tipologia lo usa — cioè per le porte
 *   col traverso. Chiederlo sempre confonderebbe, e per le finestre vale comunque 0.
 *
 * Le misure si scrivono in millimetri, con la virgola o il punto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoSerramento(
    vm: CutCalculatorViewModel,
    annulla: () -> Unit,
    conferma: (Tipologia, String, Double, Double, Double, Int, Double, Double) -> Unit
) {
    val sistemi = vm.sistemi
    var sistema by remember { mutableStateOf(sistemi.first()) }
    var tipologia by remember { mutableStateOf(sistema.tipologie().first()) }
    var colore by remember { mutableStateOf("") }
    var larghezza by remember { mutableStateOf("") }
    var altezza by remember { mutableStateOf("") }
    var altezzaParziale by remember { mutableStateOf("") }
    var quantita by remember { mutableStateOf("1") }
    var prezzoKg by remember { mutableStateOf("") }
    var prezzoMq by remember { mutableStateOf("") }

    val l = larghezza.misura()
    val h = altezza.misura()
    val hf = if (tipologia.usaHF()) altezzaParziale.misura() else 0.0
    val q = quantita.trim().toIntOrNull() ?: 0
    val valido = l != null && l > 0 && h != null && h > 0 && q > 0 && colore.isNotBlank() &&
            (!tipologia.usaHF() || (hf != null && hf > 0))

    AlertDialog(
        onDismissRequest = annulla,
        title = { Text("Nuovo serramento") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Menu("Sistema", sistemi, sistema, { it.nome() }) { scelto ->
                    sistema = scelto
                    tipologia = scelto.tipologie().first()   // le tipologie sono del sistema
                }
                Menu("Tipologia", sistema.tipologie(), tipologia, { it.nome() }) { tipologia = it }

                Numero("Larghezza L (mm)", larghezza) { larghezza = it }
                Numero("Altezza H (mm)", altezza) { altezza = it }
                if (tipologia.usaHF()) {
                    Numero("Altezza parziale HF (mm)", altezzaParziale) { altezzaParziale = it }
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
                    conferma(
                        tipologia,
                        colore.trim(),
                        l ?: 0.0,
                        h ?: 0.0,
                        hf ?: 0.0,
                        q,
                        prezzoKg.misura() ?: 0.0,
                        prezzoMq.misura() ?: 0.0
                    )
                }
            ) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = annulla) { Text("Annulla") } }
    )
}

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
