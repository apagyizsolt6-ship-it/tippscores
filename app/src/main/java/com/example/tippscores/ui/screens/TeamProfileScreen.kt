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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tippscores.data.model.LeaguePlayerStat
import com.example.tippscores.data.model.LeagueStats
import com.example.tippscores.data.model.StandingRow
import com.example.tippscores.data.model.TeamLeagueRecord
import com.example.tippscores.data.model.TeamMinuteBucket
import com.example.tippscores.data.model.TeamProfile
import com.example.tippscores.data.model.TeamSquadPlayer
import com.example.tippscores.data.remote.StatpalImages
import com.example.tippscores.data.repository.StandingsRepository
import com.example.tippscores.data.repository.TeamProfileRepository
import com.example.tippscores.data.util.JsonX
import com.example.tippscores.ui.components.RemoteImage
import com.example.tippscores.ui.theme.AppColorScheme
import com.example.tippscores.ui.theme.LocalAppColors

private val WinColor = Color(0xFF16A34A)
private val DrawColor = Color(0xFFD97706)
private val LossColor = Color(0xFFDC2626)

@Composable
fun TeamProfileScreen(
    teamId: String?,
    teamName: String,
    fallbackLogoUrl: String,
    repository: TeamProfileRepository,
    onBack: () -> Unit,
    onCoachClick: (String, String) -> Unit = { _, _ -> },
    standingsRepository: StandingsRepository? = null
) {
    val colors = LocalAppColors.current
    var profile by remember(teamId, teamName) { mutableStateOf<TeamProfile?>(null) }
    var loading by remember(teamId, teamName) { mutableStateOf(true) }
    var error by remember(teamId, teamName) { mutableStateOf<String?>(null) }

    var leagueStats by remember(teamId, teamName) { mutableStateOf<LeagueStats?>(null) }
    val leagueId = profile?.leagueRecords?.firstOrNull()?.leagueId

    // A bajnoki játékos-statisztikák külön, a profil betöltése után érkeznek.
    LaunchedEffect(leagueId) {
        leagueStats = if (leagueId.isNullOrBlank()) {
            null
        } else {
            try {
                repository.fetchLeagueStats(leagueId)
            } catch (e: Exception) {
                null
            }
        }
    }

    // Tabella-helyzet és forma (az utolsó meccsek betűi) a bajnoki tabellából.
    var standing by remember(teamId, teamName) { mutableStateOf<StandingRow?>(null) }

    LaunchedEffect(leagueId) {
        standing = if (leagueId.isNullOrBlank() || standingsRepository == null) {
            null
        } else {
            try {
                val rows = standingsRepository.fetchStandings(leagueId)
                val ownId = profile?.id ?: teamId
                val ownName = JsonX.norm(profile?.name ?: teamName)
                rows.firstOrNull { it.teamId != null && it.teamId == ownId }
                    ?: rows.firstOrNull { JsonX.norm(it.team) == ownName }
            } catch (e: Exception) {
                null
            }
        }
    }

    LaunchedEffect(teamId, teamName) {
        loading = true
        error = null
        profile = try {
            repository.fetch(teamId, teamName)
        } catch (e: Exception) {
            null
        }
        if (profile == null) error = "A csapat profiladatai jelenleg nem érhetők el."
        loading = false
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
                        .clickableCompat(onClick = onBack)
                )
                Text(
                    "Csapatprofil",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ProfileHeader(
                    teamId = profile?.id ?: teamId,
                    name = profile?.name ?: teamName,
                    logoUrl = profile?.logoUrl?.takeIf { it.isNotBlank() } ?: fallbackLogoUrl,
                    country = profile?.country,
                    league = profile?.league,
                    founded = profile?.founded,
                    colors = colors
                )
            }

            if (loading) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.cardBackground
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(30.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("Csapatadatok betöltése…", color = colors.secondaryText, fontSize = 12.sp)
                        }
                    }
                }
            } else if (error != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.cardBackground
                    ) {
                        Text(
                            error!!,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            color = colors.secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val p = profile
                if (p != null) {
                    profileSections(p, leagueStats, standing, colors, onCoachClick)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.profileSections(
    p: TeamProfile,
    leagueStats: LeagueStats?,
    standing: StandingRow?,
    colors: AppColorScheme,
    onCoachClick: (String, String) -> Unit
) {
    // --- Stadion + edző ---
    val hasVenue = !p.venueName.isNullOrBlank() || !p.coachName.isNullOrBlank()
    if (hasVenue) {
        item {
            SectionCard(title = "Stadion és edző", colors = colors) {
                InfoRow("Stadion", p.venueName, colors)
                InfoRow("Város", p.venueCity, colors)
                InfoRow("Befogadóképesség", p.venueCapacity?.let { formatNumber(it) }, colors)
                InfoRow("Pálya", p.venueSurface, colors)
                InfoRow(
                    label = "Edző",
                    value = p.coachName?.let { if (p.coachId != null) "$it ›" else it },
                    colors = colors,
                    onClick = if (p.coachId != null && p.coachName != null) {
                        { onCoachClick(p.coachId, p.coachName) }
                    } else {
                        null
                    }
                )
            }
        }
    }

    // --- Tabella-helyzet + forma ---
    if (standing != null) {
        item {
            SectionCard(title = "Tabella-helyzet", colors = colors) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${standing.position}.",
                        color = colors.primaryText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "${standing.points} pont • ${standing.played} mérkőzés",
                            color = colors.primaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            "${standing.won} GY • ${standing.drawn} D • ${standing.lost} V • gólok ${standing.goalsFor}:${standing.goalsAgainst}",
                            color = colors.secondaryText,
                            fontSize = 11.sp
                        )
                    }
                }

                val form = standing.form?.takeIf { it.isNotBlank() }
                if (form != null) {
                    Spacer(Modifier.height(10.dp))
                    Text("Forma (utolsó mérkőzések)", color = colors.tertiaryText, fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                    StandingFormRow(form)
                }

                standing.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = colors.secondaryText, fontSize = 11.sp)
                }
            }
        }
    }

    // --- Szezonmérleg bajnokságonként ---
    p.leagueRecords.forEach { record ->
        item {
            SectionCard(
                title = "Mérleg – ${record.leagueName}" +
                    if (record.season.isNotBlank()) " (${record.season})" else "",
                colors = colors
            ) {
                RecordTable(record, colors)
            }
        }

        if (record.scoringMinutes.isNotEmpty()) {
            item {
                SectionCard(title = "Lőtt gólok időszakonként", colors = colors) {
                    MinuteBars(record.scoringMinutes, WinColor, colors)
                }
            }
        }

        if (record.concededMinutes.isNotEmpty()) {
            item {
                SectionCard(title = "Kapott gólok időszakonként", colors = colors) {
                    MinuteBars(record.concededMinutes, LossColor, colors)
                }
            }
        }
    }

    // --- Forma (ha később elérhető) ---
    if (p.form.isNotEmpty()) {
        item {
            SectionCard(title = "Forma – utolsó 5", colors = colors) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    p.form.take(5).forEach { item ->
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = resultColor(item.result)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = item.result,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                p.form.take(5).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.label, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(item.opponent, color = colors.tertiaryText, fontSize = 10.sp)
                        }
                        Text(item.score, color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // --- Szezonstatisztikák (ha később elérhető) ---
    if (p.statistics.isNotEmpty()) {
        item {
            SectionCard(title = "Szezonstatisztikák", colors = colors) {
                p.statistics.forEach { stat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stat.label, color = colors.secondaryText, fontSize = 12.sp)
                        Text(stat.value, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // --- Utolsó mérkőzések (ha később elérhető) ---
    if (p.recentMatches.isNotEmpty()) {
        item {
            SectionCard(title = "Utolsó 5 mérkőzés", colors = colors) {
                p.recentMatches.forEach { game ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(30.dp),
                            shape = CircleShape,
                            color = resultColor(game.result)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(game.result, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                            }
                        }
                        Spacer(Modifier.size(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(game.opponent, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${game.homeTeam} – ${game.awayTeam}", color = colors.tertiaryText, fontSize = 9.sp)
                        }
                        Text(
                            "${game.homeScore} – ${game.awayScore}",
                            color = colors.primaryText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    // --- Játékosok szezonstatisztikája + bajnoki listák ---
    if (leagueStats != null) {
        val ordering = compareByDescending<LeaguePlayerStat> { it.goals }
            .thenByDescending { it.assists }
            .thenByDescending { it.minutes }

        val own = leagueStats.players
            .filter { it.teamId == p.id && (it.appearances > 0 || it.minutes > 0) }
            .sortedWith(ordering)
            .take(20)

        if (own.isNotEmpty()) {
            item {
                SectionCard(title = "Játékosok – ${leagueStats.leagueName}", colors = colors) {
                    own.forEach { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    player.name,
                                    color = colors.primaryText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "${player.appearances} mérk. • ${player.minutes} perc",
                                    color = colors.tertiaryText,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                "${player.goals} gól • ${player.assists} assz." +
                                    (player.rating?.let { " • ⌀ %.1f".format(it) } ?: ""),
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        val topScorers = leagueStats.players
            .filter { it.goals > 0 }
            .sortedWith(ordering)
            .take(5)

        if (topScorers.isNotEmpty()) {
            item {
                SectionCard(title = "Gólkirály-lista – ${leagueStats.leagueName}", colors = colors) {
                    TopPlayerRows(topScorers, { it.goals }, p.id, colors)
                }
            }
        }

        val topAssists = leagueStats.players
            .filter { it.assists > 0 }
            .sortedWith(
                compareByDescending<LeaguePlayerStat> { it.assists }
                    .thenByDescending { it.goals }
                    .thenBy { it.minutes }
            )
            .take(5)

        if (topAssists.isNotEmpty()) {
            item {
                SectionCard(title = "Gólpassz-lista – ${leagueStats.leagueName}", colors = colors) {
                    TopPlayerRows(topAssists, { it.assists }, p.id, colors)
                }
            }
        }
    }

    // --- Keret ---
    if (p.squad.isNotEmpty()) {
        item {
            SectionCard(title = "Keret (${p.squad.size} fő)", colors = colors) {
                val groups = p.squad.groupBy { positionGroup(it.position) }
                listOf("Kapusok", "Védők", "Középpályások", "Támadók", "Egyéb").forEach { group ->
                    val players = groups[group].orEmpty()
                    if (players.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            group,
                            color = colors.secondaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        players.forEach { player -> SquadRow(player, colors) }
                    }
                }
            }
        }
    }

    // --- Átigazolások ---
    if (p.transfers.isNotEmpty()) {
        item {
            SectionCard(title = "Átigazolások", colors = colors) {
                p.transfers.take(12).forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (t.isIncoming) "▲" else "▼",
                            color = if (t.isIncoming) WinColor else LossColor,
                            fontSize = 12.sp,
                            modifier = Modifier.width(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.playerName, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            val detail = listOfNotNull(
                                t.otherTeam?.let { if (t.isIncoming) "innen: $it" else "ide: $it" },
                                t.type,
                                t.price
                            ).joinToString(" • ")
                            if (detail.isNotBlank()) {
                                Text(detail, color = colors.tertiaryText, fontSize = 10.sp)
                            }
                        }
                        Text(t.date, color = colors.secondaryText, fontSize = 10.sp)
                    }
                }
            }
        }
    }

    // --- Trófeák ---
    if (p.trophies.isNotEmpty()) {
        item {
            SectionCard(title = "Trófeák", colors = colors) {
                p.trophies.forEach { trophy ->
                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                trophy.league,
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                listOfNotNull(trophy.status, trophy.count?.let { "×$it" })
                                    .joinToString(" "),
                                color = colors.secondaryText,
                                fontSize = 11.sp
                            )
                        }
                        if (!trophy.seasons.isNullOrBlank()) {
                            Text(trophy.seasons, color = colors.tertiaryText, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopPlayerRows(
    players: List<LeaguePlayerStat>,
    value: (LeaguePlayerStat) -> Int,
    ownTeamId: String,
    colors: AppColorScheme
) {
    players.forEachIndexed { index, player ->
        val isOwn = player.teamId == ownTeamId
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${index + 1}.",
                modifier = Modifier.width(26.dp),
                color = colors.tertiaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    player.name,
                    color = if (isOwn) WinColor else colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(player.teamName, color = colors.tertiaryText, fontSize = 10.sp)
            }
            Text(
                value(player).toString(),
                color = colors.primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun RecordTable(record: TeamLeagueRecord, colors: AppColorScheme) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("", modifier = Modifier.weight(1.4f))
        listOf("Össz.", "Hazai", "Idegen").forEach { title ->
            Text(
                title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = colors.secondaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }

    RecordRow("Győzelem", record.winsTotal, record.winsHome, record.winsAway, WinColor, colors)
    RecordRow("Döntetlen", record.drawsTotal, record.drawsHome, record.drawsAway, DrawColor, colors)
    RecordRow("Vereség", record.lossesTotal, record.lossesHome, record.lossesAway, LossColor, colors)

    Spacer(Modifier.height(6.dp))
    Text(
        "Lejátszott mérkőzés: ${record.played}",
        color = colors.tertiaryText,
        fontSize = 10.sp
    )
}

@Composable
private fun RecordRow(
    label: String,
    total: Int,
    home: Int,
    away: Int,
    accent: Color,
    colors: AppColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1.4f),
            color = accent,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        listOf(total, home, away).forEach { value ->
            Text(
                value.toString(),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = colors.primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun MinuteBars(
    buckets: List<TeamMinuteBucket>,
    barColor: Color,
    colors: AppColorScheme
) {
    val maxCount = (buckets.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
    buckets.forEach { bucket ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                bucket.range,
                modifier = Modifier.width(52.dp),
                color = colors.secondaryText,
                fontSize = 11.sp
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.divider)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(bucket.count.toFloat() / maxCount)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                )
            }
            Text(
                "${bucket.count}",
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.End,
                color = colors.primaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SquadRow(player: TeamSquadPlayer, colors: AppColorScheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            player.number ?: "–",
            modifier = Modifier.width(28.dp),
            color = colors.tertiaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        Text(
            buildString {
                append(player.name)
                if (player.isCaptain) append(" (C)")
                if (player.injured) append(" 🩹")
            },
            modifier = Modifier.weight(1f),
            color = colors.primaryText,
            fontSize = 12.sp
        )
        if (!player.age.isNullOrBlank()) {
            Text("${player.age} év", color = colors.secondaryText, fontSize = 10.sp)
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String?,
    colors: AppColorScheme,
    onClick: (() -> Unit)? = null
) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colors.secondaryText, fontSize = 12.sp)
        Text(
            value,
            color = if (onClick != null) Color(0xFF2563EB) else colors.primaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun ProfileHeader(
    teamId: String?,
    name: String,
    logoUrl: String,
    country: String?,
    league: String?,
    founded: String?,
    colors: AppColorScheme
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RemoteImage(
                url = logoUrl.takeIf { it.isNotBlank() } ?: StatpalImages.team(teamId),
                contentDescription = "$name logó",
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit,
                fallback = { Text("⚽", fontSize = 52.sp) }
            )
            Spacer(Modifier.height(10.dp))
            Text(name, color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, textAlign = TextAlign.Center)
            val subtitle = listOfNotNull(
                country?.takeIf { it.isNotBlank() },
                league?.takeIf { it.isNotBlank() },
                founded?.takeIf { it.isNotBlank() }?.let { "alapítva: $it" }
            ).joinToString(" • ")
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    subtitle,
                    color = colors.secondaryText,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    colors: AppColorScheme,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(title, color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Spacer(Modifier.height(7.dp))
            content()
        }
    }
}

@Composable
private fun StandingFormRow(form: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        form.take(6).forEach { letter ->
            val (label, color) = when (letter.uppercaseChar()) {
                'W' -> "GY" to Color(0xFF16A34A)
                'D' -> "D" to Color(0xFFD97706)
                'L' -> "V" to Color(0xFFDC2626)
                else -> "?" to Color(0xFF94A3B8)
            }
            Surface(
                modifier = Modifier.size(26.dp),
                shape = CircleShape,
                color = color
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

private fun resultColor(result: String): Color =
    when (result) {
        "GY" -> Color(0xFFD1FAE5)
        "V" -> Color(0xFFFEE2E2)
        else -> Color(0xFFFEF3C7)
    }

private fun positionGroup(position: String?): String =
    when (position?.trim()?.uppercase()) {
        "G", "GK", "GOALKEEPER" -> "Kapusok"
        "D", "DEFENDER" -> "Védők"
        "M", "MIDFIELDER" -> "Középpályások"
        "F", "A", "FORWARD", "ATTACKER" -> "Támadók"
        else -> "Egyéb"
    }

private fun formatNumber(raw: String): String {
    val n = raw.trim().toLongOrNull() ?: return raw
    return n.toString().reversed().chunked(3).joinToString(" ").reversed()
}

private fun Modifier.clickableCompat(onClick: () -> Unit): Modifier =
    clickable(onClick = onClick)
