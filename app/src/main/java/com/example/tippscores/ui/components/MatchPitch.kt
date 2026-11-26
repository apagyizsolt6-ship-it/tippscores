package com.example.tippscores.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.data.model.LeaguePlayerStat
import com.example.tippscores.data.model.LineupPlayer
import com.example.tippscores.data.model.MatchEvent
import com.example.tippscores.data.model.MatchLineup
import com.example.tippscores.data.remote.StatpalImages
import com.example.tippscores.ui.theme.AppColorScheme
import com.example.tippscores.ui.theme.LocalAppColors
import java.text.Normalizer
import java.util.Locale

private val HomeColor = Color(0xFF2563EB)
private val AwayColor = Color(0xFFDC2626)
private val KeeperColor = Color(0xFFF59E0B)

// ================================================================
// PUBLIKUS BELÉPÉSI PONT
// ================================================================

/**
 * Egyetlen pályán mutatja mindkét csapat kezdőcsapatát (hazai felül,
 * vendég alul), a formációkkal, a kapcsolható hőtérképpel, a gól /
 * gólpassz / lap / csere jelzésekkel a játékosokon, az interaktív
 * gyors-kártyával, az edzőkkel és a cserepadokkal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPitchLineupCard(
    homeTeam: String,
    awayTeam: String,
    home: MatchLineup,
    away: MatchLineup,
    events: List<MatchEvent>,
    onPlayerClick: (LineupPlayer, String) -> Unit,
    onCoachClick: (String, String) -> Unit,
    playerStats: Map<String, LeaguePlayerStat> = emptyMap()
) {
    val colors = LocalAppColors.current

    var showHeatMap by remember { mutableStateOf(false) }

    // 0 = név, 1 = értékelés, 2 = gól, 3 = gólpassz, 4 = kor (szezonadat a bajnokságból)
    var overlayMode by remember { mutableIntStateOf(0) }

    val overlayText: (LineupPlayer) -> String? = { player ->
        val stat = player.id?.let { playerStats[it] }
        when (overlayMode) {
            1 -> stat?.rating?.let { String.format(Locale.US, "%.1f", it) }
            2 -> stat?.goals?.let { "$it gól" }
            3 -> stat?.assists?.let { "$it assz." }
            4 -> stat?.age?.takeIf { it.isNotBlank() }?.let { "$it év" }
            else -> null
        }
    }
    var selectedPlayerState by remember { mutableStateOf<Triple<LineupPlayer, String, PlayerBadges>?>(null) }

    val homeRows = remember(home) { buildRows(home) }
    val awayRows = remember(away) { buildRows(away) }

    val homeBadges = remember(home, events) { badgesFor(home, events, homeTeam, true) }
    val awayBadges = remember(away, events) { badgesFor(away, events, awayTeam, false) }

    selectedPlayerState?.let { (player, teamName, badges) ->
        PlayerQuickMatchSheet(
            player = player,
            teamName = teamName,
            badges = badges,
            colors = colors,
            seasonStat = player.id?.let { playerStats[it] },
            onDismiss = { selectedPlayerState = null },
            onOpenFullProfile = {
                selectedPlayerState = null
                onPlayerClick(player, teamName)
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamHeader(
                    name = homeTeam,
                    formation = home.formation,
                    color = HomeColor,
                    alignEnd = false,
                    colors = colors,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = showHeatMap,
                    onClick = { showHeatMap = !showHeatMap },
                    label = {
                        Text(
                            text = if (showHeatMap) "🔥 Hőtérkép: BE" else "🔥 Hőtérkép",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Spacer(Modifier.width(8.dp))
                TeamHeader(
                    name = awayTeam,
                    formation = away.formation,
                    color = AwayColor,
                    alignEnd = true,
                    colors = colors,
                    modifier = Modifier.weight(1f)
                )
            }

            if (home.isProjected || away.isProjected) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Text(
                        "Várható összeállítás – a hivatalos kezdőcsapat még változhat",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(0xFF92400E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (playerStats.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Név", "Ért.", "Gól", "Assz.", "Kor").forEachIndexed { index, label ->
                        FilterChip(
                            selected = overlayMode == index,
                            onClick = { overlayMode = index },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                LineupSummaryStrip(homeTeam, awayTeam, home, away, playerStats, colors)
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (overlayMode != 0) 0.46f else 0.54f)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                PitchBackground()

                if (showHeatMap) {
                    PitchHeatMapLayer(
                        homeRows = homeRows,
                        awayRows = awayRows.asReversed(),
                        homeBadges = homeBadges,
                        awayBadges = awayBadges
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    PitchHalf(
                        rows = homeRows,
                        baseColor = HomeColor,
                        badges = homeBadges,
                        emptyText = "$homeTeam: nincs kezdőcsapat",
                        overlayText = overlayText,
                        onPlayerClick = { p ->
                            selectedPlayerState = Triple(p, homeTeam, homeBadges[p] ?: PlayerBadges())
                        },
                        modifier = Modifier.weight(1f)
                    )

                    PitchHalf(
                        rows = awayRows.asReversed(),
                        baseColor = AwayColor,
                        badges = awayBadges,
                        emptyText = "$awayTeam: nincs kezdőcsapat",
                        overlayText = overlayText,
                        onPlayerClick = { p ->
                            selectedPlayerState = Triple(p, awayTeam, awayBadges[p] ?: PlayerBadges())
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Legend(colors)

            CoachLine(homeTeam, home, HomeColor, colors, onCoachClick)
            CoachLine(awayTeam, away, AwayColor, colors, onCoachClick)

            BenchSection(homeTeam, home, HomeColor, homeBadges, colors) { p ->
                selectedPlayerState = Triple(p, homeTeam, homeBadges[p] ?: PlayerBadges())
            }
            BenchSection(awayTeam, away, AwayColor, awayBadges, colors) { p ->
                selectedPlayerState = Triple(p, awayTeam, awayBadges[p] ?: PlayerBadges())
            }
        }
    }
}

// ================================================================
// GYORS JÁTÉKOS MECCS-KÁRTYA (BOTTOM SHEET)
// ================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerQuickMatchSheet(
    player: LineupPlayer,
    teamName: String,
    badges: PlayerBadges,
    colors: AppColorScheme,
    seasonStat: LeaguePlayerStat? = null,
    onDismiss: () -> Unit,
    onOpenFullProfile: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val imageUrl = player.photoUrl?.takeIf { it.isNotBlank() }
                    ?: StatpalImages.player(player.id)

                RemoteImage(
                    url = imageUrl,
                    contentDescription = player.name,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(2.dp, HomeColor, CircleShape),
                    contentScale = ContentScale.Crop,
                    fallback = {
                        NumberCircle(
                            number = player.number,
                            fill = HomeColor,
                            isKeeper = roleOf(player.position) == 0
                        )
                    }
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildString {
                            if (!player.number.isNullOrBlank()) append("#${player.number} ")
                            append(player.name)
                            if (player.isCaptain) append(" (C)")
                        },
                        color = colors.primaryText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "$teamName • ${positionLabel(player.position)}",
                        color = colors.secondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            HorizontalDivider(color = colors.divider)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickStatPill("⚽ Gól", "${badges.goals}", colors)
                QuickStatPill("👟 Gólpassz", "${badges.assists}", colors)
                QuickStatPill("🟨 Sárga", "${badges.yellow}", colors)
                QuickStatPill("🟥 Piros", "${badges.red}", colors)
                QuickStatPill("⏱ Csere", badges.subMinute?.let { "$it'" } ?: "–", colors)
            }

            if (seasonStat != null) {
                HorizontalDivider(color = colors.divider)
                Text(
                    "Szezon a bajnokságban",
                    color = colors.tertiaryText,
                    fontSize = 10.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    QuickStatPill("Mérkőzés", "${seasonStat.appearances}", colors)
                    QuickStatPill("Gól", "${seasonStat.goals}", colors)
                    QuickStatPill("Gólpassz", "${seasonStat.assists}", colors)
                    QuickStatPill(
                        "Átl. értékelés",
                        seasonStat.rating?.let { String.format(Locale.US, "%.2f", it) } ?: "–",
                        colors
                    )
                }
            }

            Button(
                onClick = onOpenFullProfile,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HomeColor)
            ) {
                Text(
                    "Teljes játékos profil megnyitása",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun QuickStatPill(label: String, value: String, colors: AppColorScheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        Text(label, color = colors.tertiaryText, fontSize = 10.sp)
    }
}

// ================================================================
// HŐTÉRKÉP RÉTEG
// ================================================================

@Composable
private fun PitchHeatMapLayer(
    homeRows: List<List<LineupPlayer>>,
    awayRows: List<List<LineupPlayer>>,
    homeBadges: Map<LineupPlayer, PlayerBadges>,
    awayBadges: Map<LineupPlayer, PlayerBadges>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val halfH = size.height / 2f

        fun drawHalfHeat(rows: List<List<LineupPlayer>>, topY: Float, badges: Map<LineupPlayer, PlayerBadges>) {
            if (rows.isEmpty()) return
            val rowStep = halfH / (rows.size + 1)
            rows.forEachIndexed { rIdx, row ->
                val y = topY + rowStep * (rIdx + 1)
                val colStep = size.width / (row.size + 1)
                row.forEachIndexed { cIdx, player ->
                    val x = colStep * (cIdx + 1)
                    val b = badges[player]
                    val boost = 1f + ((b?.goals ?: 0) * 0.25f) + ((b?.assists ?: 0) * 0.15f)
                    val radius = (size.width * 0.16f * boost).coerceAtMost(size.width * 0.26f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF3D00).copy(alpha = 0.45f),
                                Color(0xFFFFEA00).copy(alpha = 0.24f),
                                Color.Transparent
                            ),
                            center = Offset(x, y),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(x, y)
                    )
                }
            }
        }

        drawHalfHeat(homeRows, 0f, homeBadges)
        drawHalfHeat(awayRows, halfH, awayBadges)
    }
}

// ================================================================
// FEJLÉC, LEGENDA, EDZŐ, CSEREPAD
// ================================================================

@Composable
private fun TeamHeader(
    name: String,
    formation: String?,
    color: Color,
    alignEnd: Boolean,
    colors: AppColorScheme,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!alignEnd) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                name,
                color = colors.primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (alignEnd) {
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            }
        }
        if (!formation.isNullOrBlank()) {
            Text(formation, color = colors.secondaryText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun Legend(colors: AppColorScheme) {
    Text(
        "⚽ gól   👟 gólpassz   🟨/🟥 lap   🔻/🔺 csere   Ⓒ kapitány",
        color = colors.tertiaryText,
        fontSize = 10.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CoachLine(
    teamName: String,
    lineup: MatchLineup,
    teamColor: Color,
    colors: AppColorScheme,
    onCoachClick: (String, String) -> Unit
) {
    val coach = lineup.coach?.takeIf { it.isNotBlank() } ?: return
    val coachId = lineup.coachId?.takeIf { it.isNotBlank() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(teamColor))
        Spacer(Modifier.width(6.dp))
        Text("$teamName – edző: ", color = colors.secondaryText, fontSize = 11.sp)
        if (coachId != null) {
            Text(
                "$coach ›",
                color = HomeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.clickable { onCoachClick(coachId, coach) }
            )
        } else {
            Text(coach, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BenchSection(
    teamName: String,
    lineup: MatchLineup,
    teamColor: Color,
    badges: Map<LineupPlayer, PlayerBadges>,
    colors: AppColorScheme,
    onPlayerClick: (LineupPlayer) -> Unit
) {
    if (lineup.substitutePlayers.isEmpty()) return

    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = colors.divider)
    Spacer(Modifier.height(8.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(teamColor))
        Spacer(Modifier.width(6.dp))
        Text(
            "Cserepad – $teamName",
            color = colors.primaryText,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp
        )
    }

    lineup.substitutePlayers.forEach { player ->
        val b = badges[player]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPlayerClick(player) }
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                player.number ?: "–",
                modifier = Modifier.width(26.dp),
                color = colors.tertiaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                player.name + if (player.isCaptain) " (C)" else "",
                modifier = Modifier.weight(1f),
                color = colors.primaryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
            if (b != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    if (b.subbedIn && b.subMinute != null) {
                        Text("🔺${b.subMinute}'", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    }
                    if (b.goals > 0) Text("⚽${if (b.goals > 1) b.goals else ""}", fontSize = 10.sp)
                    if (b.assists > 0) Text("👟", fontSize = 10.sp)
                    if (b.yellow > 0) Text("🟨", fontSize = 10.sp)
                    if (b.red > 0) Text("🟥", fontSize = 10.sp)
                }
            }
            Text(positionLabel(player.position), color = colors.tertiaryText, fontSize = 10.sp)
        }
    }
}

// ================================================================
// PÁLYA
// ================================================================

@Composable
private fun PitchBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stripes = 12
        val stripeHeight = size.height / stripes
        for (i in 0 until stripes) {
            drawRect(
                color = if (i % 2 == 0) Color(0xFF1F8A4C) else Color(0xFF1A7A42),
                topLeft = Offset(0f, i * stripeHeight),
                size = Size(size.width, stripeHeight + 1f)
            )
        }

        val lineColor = Color.White.copy(alpha = 0.55f)
        val stroke = Stroke(width = 1.5.dp.toPx())
        val margin = 8.dp.toPx()
        val w = size.width
        val h = size.height

        drawRect(
            color = lineColor,
            topLeft = Offset(margin, margin),
            size = Size(w - margin * 2, h - margin * 2),
            style = stroke
        )
        drawLine(lineColor, Offset(margin, h / 2f), Offset(w - margin, h / 2f), stroke.width)
        drawCircle(lineColor, radius = w * 0.14f, center = Offset(w / 2f, h / 2f), style = stroke)
        drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(w / 2f, h / 2f))

        val boxW = w * 0.58f
        val boxH = h * 0.15f
        val sixW = w * 0.28f
        val sixH = h * 0.055f

        drawRect(lineColor, Offset((w - boxW) / 2f, margin), Size(boxW, boxH), style = stroke)
        drawRect(lineColor, Offset((w - sixW) / 2f, margin), Size(sixW, sixH), style = stroke)
        drawCircle(lineColor, 2.dp.toPx(), Offset(w / 2f, margin + boxH * 0.72f))

        drawRect(lineColor, Offset((w - boxW) / 2f, h - margin - boxH), Size(boxW, boxH), style = stroke)
        drawRect(lineColor, Offset((w - sixW) / 2f, h - margin - sixH), Size(sixW, sixH), style = stroke)
        drawCircle(lineColor, 2.dp.toPx(), Offset(w / 2f, h - margin - boxH * 0.72f))
    }
}

@Composable
private fun PitchHalf(
    rows: List<List<LineupPlayer>>,
    baseColor: Color,
    badges: Map<LineupPlayer, PlayerBadges>,
    emptyText: String,
    onPlayerClick: (LineupPlayer) -> Unit,
    overlayText: (LineupPlayer) -> String? = { null },
    modifier: Modifier = Modifier
) {
    if (rows.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                emptyText,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { player ->
                    PitchMarker(
                        player = player,
                        baseColor = baseColor,
                        badges = badges[player] ?: PlayerBadges(),
                        overlayText = overlayText(player),
                        onClick = { onPlayerClick(player) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PitchMarker(
    player: LineupPlayer,
    baseColor: Color,
    badges: PlayerBadges,
    overlayText: String? = null,
    onClick: () -> Unit
) {
    val isKeeper = roleOf(player.position) == 0
    val fill = if (isKeeper) KeeperColor else baseColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.size(40.dp)) {
            val imageUrl = player.photoUrl?.takeIf { it.isNotBlank() }
                ?: StatpalImages.player(player.id)

            RemoteImage(
                url = imageUrl,
                contentDescription = "${player.name} fotója",
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .border(2.dp, fill, CircleShape),
                contentScale = ContentScale.Crop,
                fallback = {
                    NumberCircle(
                        number = player.number,
                        fill = fill,
                        isKeeper = isKeeper,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            )

            if (player.isCaptain) {
                Surface(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.TopEnd),
                    shape = CircleShape,
                    color = Color(0xFFFBBF24)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("C", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF422006))
                    }
                }
            }

            if (badges.goals > 0 || badges.assists > 0) {
                Row(modifier = Modifier.align(Alignment.TopStart)) {
                    if (badges.goals > 0) {
                        Text(
                            text = if (badges.goals > 1) "⚽${badges.goals}" else "⚽",
                            fontSize = 10.sp
                        )
                    }
                    if (badges.assists > 0) {
                        Text(
                            text = if (badges.assists > 1) "👟${badges.assists}" else "👟",
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (badges.subbedOut) {
                Surface(
                    color = Color(0xCC000000),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-4).dp)
                ) {
                    Text(
                        text = "🔻${badges.subMinute ?: ""}'",
                        color = Color(0xFFFCA5A5),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }

            val cards = buildString {
                if (badges.yellow > 0) append("🟨")
                if (badges.red > 0) append("🟥")
            }
            if (cards.isNotEmpty()) {
                Text(
                    text = cards,
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = lastName(player.name),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(Color(0x99000000), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )

        if (!overlayText.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = overlayText,
                color = Color(0xFF0F172A),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                modifier = Modifier
                    .background(Color(0xFFFBBF24), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}

/** Mindkét kezdőcsapat szezonadatainak összesítése (átlagértékelés, gólok, gólpasszok, átlagéletkor). */
@Composable
private fun LineupSummaryStrip(
    homeTeam: String,
    awayTeam: String,
    home: MatchLineup,
    away: MatchLineup,
    playerStats: Map<String, LeaguePlayerStat>,
    colors: AppColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryColumn(homeTeam, home, HomeColor, playerStats, colors, Modifier.weight(1f))
        SummaryColumn(awayTeam, away, AwayColor, playerStats, colors, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryColumn(
    teamName: String,
    lineup: MatchLineup,
    teamColor: Color,
    playerStats: Map<String, LeaguePlayerStat>,
    colors: AppColorScheme,
    modifier: Modifier = Modifier
) {
    val stats = lineup.startingPlayers.mapNotNull { player -> player.id?.let { playerStats[it] } }

    val rating = stats.mapNotNull { it.rating }.takeIf { it.isNotEmpty() }?.average()
    val age = stats.mapNotNull { it.age?.toIntOrNull() }.takeIf { it.isNotEmpty() }?.average()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = teamColor.copy(alpha = 0.10f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                teamName,
                color = teamColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Átl. értékelés: " + (rating?.let { String.format(Locale.US, "%.2f", it) } ?: "–"),
                color = colors.secondaryText,
                fontSize = 10.sp
            )
            Text(
                "Gól / gólpassz: ${stats.sumOf { it.goals }} / ${stats.sumOf { it.assists }}",
                color = colors.secondaryText,
                fontSize = 10.sp
            )
            Text(
                "Átl. életkor: " + (age?.let { String.format(Locale.US, "%.1f", it) } ?: "–"),
                color = colors.secondaryText,
                fontSize = 10.sp
            )
        }
    }
}

