package com.example.tippscores.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.data.model.Match
import com.example.tippscores.ui.components.LeagueHeader
import com.example.tippscores.ui.components.MatchRow
import com.example.tippscores.ui.components.SettingsDialog

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MatchListScreen(
    matches: List<Match>,
    statpalKey: String,
    highlightlyKey: String,
    onSaveKeys: (String, String) -> Unit,
    onMatchClick: (String) -> Unit
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        SettingsDialog(
            initialStatpalKey = statpalKey,
            initialHighlightlyKey = highlightlyKey,
            onDismiss = { showSettingsDialog = false },
            onSave = { sKey, hKey ->
                onSaveKeys(sKey, hKey)
                showSettingsDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color(0xFF0F172A))) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚽ Foci ▾", color = Color.White, fontWeight = FontWeight.Bold)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("EREDMÉNYEK", color = Color.White, fontSize = 12.sp)
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Beállítások",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Text("⚽") }, label = { Text("Összes", fontSize = 10.sp) })
                NavigationBarItem(selected = false, onClick = {}, icon = { Text("📡") }, label = { Text("ÉLŐ", fontSize = 10.sp) })
                NavigationBarItem(selected = false, onClick = {}, icon = { Text("★") }, label = { Text("Kedvencek", fontSize = 10.sp) })
                NavigationBarItem(selected = false, onClick = {}, icon = { Text("🏆") }, label = { Text("Bajnokság", fontSize = 10.sp) })
            }
        }
    ) { paddingValues ->
        val grouped = matches.groupBy { it.leagueName }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            grouped.forEach { (league, matchGroup) ->
                stickyHeader {
                    LeagueHeader(
                        country = matchGroup.first().leagueCountry,
                        leagueName = league
                    )
                }
                items(matchGroup) { match ->
                    MatchRow(match = match, onMatchClick = onMatchClick)
                }
            }
        }
    }
}
