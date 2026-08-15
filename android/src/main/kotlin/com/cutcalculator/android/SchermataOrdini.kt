package com.cutcalculator.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.cutcalculator.app.Etichette
import com.cutcalculator.dominio.Ordine

/**
 * Gli ordini: l'elenco delle commesse e, aprendone una, i suoi serramenti.
 *
 * Un ordine "da calcolare" entrerà nel prossimo calcolo globale; uno "calcolato" ha già scalato il
 * suo materiale dal magazzino — la distinzione è la stessa del desktop, e vale la pena vederla a
 * colpo d'occhio anche sul telefono.
 */
@Composable
fun SchermataOrdini(vm: CutCalculatorViewModel, modifier: Modifier = Modifier) {
    var aperto by remember { mutableStateOf<Ordine?>(null) }

    // L'ordine aperto potrebbe essere stato rimosso: si torna all'elenco invece di mostrare un
    // fantasma.
    val ordineCorrente = aperto?.takeIf { candidato -> vm.ordini.any { it === candidato } }
    if (ordineCorrente == null) {
        ElencoOrdini(vm, modifier, apri = { aperto = it })
    } else {
        DettaglioOrdine(vm, ordineCorrente, modifier, chiudi = { aperto = null })
    }
}

@Composable
private fun ElencoOrdini(
    vm: CutCalculatorViewModel,
    modifier: Modifier,
    apri: (Ordine) -> Unit
) {
    var chiediNome by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Button(
                    onClick = { vm.calcolaTutto() },
                    enabled = vm.ordiniDaCalcolare.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text("Calcola tutti gli ordini da fare (${vm.ordiniDaCalcolare.size})")
                }
                Text(
                    "Gli ordini vengono tagliati insieme: i pezzi condividono le barre e lo sfrido cala.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (vm.ordini.isEmpty()) {
                item {
                    Text(
                        "Nessun ordine. Toccare + per crearne uno.",
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

            items(vm.ordini, key = { it.nome() }) { ordine ->
                Card(
                    onClick = { apri(ordine) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(ordine.nome(), fontWeight = FontWeight.Bold)
                        Text(
                            "${ordine.serramenti().size} righe, ${ordine.totaleSerramenti()} serramenti" +
                                    if (ordine.calcolato()) " — calcolato" else " — da calcolare",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { chiediNome = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nuovo ordine")
        }
    }

    if (chiediNome) {
        DialogoNuovoOrdine(
            proposto = vm.nomeOrdineProposto(),
            nomeLibero = vm::nomeLibero,
            annulla = { chiediNome = false },
            conferma = { nome ->
                vm.nuovoOrdine(nome)
                chiediNome = false
            }
        )
    }
}

@Composable
private fun DettaglioOrdine(
    vm: CutCalculatorViewModel,
    ordine: Ordine,
    modifier: Modifier,
    chiudi: () -> Unit
) {
    var aggiungi by remember { mutableStateOf(false) }
    val serramenti = ordine.serramenti()

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = chiudi) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                    Text(
                        ordine.nome(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        vm.rimuoviOrdine(ordine)
                        chiudi()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Elimina ordine")
                    }
                }
                if (ordine.calcolato()) {
                    Text(
                        "Ordine gia' calcolato: modificarlo lo rimette fra quelli da fare, e il " +
                                "prossimo calcolo gli riassegnera' tutto il materiale da capo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (vm.ripristinabile(ordine)) {
                        OutlinedButton(
                            onClick = { vm.ripristina(ordine) },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = null)
                            Text("  Annulla l'ultimo calcolo")
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }

            if (serramenti.isEmpty()) {
                item { Text("Nessun serramento. Toccare + per aggiungerne uno.") }
            }

            items(serramenti.size) { indice ->
                val serramento = serramenti[indice]
                val d = serramento.dimensione
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(serramento.tipologia.nome(), fontWeight = FontWeight.Bold)
                            val misure = "${Etichette.misura(d.L, vm.unita)} x " +
                                    "${Etichette.misura(d.H, vm.unita)}" +
                                    if (d.HF > 0) " (HF ${Etichette.misura(d.HF, vm.unita)})" else ""
                            Text(
                                "$misure ${vm.unita.simbolo()} — x${serramento.quantita} — " +
                                        serramento.colore.nome(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { vm.rimuoviSerramento(ordine, indice) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Togli serramento")
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { aggiungi = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi serramento")
        }
    }

    if (aggiungi) {
        DialogoSerramento(
            vm = vm,
            annulla = { aggiungi = false },
            conferma = { tipologia, colore, l, h, hf, quantita, prezzoKg, prezzoMq ->
                vm.aggiungiSerramento(ordine, tipologia, colore, l, h, hf, quantita, prezzoKg, prezzoMq)
                aggiungi = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoNuovoOrdine(
    proposto: String,
    nomeLibero: (String) -> Boolean,
    annulla: () -> Unit,
    conferma: (String) -> Unit
) {
    var nome by remember { mutableStateOf(proposto) }
    val valido = nome.isNotBlank() && nomeLibero(nome.trim())

    AlertDialog(
        onDismissRequest = annulla,
        title = { Text("Nuovo ordine") },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    isError = !valido
                )
                if (!valido) {
                    // Il nome e' la chiave con cui gli ordini si rileggono da disco: due omonimi si
                    // fonderebbero in uno solo al prossimo avvio.
                    Text(
                        "Nome vuoto o gia' usato da un altro ordine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { conferma(nome.trim()) }, enabled = valido) { Text("Crea") }
        },
        dismissButton = { TextButton(onClick = annulla) { Text("Annulla") } }
    )
}
