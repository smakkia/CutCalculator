package com.cutcalculator.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * I pezzi di interfaccia condivisi dalle schermate: la riga "etichetta → valore" e la **sezione a
 * tendina**. Stanno qui e non dentro una schermata perché li usano sia i risultati del calcolo
 * globale sia i documenti del singolo ordine, e ricopiarli farebbe divergere le due viste.
 */

/**
 * La domanda da fare prima di cancellare qualcosa: un ordine, un serramento, un pezzo di magazzino.
 *
 * Il bottone che conferma porta il nome di quel che fa ("Elimina", "Togli") invece di un "OK" che
 * non dice niente, e l'annullamento è la via d'uscita anche toccando fuori dal dialogo.
 *
 * @param testo cosa si sta per cancellare, scritto per esteso: è l'ultima occasione per accorgersi
 *              di aver toccato la riga sbagliata
 */
@Composable
fun DialogoConferma(
    titolo: String,
    testo: String,
    azione: String,
    annulla: () -> Unit,
    conferma: () -> Unit
) {
    AlertDialog(
        onDismissRequest = annulla,
        title = { Text(titolo) },
        text = { Text(testo) },
        confirmButton = { TextButton(onClick = conferma) { Text(azione) } },
        dismissButton = { TextButton(onClick = annulla) { Text("Annulla") } }
    )
}

/** Una riga "etichetta → valore", il mattone con cui sono scritti preventivo e documenti. */
@Composable
fun Voce(etichetta: String, valore: String, grassetto: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(etichetta, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            valore,
            fontWeight = if (grassetto) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Una sezione a tendina dentro una `LazyColumn`: l'intestazione è sempre in elenco, il contenuto
 * compare solo da aperta.
 *
 * È una funzione di estensione su [LazyListScope], non un `@Composable`, perché il contenuto deve
 * restare fatto di **item della lista**: racchiuderlo in una `Column` costringerebbe Compose a
 * comporre tutte le barre del piano insieme, anche quelle fuori schermo.
 *
 * @param riepilogo il numero che conta, visibile **anche da chiusa**: serve a decidere se aprire
 */
fun LazyListScope.sezione(
    titolo: String,
    riepilogo: String,
    espansa: Boolean,
    cambia: () -> Unit,
    contenuto: LazyListScope.() -> Unit
) {
    item {
        Card(Modifier.fillMaxWidth().clickable(onClick = cambia)) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(titolo, style = MaterialTheme.typography.titleMedium)
                    if (riepilogo.isNotBlank()) {
                        Text(riepilogo, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Icon(
                    if (espansa) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (espansa) "Chiudi $titolo" else "Apri $titolo"
                )
            }
        }
    }
    if (espansa) {
        contenuto()
    }
}
