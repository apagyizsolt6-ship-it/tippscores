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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import coil.compose.AsyncImage
import com.example.tippscores.data.model.LineupPlayer
import com.example.tippscores.data.model.MatchEvent
import com.example.tippscores.data.model.MatchLineup
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
 * vendég alul), a formációkkal, a gól / lap jelzésekkel a játékosokon,
 * az edzőkkel és a cserepadokkal.
 */
@Composable
fun FullPitchLineupCard(
    homeTeam: String,
    awayTeam: String,
    home: MatchLineup,
    away: MatchLineup,
    events: List<MatchEvent>,
    onPlayerClick: (LineupPlayer, String) -> Unit,
    onCoachClick: (String, String) -> Unit
) {
    val colors = LocalAppColors.current

    val homeRows = remember(home) { buildRows(home) }
    val awayRows = remember(away) { buildRows(away) }

    val homeBadges = remember(home, events) { badgesFor(home, events, homeTeam, true) }
    val awayBadges = remember(away, events) { badgesFor(away, events, awayTeam, false) }

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
                Spacer(Modifier.width(10.dp))
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

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.54f)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                PitchBackground()

                Column(modifier = Modifier.fillMaxSize()) {

                    // Hazai: kapus fent, támadók a középvonal felé.
                    PitchHalf(
                        rows = homeRows,
                        baseColor = HomeColor,
                        badges = homeBadges,
                        emptyText = "$homeTeam: nincs kezdőcsapat",
                        onPlayerClick = { onPlayerClick(it, homeTeam) },
                        modifier = Modifier.weight(1f)
                    )

                    // Vendég: támadók a középvonal felől, kapus legalul.
                    PitchHalf(
                        rows = awayRows.asReversed(),
                        baseColor = AwayColor,
                        badges = awayBadges,
                        emptyText = "$awayTeam: nincs kezdőcsapat",
                        onPlayerClick = { onPlayerClick(it, awayTeam) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Legend(colors)

            CoachLine(homeTeam, home, HomeColor, colors, onCoachClick)
            CoachLine(awayTeam, away, AwayColor, colors, onCoachClick)

            BenchSection(homeTeam, home, HomeColor, colors) { onPlayerClick(it, homeTeam) }
            BenchSection(awayTeam, away, AwayColor, colors) { onPlayerClick(it, awayTeam) }
        }
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
        "⚽ gól   🟨 sárga lap   🟥 piros lap   Ⓒ csapatkapitány",
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
        Text(
            "$teamName – edző: ",
            color = colors.secondaryText,
            fontSize = 11.sp
        )
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

        // Fűcsíkok
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

        // Külső vonal + középvonal + középkör + középpont
        drawRect(
            color = lineColor,
            topLeft = Offset(margin, margin),
            size = Size(w - margin * 2, h - margin * 2),
            style = stroke
        )
        drawLine(lineColor, Offset(margin, h / 2f), Offset(w - margin, h / 2f), stroke.width)
        drawCircle(lineColor, radius = w * 0.14f, center = Offset(w / 2f, h / 2f), style = stroke)
        drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(w / 2f, h / 2f))

        // Tizenhatosok és kapuelőterek (fent és lent)
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
    onClick: () -> Unit
) {
    val isKeeper = roleOf(player.position) == 0
    val fill = if (isKeeper) KeeperColor else baseColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(62.dp)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.size(38.dp)) {

            if (!player.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = player.photoUrl,
                    contentDescription = "${player.name} fotója",
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .border(2.dp, fill, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.Center)
                        .border(1.5.dp, Color.White, CircleShape),
                    shape = CircleShape,
                    color = fill
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = player.number?.takeIf { it.isNotBlank() } ?: "·",
                            color = if (isKeeper) Color(0xFF422006) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

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

            if (badges.goals > 0) {
                Text(
                    text = if (badges.goals > 1) "⚽${badges.goals}" else "⚽",
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.TopStart)
                )
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
    }
}

// ================================================================
// SORBA RENDEZÉS (kapus -> védők -> középpályások -> támadók)
// ================================================================

/** 0 = kapus, 1 = védő, 2 = középpályás, 3 = támadó, 4 = ismeretlen. */
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

/**
 * A visszaadott lista első eleme a kapus, utána a vonalak a védőktől a
 * támadókig. Először a formációt használjuk (ha a számok kiadják a 10
 * mezőnyjátékost), egyébként a posztok szerint csoportosítunk.
 */
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
// GÓL / LAP JELZÉSEK A JÁTÉKOSOKON
// ================================================================

private data class PlayerBadges(
    val goals: Int = 0,
    val yellow: Int = 0,
    val red: Int = 0
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

    lineup.startingPlayers.forEach { player ->
        var goals = 0
        var yellow = 0
        var red = 0
        teamEvents.forEach { event ->
            if (!samePlayer(event.player, player, everyone)) return@forEach
            when (eventKind(event)) {
                1 -> goals++
                2 -> yellow++
                3 -> red++
            }
        }
        if (goals + yellow + red > 0) {
            result[player] = PlayerBadges(goals, yellow, red)
        }
    }
    return result
}

/** 1 = gól, 2 = sárga lap, 3 = piros lap, 0 = egyéb / nem számít. */
private fun eventKind(event: MatchEvent): Int {
    val type = event.type.lowercase(Locale.ROOT)
    val detail = event.detail.orEmpty().lowercase(Locale.ROOT)
    return when {
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

/**
 * Az események játékosnevei gyakran rövidítettek ("J. Martinez"), ezért
 * a teljes név egyezése mellett a vezetéknév + keresztnév-kezdőbetű is
 * elég, ha a vezetéknév a keretben egyedi.
 */
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
