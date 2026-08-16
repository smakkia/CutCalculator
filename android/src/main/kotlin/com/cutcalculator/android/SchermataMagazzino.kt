package com.cutcalculator.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cutcalculator.app.Etichette

/**
 * Il magazzino degli avanzi: gli spezzoni già di proprietà, che l'ottimizzatore usa **prima** di
 * aprire una barra nuova. In officina si chiamano "pezzi", ed è così che li chiama anche la CLI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataMagazzino(vm: CutCalculatorViewModel, modifier: Modifier = Modifier) {
    var aggiungi by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${vm.magazzino.size} righe",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (vm.magazzino.isNotEmpty()) {
                        OutlinedButton(onClick = { vm.svuotaMagazzino() }) { Text("Svuota") }
                    }
                }
            }

            if (vm.magazzino.isEmpty()) {
                item { Text("Magazzino vuoto: si taglieranno solo barre nuove.") }
            }

            items(vm.magazzino.size) { indice ->
                val avanzo = vm.magazzino[indice]
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(Etichette.materiale(avanzo.materiale()), fontWeight = FontWeight.Bold)
                            Text(
                                "${Etichette.misuraConSimbolo(avanzo.lunghezza, vm.unita)} " +
                                        "x${avanzo.quantita}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { vm.rimuoviAvanzo(indice, avanzo.quantita) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Togli")
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { aggiungi = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi pezzo")
        }
    }

    if (aggiungi) {
        DialogoAvanzo(vm, annulla = { aggiungi = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoAvanzo(vm: CutCalculatorViewModel, annulla: () -> Unit) {
    val profili = remember { vm.profili() }
    var profilo by remember { mutableStateOf(profili.first()) }
    var colore by remember { mutableStateOf("") }
    var lunghezza by remember { mutableStateOf("") }
    var quantita by remember { mutableStateOf("1") }
    var apertoMenu by remember { mutableStateOf(false) }

    // Quel che l'utente scrive e' nell'unita' scelta nelle impostazioni, non per forza in mm.
    val scritto = lunghezza.trim().replace(',', '.').toDoubleOrNull()
    val q = quantita.trim().toIntOrNull() ?: 0
    val valido = scritto != null && scritto > 0 && q > 0 && colore.isNotBlank()

    AlertDialog(
        onDismissRequest = annulla,
        title = { Text("Nuovo pezzo a magazzino") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ExposedDropdownMenuBox(
                    expanded = apertoMenu,
                    onExpandedChange = { apertoMenu = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = Etichette.profilo(profilo),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Profilo") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = apertoMenu)
                        },
                        modifier = Modifier.fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = apertoMenu,
                        onDismissRequest = { apertoMenu = false }
                    ) {
                        profili.forEach { candidato ->
                            DropdownMenuItem(
                                text = { Text(Etichette.profilo(candidato)) },
                                onClick = {
                                    profilo = candidato
                                    apertoMenu = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = colore,
                    onValueChange = { colore = it },
                    label = { Text("Colore") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
                OutlinedTextField(
                    value = lunghezza,
                    onValueChange = { lunghezza = it },
                    label = { Text("Lunghezza (${vm.unita.simbolo()})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
                OutlinedTextField(
                    value = quantita,
                    onValueChange = { quantita = it },
                    label = { Text("Quantita'") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = {
                    vm.aggiungiAvanzo(profilo, colore.trim(), vm.versoMm(scritto ?: 0.0), q)
                    annulla()
                }
            ) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = annulla) { Text("Annulla") } }
    )
}