// ================================================================
// SORBA RENDEZÉS
// ================================================================

private fun roleOf(position: String?): Int {
    val p = position?.trim()?.lowercase(Locale.ROOT).orEmpty()
    return when {
        p.isEmpty() -> 4
        p == "g" || p == "gk" || p.contains("keeper") || p.contains("kapus") -> 0
        p == "d" || p.contains("defen") || p.contains("back") || p.contains("véd") -> 1
        p == "m" || p.contains("mid") || p.contains("közép") -> 2
        p == "f" || p == "a" || p.contains("forw") || p.contains("attack") ||
            p.contains("strik") || p.contains("wing") || p.contains("támad") -> 3
        else -> 4
    }
}

private fun positionLabel(position: String?): String =
    when (roleOf(position)) {
        0 -> "Kapus"
        1 -> "Védő"
        2 -> "Középpályás"
        3 -> "Támadó"
        else -> position.orEmpty()
    }

private fun buildRows(lineup: MatchLineup): List<List<LineupPlayer>> {
    val players = lineup.startingPlayers
    if (players.isEmpty()) return emptyList()

    val keeperIndex = players.indexOfFirst { roleOf(it.position) == 0 }
        .let { if (it >= 0) it else 0 }
    val keeper = players[keeperIndex]
    val outfield = players.filterIndexed { index, _ -> index != keeperIndex }

    val rows = mutableListOf<List<LineupPlayer>>()
    rows.add(listOf(keeper))

    val sizes = lineup.formation
        ?.split("-", "–", ":", "/")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.filter { it > 0 }
        .orEmpty()

    if (sizes.isNotEmpty() && sizes.sum() == outfield.size) {
        var cursor = 0
        sizes.forEach { size ->
            rows.add(outfield.subList(cursor, cursor + size).toList())
            cursor += size
        }
        return rows
    }

    val groups = outfield.groupBy { roleOf(it.position) }
    if (groups.isNotEmpty() && groups.keys.all { it in 1..3 }) {
        listOf(1, 2, 3).forEach { role -> groups[role]?.let { rows.add(it) } }
        return rows
    }

    outfield.chunked(4).forEach { rows.add(it) }
    return rows
}

