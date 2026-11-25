package com.example.tippscores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.data.model.MatchEvent
import com.example.tippscores.data.util.JsonX
import com.example.tippscores.ui.theme.AppColorScheme
import com.example.tippscores.ui.theme.LocalAppColors

private val HomeAccent = Color(0xFF2563EB)
private val AwayAccent = Color(0xFFDC2626)

@Composable
fun EventTimeline(
    events: List<MatchEvent>,
    homeTeam: String,
    awayTeam: String
) {
    val colors = LocalAppColors.current

    val sorted = remember(events) {
        events.sortedBy { minuteKey(it.minute) }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Fejléc
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    homeTeam,
                    modifier = Modifier.weight(1f),
                    color = HomeAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Események",
                    color = colors.secondaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Text(
                    awayTeam,
                    modifier = Modifier.weight(1f),
                    color = AwayAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = colors.divider.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))

            var lastHalf = 1

            sorted.forEach { event ->
                val currentHalf = if (minuteKey(event.minute) > 4500) 2 else 1
                if (currentHalf > lastHalf) {
                    HalfTimeMarker(colors)
                    lastHalf = currentHalf
                }

                TimelineRow(
                    event = event,
                    isHome = isHomeEvent(event, homeTeam, awayTeam),
                    colors = colors
                )
            }
        }
    }
}

@Composable
private fun HalfTimeMarker(colors: AppColorScheme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.divider.copy(alpha = 0.7f)
        ) {
            Text(
                "Félidő",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = colors.secondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TimelineRow(
    event: MatchEvent,
    isHome: Boolean,
    colors: AppColorScheme
) {
    val icon = iconFor(event)
    val accent = if (isHome) HomeAccent else AwayAccent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bal oldal (hazai)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (isHome) {
                EventCard(event, icon, accent, alignEnd = true, colors)
            }
        }

        // Középső perc
        Surface(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .width(56.dp),
            shape = RoundedCornerShape(12.dp),
            color = colors.divider.copy(alpha = 0.35f)
        ) {
            Text(
                text = if (event.minute.contains("'")) event.minute else "${event.minute}'",
                modifier = Modifier.padding(vertical = 6.dp),
                color = colors.primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        // Jobb oldal (vendég)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (!isHome) {
                EventCard(event, icon, accent, alignEnd = false, colors)
            }
        }
    }
}

@Composable
private fun EventCard(
    event: MatchEvent,
    icon: String,
    accent: Color,
    alignEnd: Boolean,
    colors: AppColorScheme
) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            text = if (alignEnd) "${event.player} $icon" else "$icon ${event.player}",
            color = colors.primaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val secondary = listOfNotNull(
            event.assist?.takeIf { it.isNotBlank() }?.let { "gólpassz: $it" },
            event.detail?.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        if (secondary.isNotBlank()) {
            Text(
                text = secondary,
                color = colors.tertiaryText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** "45+2" -> 4502, "67" -> 6700 */
private fun minuteKey(raw: String): Int {
    val match = Regex("""(\d+)(?:\s*\+\s*(\d+))?""").find(raw) ?: return 0
    val base = match.groupValues[1].toIntOrNull() ?: 0
    val extra = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
    return base * 100 + extra
}

private fun isHomeEvent(event: MatchEvent, homeTeam: String, awayTeam: String): Boolean {
    val team = JsonX.norm(event.team)
    if (team == "home") return true
    if (team == "away") return false
    val home = JsonX.norm(homeTeam)
    val away = JsonX.norm(awayTeam)
    return when {
        team.isEmpty() -> true
        home.isNotEmpty() && (team.contains(home) || home.contains(team)) -> true
        away.isNotEmpty() && (team.contains(away) || away.contains(team)) -> false
        else -> true
    }
}

private fun iconFor(event: MatchEvent): String {
    val text = "${event.type} ${event.detail.orEmpty()}".lowercase()
    val isPenalty = text.contains("pen") || text.contains("tizenegyes")
    val isMissed = text.contains("miss") || text.contains("kihagy") ||
            text.contains("saved") || text.contains("véd")

    return when {
        text.contains("own") || text.contains("öngól") -> "⚽ (öngól)"
        (text.contains("yellow") && text.contains("red")) || text.contains("második sárga") -> "🟨🟥"
        text.contains("sárga") || text.contains("yellow") -> "🟨"
        text.contains("piros") || text.contains("red") -> "🟥"
        isPenalty && isMissed -> "❌ (11-es)"
        text.contains("gól") || text.contains("goal") -> if (isPenalty) "⚽ (11-es)" else "⚽"
        text.contains("csere") || text.contains("sub") -> "🔄"
        text.contains("var") -> "📺"
        else -> "•"
    }
}
