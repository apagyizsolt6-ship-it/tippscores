package com.example.tippscores.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.tippscores.data.model.PlayerProfile
import com.example.tippscores.data.repository.MatchDetailsRepository
import com.example.tippscores.ui.theme.LocalAppColors
import java.util.Locale

@Composable
fun MatchDetailScreen(
    match: Match?,
    details: MatchDetails?,
    detailsLoading: Boolean,
    detailsError: String?,
    detailsRepository: MatchDetailsRepository,
    onRetryDetails: () -> Unit,
    favoriteTeamNames: Set<String>,
    onBack: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleFollowTeam: (String) -> Unit,
    onTeamClick: (String, String) -> Unit
) {
    val colors = LocalAppColors.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // Kártyán megnyitott játékos (pálya nézetben rákoppintva).
    var selectedPlayer by remember { mutableStateOf<LineupPlayer?>(null) }
    var selectedPlayerTeam by remember { mutableStateOf("") }

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
                if (details?.hasPrediction == true || details?.hasWeather == true || details?.hasInjuries == true) {
                    ExtraInfoCard(details)
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
                            item { EmptyDetailsCard("Még nincsenek események (gól, lap, csere).") }
                        }
                    }

                    2 -> {
                        item {
                            LineupSection(
                                title = match.homeTeam,
                                lineup = details?.homeLineup ?: MatchLineup(),
                                onPlayerClick = { player ->
                                    selectedPlayer = player
                                    selectedPlayerTeam = match.homeTeam
                                }
                            )
                        }
                        item {
                            LineupSection(
                                title = match.awayTeam,
                                lineup = details?.awayLineup ?: MatchLineup(),
                                onPlayerClick = { player ->
                                    selectedPlayer = player
                                    selectedPlayerTeam = match.awayTeam
                                }
                            )
                        }
                        if (details?.hasLineups != true) {
                            item {
                                EmptyDetailsCard("A kezdőcsapat és a cserepad jelenleg nem érhető el.")
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

    val currentPlayer = selectedPlayer

    if (currentPlayer != null) {

        var playerProfile by remember(currentPlayer.id) { mutableStateOf<PlayerProfile?>(null) }
        var profileLoading by remember(currentPlayer.id) { mutableStateOf(currentPlayer.id != null) }

        LaunchedEffect(currentPlayer.id) {
            val playerId = currentPlayer.id
            if (playerId != null) {
                profileLoading = true
                playerProfile = try {
                    detailsRepository.fetchPlayerProfile(playerId)
                } catch (e: Exception) {
                    null
                }
                profileLoading = false
            }
        }

        PlayerCardDialog(
            player = currentPlayer,
            teamName = selectedPlayerTeam,
            profile = playerProfile,
            profileLoading = profileLoading,
            onDismiss = { selectedPlayer = null }
        )
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
        EmptyDetailsCard("A részletes statisztika ehhez a mérkőzéshez jelenleg nem érhető el.")
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
        type.contains("Gól", true) || type.contains("goal", true) -> "⚽"
        type.contains("Sárga", true) || type.contains("yellow", true) -> "🟨"
        type.contains("Piros", true) || type.contains("red", true) -> "🟥"
        type.contains("Csere", true) || type.contains("sub", true) || type.contains("substitution", true) -> "🔄"
        type.contains("Penalty", true) || type.contains("tizenegyes", true) -> "⚽"
        type.contains("Own", true) || type.contains("öngól", true) -> "⚽"
        else -> "•"
    }

@Composable
private fun LineupSection(
    title: String,
    lineup: MatchLineup,
    onPlayerClick: (LineupPlayer) -> Unit
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

            Spacer(Modifier.height(10.dp))

            if (lineup.startingPlayers.isNotEmpty()) {
                PitchView(lineup = lineup, onPlayerClick = onPlayerClick)
            }

            if (lineup.substitutePlayers.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Cserepad",
                    color = colors.secondaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(5.dp))
                lineup.substitutePlayers.forEach { player ->
                    PlayerRow(player, onClick = { onPlayerClick(player) })
                }
            }
        }
    }
}

// ================================================================
// PÁLYAKÉP (kezdő 11, formáció szerint elrendezve)
// ================================================================

