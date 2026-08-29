package com.example.tippscores.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsDialog(
    initialStatpalKey: String,
    initialHighlightlyKey: String,
    onDismiss: () -> Unit,
    onSave: (statpalKey: String, highlightlyKey: String) -> Unit
) {
    var statpalKey by remember(initialStatpalKey) { mutableStateOf(initialStatpalKey) }
    var highlightlyKey by remember(initialHighlightlyKey) { mutableStateOf(initialHighlightlyKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "API-kulcsok",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    "Az adatok betöltéséhez szükséges API-kulcsok.",
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = statpalKey,
                    onValueChange = { statpalKey = it },
                    label = { Text("StatPal API-kulcs") },
                    placeholder = { Text("StatPal kulcs") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = highlightlyKey,
                    onValueChange = { highlightlyKey = it },
                    label = { Text("Highlightly API-kulcs") },
                    placeholder = { Text("Highlightly kulcs") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(statpalKey.trim(), highlightlyKey.trim())
                }
            ) {
                Text("Mentés és frissítés")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Mégse")
            }
        }
    )
}
