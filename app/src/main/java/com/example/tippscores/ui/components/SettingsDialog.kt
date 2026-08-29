package com.example.tippscores.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var statpalKey by remember { mutableStateOf(initialStatpalKey) }
    var highlightlyKey by remember { mutableStateOf(initialHighlightlyKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API Kulcsok Beállítása", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = statpalKey,
                    onValueChange = { statpalKey = it },
                    label = { Text("Statpal API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = highlightlyKey,
                    onValueChange = { highlightlyKey = it },
                    label = { Text("Highlightly API Key (RapidAPI)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(statpalKey, highlightlyKey) }) {
                Text("Mentés & Frissítés")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Mégse")
            }
        }
    )
}
