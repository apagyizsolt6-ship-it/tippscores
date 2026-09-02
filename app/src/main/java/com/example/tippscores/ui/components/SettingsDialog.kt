package com.example.tippscores.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.sp

@Composable
fun SettingsDialog(
    initialStatpalKey: String,
    initialHighlightlyKey: String,
    darkModeEnabled: Boolean,
    goalNotificationsEnabled: Boolean,
    onDismiss: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onGoalNotificationsChange: (Boolean) -> Unit,
    onSave: (
        statpalKey: String,
        highlightlyKey: String
    ) -> Unit
) {

    var statpalKey by
        remember(initialStatpalKey) {
            mutableStateOf(
                initialStatpalKey
            )
        }

    var highlightlyKey by
        remember(initialHighlightlyKey) {
            mutableStateOf(
                initialHighlightlyKey
            )
        }

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {

            Text(
                text =
                    "⚙️ Beállítások",

                fontWeight =
                    FontWeight.Bold,

                fontSize = 18.sp
            )
        },

        text = {

            Column {

                // ==================================================
                // MEGJELENÉS
                // ==================================================

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Sötét mód",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Text(
                            text = "Kényelmesebb esti meccsnézéshez",
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = darkModeEnabled,
                        onCheckedChange = onDarkModeChange
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Push értesítés gólnál",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Text(
                            text = "Kedvenc meccseknél/csapatoknál, kb. 15 percenként ellenőrizve",
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = goalNotificationsEnabled,
                        onCheckedChange = onGoalNotificationsChange
                    )
                }

                Spacer(Modifier.height(12.dp))

                HorizontalDivider()

                Spacer(Modifier.height(12.dp))

                // ==================================================
                // API KULCSOK
                // ==================================================

                Text(
                    text =
                        "Az alkalmazás a mérkőzésekhez a StatPal, a videókhoz pedig a Highlightly szolgáltatást használja.",

                    fontSize = 12.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                OutlinedTextField(

                    value =
                        statpalKey,

                    onValueChange = {
                        statpalKey = it
                    },

                    label = {
                        Text(
                            "StatPal API-kulcs"
                        )
                    },

                    placeholder = {
                        Text(
                            "Írd be a StatPal kulcsot"
                        )
                    },

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                OutlinedTextField(

                    value =
                        highlightlyKey,

                    onValueChange = {
                        highlightlyKey = it
                    },

                    label = {
                        Text(
                            "Highlightly API-kulcs"
                        )
                    },

                    placeholder = {
                        Text(
                            "Írd be a Highlightly kulcsot"
                        )
                    },

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    onSave(
                        statpalKey.trim(),
                        highlightlyKey.trim()
                    )
                }
            ) {

                Text(
                    "Mentés és frissítés"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Mégse"
                )
            }
        }
    )
}
