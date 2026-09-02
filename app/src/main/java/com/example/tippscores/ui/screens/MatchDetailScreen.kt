package com.example.tippscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.tippscores.data.model.LineupPlayer
import com.example.tippscores.data.model.Match
import com.example.tippscores.data.model.MatchDetails
import com.example.tippscores.data.model.MatchEvent
import com.example.tippscores.data.model.HeadToHeadMatch
import com.example.tippscores.data.model.MatchLineup
import com.example.tippscores.ui.theme.LocalAppColors

@Composable
fun MatchDetailScreen(
    match: Match?,
    details: MatchDetails?,
    detailsLoading: Boolean,
    detailsError: String?,
    onRetryDetails: () -> Unit,
    favoriteTeamNames: Set<String>,
    onBack: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleFollowTeam: (String) -> Unit,
    onTeamClick: (String, String) -> Unit
) {
    val colors = LocalAppColors.current
    var selectedTab by remember { mutableIntStateOf(0) }

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
                    text = "←",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(end = 14.dp)
                        .clickable(onClick = onBack)
                )
                Text(
                    text = "Mérkőzés részletei",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    ) { paddingValues ->
        if (match == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("⚽", fontSize = 36.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ez a mérkőzés már nem érhető el.",
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 14.dp,
                vertical = 14.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        match.leagueCountryFlag.ifBlank { "🏆" },
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        match.leagueCountry,
                        color = colors.secondaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        "  •  ",
                        color = colors.tertiaryText,
                        fontSize = 12.sp
                    )
                    Text(
                        match.leagueName,
                        color = colors.primaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            item {
                MatchHeaderCard(
                    match = match,
                    colors = colors,
                    favoriteTeamNames = favoriteTeamNames,
                    onToggleFollowTeam = onToggleFollowTeam,
                    onTeamClick = onTeamClick
                )
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleFavorite(match.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardBackground
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector =
                                if (match.isFavorite) Icons.Filled.Star
                                else Icons.Outlined.Star,
                            contentDescription = "Kedvenc meccs",
                            tint =
                                if (match.isFavorite) Color(0xFFF59E0B)
                                else colors.tertiaryText
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (match.isFavorite)
                                "Eltávolítás a kedvencekből"
                            else
                                "Hozzáadás a kedvencekhez",
                            color = colors.primaryText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = colors.cardBackground,
                    contentColor = Color(0xFF2563EB)
                ) {
                    listOf("Statisztika", "Események", "Összeállítás", "H2H").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                }
            }

            if (detailsLoading) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = colors.cardBackground
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Részletes adatok betöltése…",
                                color = colors.secondaryText,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else if (detailsError != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = colors.cardBackground
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "A részletes adatok most nem érhetők el.",
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                detailsError,
                                color = colors.tertiaryText,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(10.dp))
                            IconButton(onClick = onRetryDetails) {
                                Icon(Icons.Default.Refresh, "Újrapróbálás")
                            }
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        item {
                            StatisticsSection(
                                details = details,
                                homeTeam = match.homeTeam,
                                awayTeam = match.awayTeam
                            )
                        }
                    }

                    1 -> {
                        items(
                            items = details?.events.orEmpty(),
                            key = { "${it.minute}-${it.player}-${it.type}" }
                        ) { event ->
                            EventRow(event)
                        }
                        if (details?.events.isNullOrEmpty()) {
                            item { EmptyDetailsCard("Élő események ehhez a mérkőzéshez jelenleg nem érhetők el.") }
                        }
                    }

                    2 -> {
                        item {
                            LineupSection(
                                title = match.homeTeam,
                                lineup = details?.homeLineup ?: MatchLineup()
                            )
                        }
                        item {
                            LineupSection(
                                title = match.awayTeam,
                                lineup = details?.awayLineup ?: MatchLineup()
                            )
                        }
                        if (details?.hasLineups != true) {
                            item {
                                EmptyDetailsCard(
                                    "A kezdő 11 és a cserepad ehhez a mérkőzéshez jelenleg nem érhető el."
                                )
                            }
                        }
                    }

                    3 -> {
                        item {
                            HeadToHeadSection(
                                matches = details?.headToHead.orEmpty(),
                                homeTeam = match.homeTeam,
                                awayTeam = match.awayTeam
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchHeaderCard(
    match: Match,
    colors: com.example.tippscores.ui.theme.AppColorScheme,
    favoriteTeamNames: Set<String>,
    onToggleFollowTeam: (String) -> Unit,
    onTeamClick: (String, String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (match.isLive) colors.cardBackgroundLive else colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = colors.statusChipBackground
            ) {
                Text(
                    text = match.status,
                    color = if (match.isLive) Color(0xFFDC2626) else colors.secondaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            TeamDetailRow(
                name = match.homeTeam,
                logoUrl = match.homeTeamLogo,
                score = match.homeScore,
                isFollowed = favoriteTeamNames.contains(match.homeTeam),
                primaryText = colors.primaryText,
                onFollowClick = { onToggleFollowTeam(match.homeTeam) },
                onProfileClick = { onTeamClick(match.homeTeam, match.homeTeamLogo) }
            )

            Spacer(Modifier.height(12.dp))

            TeamDetailRow(
                name = match.awayTeam,
                logoUrl = match.awayTeamLogo,
                score = match.awayScore,
                isFollowed = favoriteTeamNames.contains(match.awayTeam),
                primaryText = colors.primaryText,
                onFollowClick = { onToggleFollowTeam(match.awayTeam) },
                onProfileClick = { onTeamClick(match.awayTeam, match.awayTeamLogo) }
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Koppints a csapat nevére a követéshez.",
                color = colors.tertiaryText,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun TeamDetailRow(
    name: String,
    logoUrl: String,
    score: Int?,
    isFollowed: Boolean,
    primaryText: Color,
    onFollowClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onFollowClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TeamLogo(
            logoUrl = logoUrl,
            teamName = name,
            size = 38.dp
        )
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = (if (isFollowed) "● " else "") + name,
                color = if (isFollowed) Color(0xFF2563EB) else primaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.clickable(onClick = onProfileClick)
            )
            Text(
                text = "Csapatprofil",
                color = Color(0xFF2563EB),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onProfileClick)
            )
        }

        Text(
            text = score?.toString() ?: "-",
            color = primaryText,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp
        )
    }
}

@Composable
private fun TeamLogo(
    logoUrl: String,
    teamName: String,
    size: androidx.compose.ui.unit.Dp
) {
    if (logoUrl.isNotBlank()) {
        AsyncImage(
            model = logoUrl,
            contentDescription = "$teamName logó",
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Fit
        )
    } else {
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = Color(0xFFEFF6FF)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("⚽", fontSize = (size.value * 0.42f).sp)
            }
        }
    }
}

@Composable
private fun StatisticsSection(
    details: MatchDetails?,
    homeTeam: String,
    awayTeam: String
) {
    val colors = LocalAppColors.current
    val stats = details?.statistics.orEmpty()

    if (stats.isEmpty()) {
        EmptyDetailsCard("Részletes mérkőzés-statisztika ehhez a mérkőzéshez jelenleg nem érhető el.")
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Mérkőzés statisztikák",
                color = colors.primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(homeTeam, fontSize = 11.sp, color = colors.secondaryText, modifier = Modifier.weight(1f))
                Text("STAT", fontSize = 10.sp, color = colors.tertiaryText, textAlign = TextAlign.Center, modifier = Modifier.width(90.dp))
                Text(awayTeam, fontSize = 11.sp, color = colors.secondaryText, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))

            stats.forEach { stat ->
                StatisticRow(stat)
            }
        }
    }
}

@Composable
private fun StatisticRow(stat: com.example.tippscores.data.model.MatchStatistic) {
    val colors = LocalAppColors.current

    val homeNumber = stat.home.replace(",", ".").replace("%", "").trim().toFloatOrNull()
    val awayNumber = stat.away.replace(",", ".").replace("%", "").trim().toFloatOrNull()
    val total = if (homeNumber != null && awayNumber != null) homeNumber + awayNumber else 0f
    val homeRatio = if (total > 0f) (homeNumber ?: 0f) / total else 0.5f
    val awayRatio = if (total > 0f) (awayNumber ?: 0f) / total else 0.5f

    Column(modifier = Modifier.padding(vertical = 7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stat.home,
                color = colors.primaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.width(46.dp)
            )
            Text(
                stat.label,
                color = colors.secondaryText,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                stat.away,
                color = colors.primaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.width(46.dp)
            )
        }

        if (homeNumber != null && awayNumber != null && total > 0f) {
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(homeRatio.coerceIn(0.05f, 0.95f))
                        .height(5.dp)
                        .background(Color(0xFF2563EB))
                )
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .weight(awayRatio.coerceIn(0.05f, 0.95f))
                        .height(5.dp)
                        .background(Color(0xFF94A3B8))
                )
            }
        }
    }
}