// ================================================================
// GÓL / GÓLPASSZ / LAP / CSERE JELZÉSEK A JÁTÉKOSOKON
// ================================================================

private data class PlayerBadges(
    val goals: Int = 0,
    val assists: Int = 0,
    val yellow: Int = 0,
    val red: Int = 0,
    val subbedOut: Boolean = false,
    val subbedIn: Boolean = false,
    val subMinute: String? = null
)

private fun badgesFor(
    lineup: MatchLineup,
    events: List<MatchEvent>,
    teamName: String,
    isHome: Boolean
): Map<LineupPlayer, PlayerBadges> {
    if (events.isEmpty()) return emptyMap()

    val teamEvents = events.filter { eventBelongs(it, teamName, isHome) }
    if (teamEvents.isEmpty()) return emptyMap()

    val everyone = lineup.startingPlayers + lineup.substitutePlayers
    val result = mutableMapOf<LineupPlayer, PlayerBadges>()

    everyone.forEach { player ->
        var goals = 0
        var assists = 0
        var yellow = 0
        var red = 0
        var subbedOut = false
        var subbedIn = false
        var subMinute: String? = null

        teamEvents.forEach { event ->
            val kind = eventKind(event)
            if (samePlayer(event.player, player, everyone)) {
                when (kind) {
                    1 -> goals++
                    2 -> yellow++
                    3 -> red++
                    4 -> {
                        subbedOut = true
                        subMinute = event.minute.filter { it.isDigit() }
                    }
                }
            }
            val assistName = event.assist.orEmpty()
            if (assistName.isNotBlank() && samePlayer(assistName, player, everyone)) {
                when (kind) {
                    1 -> assists++
                    4 -> {
                        subbedIn = true
                        subMinute = event.minute.filter { it.isDigit() }
                    }
                }
            }
        }
        if (goals + assists + yellow + red > 0 || subbedOut || subbedIn) {
            result[player] = PlayerBadges(goals, assists, yellow, red, subbedOut, subbedIn, subMinute)
        }
    }
    return result
}

