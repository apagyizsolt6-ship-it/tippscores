package com.example.tippscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.data.model.PlayerMatchLog
import com.example.tippscores.data.model.PlayerProfile
import com.example.tippscores.data.model.PlayerSeasonStat
import com.example.tippscores.ui.components.RemoteImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    player: PlayerProfile?,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onTeamClick: (String) -> Unit,
    onMatchClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Áttekintés", "Statisztika", "Meccsek")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(player?.name ?: "Játékos profil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Vissza")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
            player != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    PlayerHeroHeader(player = player, onTeamClick = onTeamClick)

                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }

                    when (selectedTab) {
                        0 -> PlayerOverviewTab(player)
                        1 -> PlayerStatsTab(player.seasonStats)
                        2 -> PlayerMatchesTab(player.recentMatches, onMatchClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerHeroHeader(
    player: PlayerProfile,
    onTeamClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                RemoteImage(
                    url = player.photoUrl.orEmpty(),
                    contentDescription = player.name,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                if (player.jerseyNumber.isNotBlank()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#${player.jerseyNumber}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.fullName.ifBlank { player.name },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (player.teamName.isNotBlank()) {
                    Text(
                        text = player.teamName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(enabled = player.teamId.isNotBlank()) {
                                onTeamClick(player.teamId)
                            }
                            .padding(vertical = 2.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (player.position.isNotBlank()) {
                        AssistChip(onClick = {}, label = { Text(player.position) })
                    }
                    if (player.nationality.isNotBlank()) {
                        AssistChip(onClick = {}, label = { Text(player.nationality) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerOverviewTab(player: PlayerProfile) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Játékos adatok", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    BioRow("Életkor / Születés", listOfNotNull(player.age?.let { "$it év" }, player.birthDate.takeIf { it.isNotBlank() }).joinToString(" • "))
                    BioRow("Magasság / Súly", listOfNotNull(player.height.takeIf { it.isNotBlank() }, player.weight.takeIf { it.isNotBlank() }).joinToString(" / "))
                    BioRow("Erősebbik láb", player.preferredFoot)
                    BioRow("Piaci érték", player.marketValue)
                    BioRow("Szerződés lejárta", player.contractUntil)
                }
            }
        }

        if (player.trophies.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Trófeák és sikerek", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        HorizontalDivider()
                        player.trophies.forEach { trophy ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("🏆 ${trophy.title}", fontWeight = FontWeight.Medium)
                                Text("${trophy.season} (${trophy.teamName})", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BioRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PlayerStatsTab(stats: List<PlayerSeasonStat>) {
    if (stats.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nincs elérhető szezonstatisztika.")
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stats) { stat ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stat.leagueName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
if (stat.season.isNotBlank()) {
Text(stat.season, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
}
if (stat.rating > 0f) {
RatingBadge(rating = stat.rating)
}
}
HorizontalDivider()
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
StatCounter("Meccs", "${stat.appearances} (${stat.lineups})")
StatCounter("Perc", "${stat.minutesPlayed}'")
StatCounter("Gól", "${stat.goals}")
StatCounter("Gólpassz", "${stat.assists}")
}
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
StatCounter("Lövés (kapura)", "${stat.shotsTotal} (${stat.shotsOnTarget})")
StatCounter("Kulcspassz", "${stat.keyPasses}")
StatCounter("Passz %", if (stat.passAccuracy > 0f) "${stat.passAccuracy.toInt()}%" else "-")
StatCounter("Lapok", "🟨${stat.yellowCards} 🟥${stat.redCards}")
}
}
}
}
}
}
@Composable
private fun StatCounter(label: String, value: String) {
Column(horizontalAlignment = Alignment.CenterHorizontally) {
Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
}
@Composable
private fun PlayerMatchesTab(matches: List, onMatchClick: (String) -> Unit) {
if (matches.isEmpty()) {
Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
Text("Nincs rögzített mérkőzésnapló.")
}
return
}
LazyColumn(
contentPadding = PaddingValues(16.dp),
verticalArrangement = Arrangement.spacedBy(8.dp)
) {
items(matches) { match ->
Card(
shape = RoundedCornerShape(12.dp),
modifier = Modifier
.fillMaxWidth()
.clickable(enabled = match.matchId.isNotBlank()) { onMatchClick(match.matchId) }
) {
Row(
modifier = Modifier.padding(12.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.SpaceBetween
) {
Column(Modifier.weight(1f)) {
Text(match.opponent, fontWeight = FontWeight.Bold)
Text("${match.date} • Eredmény: ${match.score} • ${match.minutes}'", style = MaterialTheme.typography.bodySmall)
}
Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
if (match.goals > 0) Text("⚽${match.goals}", fontSize = 12.sp)
if (match.assists > 0) Text("👟${match.assists}", fontSize = 12.sp)
if (match.yellowCard) Text("🟨", fontSize = 12.sp)
if (match.redCard) Text("🟥", fontSize = 12.sp)
if (match.rating > 0f) RatingBadge(rating = match.rating)
}
}
}
}
}
}
@Composable
fun RatingBadge(rating: Float, modifier: Modifier = Modifier) {
val bgColor = when {
rating >= 8.5f -> Color(0xFF0088FF) // Kiemelkedő kék
rating >= 7.0f -> Color(0xFF16A34A) // Jó zöld
rating >= 6.0f -> Color(0xFFD97706) // Átlagos narancs
else -> Color(0xFFDC2626)           // Gyenge piros
}
Surface(
color = bgColor,
shape = RoundedCornerShape(6.dp),
modifier = modifier
) {
Text(
text = String.format("%.1f", rating),
color = Color.White,
fontWeight = FontWeight.Bold,
fontSize = 12.sp,
modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
)
}
}