@Composable
private fun EventRow(event: MatchEvent) {
    val colors = LocalAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.cardBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                event.minute,
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                modifier = Modifier.width(40.dp)
            )
            Text(
                eventIcon(event.type),
                fontSize = 18.sp,
                modifier = Modifier.width(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${event.type} • ${event.player}",
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    listOfNotNull(event.team.takeIf { it != "–" }, event.assist?.let { "Gólpassz: $it" }, event.detail)
                        .joinToString(" • "),
                    color = colors.tertiaryText,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun eventIcon(type: String): String =
    when {
        type.contains("Gól", true) -> "⚽"
        type.contains("Sárga", true) -> "🟨"
        type.contains("Piros", true) -> "🟥"
        type.contains("Csere", true) -> "🔄"
        else -> "•"
    }

@Composable
private fun LineupSection(
    title: String,
    lineup: MatchLineup
) {
    val colors = LocalAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                title,
                color = colors.primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )

            val meta = listOfNotNull(
                lineup.formation?.takeIf { it.isNotBlank() },
                lineup.coach?.takeIf { it.isNotBlank() }
            ).joinToString(" • ")

            if (meta.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(meta, color = colors.tertiaryText, fontSize = 10.sp)
            }

            Spacer(Modifier.height(8.dp))

            if (lineup.startingPlayers.isNotEmpty()) {
                Text(
                    "Kezdő 11",
                    color = colors.secondaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(5.dp))
                lineup.startingPlayers.forEach { player ->
                    PlayerRow(player)
                }
            }

            if (lineup.substitutePlayers.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Cserepad",
                    color = colors.secondaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(5.dp))
                lineup.substitutePlayers.forEach { player ->
                    PlayerRow(player)
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(player: LineupPlayer) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!player.photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = player.photoUrl,
                contentDescription = "${player.name} fotója",
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = colors.statusChipBackground
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("👤", fontSize = 16.sp)
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        if (!player.number.isNullOrBlank()) {
            Text(
                player.number,
                color = colors.tertiaryText,
                fontSize = 10.sp,
                modifier = Modifier.width(24.dp)
            )
        }

        Text(
            player.name + if (player.isCaptain) " ©" else "",
            color = colors.primaryText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )

        player.position?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = colors.tertiaryText, fontSize = 10.sp)
        }
    }
}