private fun eventKind(event: MatchEvent): Int {
    val type = event.type.lowercase(Locale.ROOT)
    val detail = event.detail.orEmpty().lowercase(Locale.ROOT)
    return when {
        type.contains("sub") || type.contains("csere") -> 4
        type.contains("own") || type.contains("öngól") || detail.contains("own") -> 0
        type.contains("sárga") || type.contains("yellow") ->
            if (type.contains("red") || type.contains("piros") || type.contains("yellowred")) 3 else 2
        type.contains("piros") || type.contains("red") -> 3
        type.contains("gól") || type.contains("goal") -> 1
        type.contains("penalty") || type.contains("tizenegyes") ->
            if (detail.contains("miss") || detail.contains("kihagy") ||
                detail.contains("saved") || detail.contains("véd")
            ) 0 else 1
        else -> 0
    }
}

private fun eventBelongs(event: MatchEvent, teamName: String, isHome: Boolean): Boolean {
    val t = normalize(event.team)
    if (t.isEmpty()) return true
    if (t == "home") return isHome
    if (t == "away") return !isHome
    val name = normalize(teamName)
    return t.contains(name) || name.contains(t)
}

private fun samePlayer(
    eventName: String,
    player: LineupPlayer,
    everyone: List<LineupPlayer>
): Boolean {
    val eventFull = normalize(eventName)
    val playerFull = normalize(player.name)
    if (eventFull.isNotEmpty() && eventFull == playerFull) return true

    val eventLast = normalize(lastName(eventName))
    val playerLast = normalize(lastName(player.name))
    if (eventLast.isEmpty() || eventLast != playerLast) return false

    val sameLastCount = everyone.count { normalize(lastName(it.name)) == playerLast }
    if (sameLastCount > 1) {
        val eventInitial = eventName.trim().firstOrNull()?.lowercaseChar()
        val playerInitial = player.name.trim().firstOrNull()?.lowercaseChar()
        return eventInitial != null && eventInitial == playerInitial
    }
    return true
}

private fun normalize(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }

private fun lastName(fullName: String): String {
    val parts = fullName.trim().split(" ").filter { it.isNotBlank() }
    return if (parts.isEmpty()) fullName else parts.last()
}

@Composable
private fun NumberCircle(
    number: String?,
    fill: Color,
    isKeeper: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(34.dp)
            .border(1.5.dp, Color.White, CircleShape),
        shape = CircleShape,
        color = fill
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number?.takeIf { it.isNotBlank() } ?: "·",
                color = if (isKeeper) Color(0xFF422006) else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