@Composable
private fun PitchView(lineup: MatchLineup, onPlayerClick: (LineupPlayer) -> Unit) {

    val rows = remember(lineup) { buildPitchRows(lineup) }

    if (rows.isEmpty()) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E8449), Color(0xFF14532D))
                )
            )
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = Color.White.copy(alpha = 0.55f)
            val stroke = Stroke(width = 1.8.dp.toPx())
            val margin = 6.dp.toPx()
            val w = size.width
            val h = size.height

            // Outer boundary
            drawRect(color = lineColor, topLeft = Offset(margin, margin),
                size = Size(w - margin * 2, h - margin * 2), style = stroke)

            // Halfway line
            drawLine(lineColor, Offset(margin, h / 2f), Offset(w - margin, h / 2f), stroke.width)

            // Center circle
            drawCircle(lineColor, radius = w * 0.14f, center = Offset(w / 2f, h / 2f), style = stroke)
            drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(w / 2f, h / 2f))

            // Penalty areas
            val boxW = w * 0.62f
            val boxH = h * 0.16f
            drawRect(lineColor, Offset((w - boxW) / 2f, margin), Size(boxW, boxH), style = stroke)
            drawRect(lineColor, Offset((w - boxW) / 2f, h - margin - boxH), Size(boxW, boxH), style = stroke)

            // Goal areas (6-yard boxes)
            val goalW = w * 0.32f
            val goalH = h * 0.07f
            drawRect(lineColor, Offset((w - goalW) / 2f, margin), Size(goalW, goalH), style = stroke)
            drawRect(lineColor, Offset((w - goalW) / 2f, h - margin - goalH), Size(goalW, goalH), style = stroke)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 14.dp, horizontal = 4.dp),

            verticalArrangement = Arrangement.SpaceEvenly
        ) {

            // A pályán a kapus legyen legalul, a támadók legfelül -
            // ezért fordítva jelenítjük meg, mint ahogy a formáció
            // (védők -> középpályások -> támadók) sorolja őket.
            rows.asReversed().forEach { row ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { player ->
                        PitchPlayerMarker(player, onClick = { onPlayerClick(player) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PitchPlayerMarker(player: LineupPlayer, onClick: () -> Unit) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .clickable(onClick = onClick)
    ) {

        Box {

            if (!player.photoUrl.isNullOrBlank()) {

                AsyncImage(
                    model = player.photoUrl,
                    contentDescription = "${player.name} fotója",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )

            } else {

                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.5.dp, Color.White, CircleShape),
                    shape = CircleShape,
                    color = Color(0xFF0F172A).copy(alpha = 0.55f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = player.number?.takeIf { it.isNotBlank() } ?: "👤",
                            color = Color.White,
                            fontSize = if (player.number.isNullOrBlank()) 14.sp else 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (player.isCaptain) {

                Surface(
                    modifier = Modifier
                        .size(15.dp)
                        .align(Alignment.TopEnd),
                    shape = CircleShape,
                    color = Color(0xFFFBBF24)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "C",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF422006)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = shortenPlayerName(player.name),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp)
        )
    }
}

// ================================================================
// JÁTÉKOSKÁRTYA (pályán/cserepadon koppintva)
// ================================================================

@Composable
private fun PlayerCardDialog(
    player: LineupPlayer,
    teamName: String,
    profile: PlayerProfile?,
    profileLoading: Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.cardBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!player.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = player.photoUrl,
                            contentDescription = "${player.name} fotója",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF2563EB), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .size(96.dp)
                                .border(2.dp, Color(0xFF2563EB), CircleShape),
                            shape = CircleShape,
                            color = colors.statusChipBackground
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    player.number?.takeIf { it.isNotBlank() } ?: "👤",
                                    fontSize = 30.sp,
                                    color = colors.primaryText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    player.name + if (player.isCaptain) " (C)" else "",
                    color = colors.primaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    listOfNotNull(
                        teamName.takeIf { it.isNotBlank() },
                        player.number?.takeIf { it.isNotBlank() }?.let { "#$it" },
                        (profile?.position ?: player.position)?.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = colors.divider)
                Spacer(Modifier.height(14.dp))

                when {

                    profileLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF2563EB),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    profile != null -> {
                        PlayerInfoRow("Kor", profile.age)
                        PlayerInfoRow("Nemzetiség", profile.nationality)
                        PlayerInfoRow("Magasság", profile.height?.let { "$it cm" })
                        PlayerInfoRow("Testsúly", profile.weight?.let { "$it kg" })
                        PlayerInfoRow("Erősebb láb", translatePreferredFoot(profile.preferredFoot))
                        PlayerInfoRow("Piaci érték", formatMarketValue(profile.marketValueEur))

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = colors.divider)
                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Szezon statisztika",
                            color = colors.tertiaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Spacer(Modifier.height(4.dp))

                        PlayerInfoRow("Mérkőzések", profile.appearances)
                        PlayerInfoRow("Gólok", profile.goals)
                        PlayerInfoRow("Gólpasszok", profile.assists)
                        PlayerInfoRow("Percek a pályán", profile.minutesPlayed)
                        PlayerInfoRow("Értékelés", profile.rating)
                        PlayerInfoRow("Sárga lap", profile.yellowCards)
                        PlayerInfoRow("Piros lap", profile.redCards)
                    }

                    player.id == null -> {
                        Text(
                            "Ehhez a mérkőzéshez a StatPal nem ad játékos-azonosítót, így bővebb adat jelenleg nem tölthető be.",
                            color = colors.tertiaryText,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> {
                        Text(
                            "A további adatok jelenleg nem érhetők el ehhez a játékoshoz.",
                            color = colors.tertiaryText,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDismiss),
                    shape = RoundedCornerShape(10.dp),
                    color = colors.statusChipBackground
                ) {
                    Text(
                        "Bezárás",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        color = colors.primaryText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerInfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            label,
            color = colors.tertiaryText,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            color = colors.primaryText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

private fun translatePreferredFoot(foot: String?): String? =
    when (foot?.trim()?.lowercase(Locale.ROOT)) {
        "left" -> "Bal"
        "right" -> "Jobb"
        "both" -> "Mindkettő"
        null, "" -> null
        else -> foot
    }

private fun formatMarketValue(raw: String?): String? {
    val value = raw?.toLongOrNull() ?: return null
    return when {
        value >= 1_000_000 -> "%.1f M €".format(value / 1_000_000.0)
        value >= 1_000 -> "%.0f E €".format(value / 1_000.0)
        else -> "$value €"
    }
}

/** Csak a vezetéknevet mutatjuk a pályán - kevés a hely egy teljes névnek. */
private fun shortenPlayerName(fullName: String): String {
    val parts = fullName.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> fullName
        else -> parts.last()
    }
}

/**
 * A kezdő 11-et sorokba rendezi (kapus, védők, középpályások, támadók)
 * a formáció string alapján (pl. "4-3-3"). Ha a formáció hiányzik vagy
 * nem egyezik a játékosok számával, egy ésszerű alapértelmezett
 * elrendezést használunk, hogy akkor is legyen pályakép.
 */
private fun buildPitchRows(lineup: MatchLineup): List<List<LineupPlayer>> {

    val players = lineup.startingPlayers

    if (players.isEmpty()) {
        return emptyList()
    }

    val keeperIndex = players.indexOfFirst { player ->
        val pos = player.position?.uppercase(Locale.ROOT).orEmpty()
        pos.contains("GK") || pos.contains("KAPUS") ||
            pos.contains("GOALKEEPER") || pos.contains("KEEPER")
    }.let { if (it >= 0) it else 0 }

    val keeper = players[keeperIndex]
    val outfield = players.filterIndexed { index, _ -> index != keeperIndex }

    val rowSizes = parseFormationRows(lineup.formation, outfield.size)

    val rows = mutableListOf<List<LineupPlayer>>()
    rows.add(listOf(keeper))

    var cursor = 0
    for (rowSize in rowSizes) {
        val row = outfield.drop(cursor).take(rowSize)
        if (row.isNotEmpty()) {
            rows.add(row)
        }
        cursor += rowSize
    }

    if (cursor < outfield.size) {
        rows.add(outfield.drop(cursor))
    }

    return rows
}

private fun parseFormationRows(formation: String?, outfieldCount: Int): List<Int> {

    val parsed = formation
        ?.split("-", "–", ":", "/")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.filter { it > 0 }
        .orEmpty()

    val sum = parsed.sum()

    return when {
        parsed.isNotEmpty() && sum == outfieldCount -> parsed

        outfieldCount > 0 -> {
            // Nincs használható formáció - kb. 3-4 fős sorokra osztjuk,
            // hogy akkor is legyen áttekinthető pályakép.
            val rows = mutableListOf<Int>()
            var remaining = outfieldCount
            while (remaining > 0) {
                val take = if (remaining > 4) 4 else remaining
                rows.add(take)
                remaining -= take
            }
            rows
        }

        else -> emptyList()
    }
}

@Composable
private fun PlayerRow(player: LineupPlayer, onClick: () -> Unit = {}) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        EmptyDetailsCard("Az egymás elleni mérkőzések adatai jelenleg nem érhetők el.")
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

@Composable
private fun ExtraInfoCard(details: MatchDetails?) {
    val colors = LocalAppColors.current
    if (details == null) return
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (details.hasPrediction) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Előrejelzés", color = colors.tertiaryText, fontSize = 10.sp)
                        Text(details.prediction ?: "", color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            if (details.hasWeather) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌤️", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Időjárás", color = colors.tertiaryText, fontSize = 10.sp)
                        Text(details.weather ?: "", color = colors.secondaryText, fontSize = 13.sp)
                    }
                }
            }
            if (details.hasInjuries) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏥", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("Sérültek / eltiltások", color = colors.tertiaryText, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    details.injuries.forEach { injury ->
                        Text("• $injury", color = colors.primaryText, fontSize = 12.sp, modifier = Modifier.padding(start = 28.dp))
                    }
                }
            }
        }
    }
}