@Composable
private fun HeadToHeadSection(
    matches: List<HeadToHeadMatch>,
    homeTeam: String,
    awayTeam: String
) {
    val colors = LocalAppColors.current

    if (matches.isEmpty()) {
        EmptyDetailsCard("Az utolsó 10 egymás elleni mérkőzés jelenleg nem érhető el.")
        return
    }

    val homeWins = matches.count {
        it.homeTeam.equals(homeTeam, true) && (it.homeScore.toIntOrNull() ?: -1) > (it.awayScore.toIntOrNull() ?: -1) ||
            it.awayTeam.equals(homeTeam, true) && (it.awayScore.toIntOrNull() ?: -1) > (it.homeScore.toIntOrNull() ?: -1)
    }
    val awayWins = matches.count {
        it.homeTeam.equals(awayTeam, true) && (it.homeScore.toIntOrNull() ?: -1) > (it.awayScore.toIntOrNull() ?: -1) ||
            it.awayTeam.equals(awayTeam, true) && (it.awayScore.toIntOrNull() ?: -1) > (it.homeScore.toIntOrNull() ?: -1)
    }
    val draws = matches.count {
        val h = it.homeScore.toIntOrNull()
        val a = it.awayScore.toIntOrNull()
        h != null && a != null && h == a
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = colors.cardBackground
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Egymás elleni mérleg", color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    H2HSummary(homeTeam, homeWins, colors.primaryText)
                    H2HSummary("Döntetlen", draws, colors.secondaryText)
                    H2HSummary(awayTeam, awayWins, colors.primaryText)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = colors.cardBackground
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Utolsó 10 egymás elleni", color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                matches.forEach { match ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatH2HDate(match.date), color = colors.tertiaryText, fontSize = 9.sp, modifier = Modifier.width(72.dp))
                        Column(Modifier.weight(1f)) {
                            Text(match.homeTeam, color = colors.primaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(match.awayTeam, color = colors.secondaryText, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${match.homeScore} – ${match.awayScore}", color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun H2HSummary(label: String, value: Int, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
        Text(value.toString(), color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text(label, color = LocalAppColors.current.tertiaryText, fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

private fun formatH2HDate(value: String): String =
    value.take(10).ifBlank { "–" }

@Composable
private fun EmptyDetailsCard(text: String) {
    val colors = LocalAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.cardBackground
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = colors.tertiaryText,
            fontSize = 11.sp
        )
    }
}
