package com.example.tippscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.data.model.LeaguePlayerStat
import com.example.tippscores.data.model.LeagueStats
import com.example.tippscores.data.model.Match
import com.example.tippscores.data.model.StandingRow
import com.example.tippscores.data.repository.StandingsRepository
import com.example.tippscores.data.repository.TeamProfileRepository
import com.example.tippscores.ui.components.rememberHu
import com.example.tippscores.ui.theme.AppColorScheme
import com.example.tippscores.ui.theme.LocalAppColors

private val Accent = Color(0xFF2563EB)

/** Bajnokság oldal: tabella, gólkirályok, gólpasszok és a mai meccsek. */
@Composable
fun LeagueScreen(
    leagueId: String,
    leagueName: String,
    country: String,
    flag: String,
    standingsRepository: StandingsRepository,
    teamRepository: TeamProfileRepository,
    leagueMatches: List<Match>,
    onMatchClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current

    var tab by remember { mutableIntStateOf(0) }

    var standings by remember(leagueId) { mutableStateOf<List<StandingRow>?>(null) }
    var stats by remember(leagueId) { mutableStateOf<LeagueStats?>(null) }
    var statsLoading by remember(leagueId) { mutableStateOf(false) }
    var statsTried by remember(leagueId) { mutableStateOf(false) }

    LaunchedEffect(leagueId) {
        standings = try {
            standingsRepository.fetchStandings(leagueId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    LaunchedEffect(leagueId, tab) {
        if ((tab == 1 || tab == 2) && stats == null) {
            statsLoading = true
            stats = try {
                teamRepository.fetchLeagueStats(leagueId)
            } catch (e: Exception) {
                null
            }
            statsLoading = false
            statsTried = true
        }
    }

    Scaffold(
        containerColor = colors.screenBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.topBar)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "←",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(end = 14.dp)
                        .clickable(onClick = onBack)
                )
                Text(
                    (flag.ifBlank { "🏆" }) + " " + leagueName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (country.isNotBlank()) {
                Text(
                    country,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = colors.secondaryText,
                    fontSize = 11.sp
                )
            }

            ScrollableTabRow(
                selectedTabIndex = tab,
                containerColor = colors.cardBackground,
                contentColor = Accent,
                edgePadding = 8.dp
            ) {
                listOf("Tabella", "Gólkirályok", "Gólpasszok", "Mai meccsek").forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (tab) {
                    0 -> {
                        val rows = standings
                        when {
                            rows == null -> item { LoadingCard(colors) }
                            rows.isEmpty() -> item {
                                InfoCard(
                                    "A tabella ehhez a bajnoksághoz jelenleg nem érhető el.",
                                    colors
                                )
                            }
                            else -> {
                                item { StandingsHeader(colors) }
                                items(rows, key = { it.team }) { row ->
                                    StandingsRowView(row, colors)
                                }
                            }
                        }
                    }

                    1, 2 -> {
                        val data = stats
                        when {
                            data == null && (statsLoading || !statsTried) -> item { LoadingCard(colors) }
                            data == null -> item {
                                InfoCard("A játékos-statisztika nem érhető el.", colors)
                            }
                            else -> {
                                val goals = tab == 1
                                val ranked = data.players
                                    .filter { (if (goals) it.goals else it.assists) > 0 }
                                    .sortedWith(
                                        if (goals) {
                                            compareByDescending<LeaguePlayerStat> { it.goals }
                                                .thenByDescending { it.assists }
                                                .thenBy { it.minutes }
                                        } else {
                                            compareByDescending<LeaguePlayerStat> { it.assists }
                                                .thenByDescending { it.goals }
                                                .thenBy { it.minutes }
                                        }
                                    )
                                    .take(25)

                                if (ranked.isEmpty()) {
                                    item { InfoCard("Még nincs adat.", colors) }
                                } else {
                                    items(
                                        ranked.withIndex().toList(),
                                        key = { it.value.playerId ?: "${it.index}-${it.value.name}" }
                                    ) { indexed ->
                                        PlayerRankRow(
                                            rank = indexed.index + 1,
                                            player = indexed.value,
                                            value = if (goals) indexed.value.goals else indexed.value.assists,
                                            colors = colors
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        if (leagueMatches.isEmpty()) {
                            item {
                                InfoCard(
                                    "Ebben a bajnokságban a kiválasztott napon nincs mérkőzés.",
                                    colors
                                )
                            }
                        } else {
                            items(leagueMatches, key = { it.id }) { match ->
                                MatchMiniRow(match, colors) { onMatchClick(match.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StandingsHeader(colors: AppColorScheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell("#", 24, colors)
        Text(
            "Csapat",
            modifier = Modifier.weight(1f),
            color = colors.tertiaryText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        HeaderCell("M", 26, colors)
        HeaderCell("GY", 26, colors)
        HeaderCell("D", 26, colors)
        HeaderCell("V", 26, colors)
        HeaderCell("Gól", 46, colors)
        HeaderCell("P", 30, colors)
    }
}

@Composable
private fun HeaderCell(text: String, widthDp: Int, colors: AppColorScheme) {
    Text(
        text,
        modifier = Modifier.width(widthDp.dp),
        color = colors.tertiaryText,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun StandingsRowView(row: StandingRow, colors: AppColorScheme) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = colors.cardBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cell(row.position.toString(), 24, colors, bold = true)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.team,
                    color = colors.primaryText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val form = row.form?.takeIf { it.isNotBlank() }
                val description = row.description?.takeIf { it.isNotBlank() }

                if (form != null || description != null) {
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (form != null) {
                            FormDots(form)
                        }
                        if (description != null) {
                            if (form != null) Spacer(Modifier.width(6.dp))
                            Text(
                                rememberHu(description) ?: description,
                                color = descriptionColor(description, colors),
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Cell(row.played.toString(), 26, colors)
            Cell(row.won.toString(), 26, colors)
            Cell(row.drawn.toString(), 26, colors)
            Cell(row.lost.toString(), 26, colors)
            Cell("${row.goalsFor}:${row.goalsAgainst}", 46, colors)
            Cell(row.points.toString(), 30, colors, bold = true)
        }
    }
}

@Composable
private fun FormDots(form: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        form.take(6).forEach { letter ->
            val color = when (letter.uppercaseChar()) {
                'W' -> Color(0xFF16A34A)
                'D' -> Color(0xFFD97706)
                'L' -> Color(0xFFDC2626)
                else -> Color(0xFF94A3B8)
            }
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

private fun descriptionColor(description: String, colors: AppColorScheme): Color {
    val text = description.lowercase()
    return when {
        text.contains("relegation") -> Color(0xFFDC2626)
        text.contains("promotion") || text.contains("champions") -> Color(0xFF16A34A)
        text.contains("europa") || text.contains("conference") || text.contains("playoff") ->
            Color(0xFF2563EB)
        else -> colors.tertiaryText
    }
}

@Composable
private fun Cell(text: String, widthDp: Int, colors: AppColorScheme, bold: Boolean = false) {
    Text(
        text,
        modifier = Modifier.width(widthDp.dp),
        color = colors.primaryText,
        fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Normal,
        fontSize = 11.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PlayerRankRow(
    rank: Int,
    player: LeaguePlayerStat,
    value: Int,
    colors: AppColorScheme
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.cardBackground
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$rank.",
                modifier = Modifier.width(30.dp),
                color = colors.tertiaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    player.name,
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    "${player.teamName} • ${player.appearances} mérk.",
                    color = colors.tertiaryText,
                    fontSize = 10.sp
                )
            }
            Text(
                value.toString(),
                color = colors.primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun MatchMiniRow(match: Match, colors: AppColorScheme, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = colors.cardBackground
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(match.homeTeam, color = colors.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(2.dp))
                Text(match.awayTeam, color = colors.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${match.homeScore ?: "-"}",
                    color = colors.primaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Text(
                    "${match.awayScore ?: "-"}",
                    color = colors.primaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
            Text(
                match.status,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .width(64.dp),
                color = if (match.isLive) Color(0xFFDC2626) else colors.secondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun LoadingCard(colors: AppColorScheme) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
            Spacer(Modifier.height(8.dp))
            Text("Betöltés…", color = colors.secondaryText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun InfoCard(text: String, colors: AppColorScheme) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.cardBackground
    ) {
        Text(
            text,
            modifier = Modifier.padding(18.dp),
            color = colors.secondaryText,
            fontSize = 12.sp
        )
    }
}
