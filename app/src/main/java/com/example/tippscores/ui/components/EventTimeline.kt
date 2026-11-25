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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.data.model.MatchEvent
import com.example.tippscores.data.util.JsonX
import com.example.tippscores.ui.theme.AppColorScheme
import com.example.tippscores.ui.theme.LocalAppColors

private val HomeAccent = Color(0xFF2563EB)
private val AwayAccent = Color(0xFFDC2626)

/**
 * Időrendi idővonal: középen a perc, bal oldalon a hazai, jobb oldalon a
 * vendég csapat eseményei (gól, lap, csere), félidő-elválasztóval.
 */
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
        Column(modifier = Modifier.padding(14.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    homeTeam,
                    modifier = Modifier.weight(1f),
                    color = HomeAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    "Idővonal",
                    color = colors.tertiaryText,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(64.dp)
                )
                Text(
                    awayTeam,
                    modifier = Modifier.weight(1f),
                    color = AwayAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.height(8.dp))

            val lineColor = colors.divider

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = lineColor,
                            start = Offset(size.width / 2f, 0f),
                            end = Offset(size.width / 2f, size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
            ) {
                var halfTimeShown = false

                sorted.forEach { event ->
                    val base = minuteKey(event.minute) / 100

                    if (!halfTimeShown && base > 45) {
                        halfTimeShown = true
                        HalfTimeMarker(colors)
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
}

@Composable
private fun HalfTimeMarker(colors: AppColorScheme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(shape = RoundedCornerShape(8.dp), color = colors.divider) {
            Text(
                "Félidő",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                color = colors.secondaryText,
                fontSize = 10.sp,
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (isHome) EventText(event, icon, colors, alignEnd = true)
        }

        Surface(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .width(52.dp),
            shape = RoundedCornerShape(8.dp),
            color = colors.cardBackground
        ) {
            Text(
                text = if (event.minute.contains("'")) event.minute else "${event.minute}'",
                modifier = Modifier.padding(vertical = 3.dp),
                color = colors.primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (!isHome) EventText(event, icon, colors, alignEnd = false)
        }
    }
}

@Composable
private fun EventText(
    event: MatchEvent,
    icon: String,
    colors: AppColorScheme,
    alignEnd: Boolean
) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (alignEnd) "${event.player} $icon" else "$icon ${event.player}",
            color = colors.primaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
        )

        val secondary = listOfNotNull(
            event.assist?.takeIf { it.isNotBlank() }?.let { "gólpassz: $it" },
            rememberHu(event.detail)?.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        if (secondary.isNotBlank()) {
            Text(
                text = secondary,
                color = colors.tertiaryText,
                fontSize = 10.sp,
                textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
            )
        }
    }
}

/** "45+2" -> 4502, "67" -> 6700: rendezéshez és félidő-észleléshez. */
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
