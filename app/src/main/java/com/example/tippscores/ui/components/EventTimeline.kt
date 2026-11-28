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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay

private val HomeAccent = Color(0xFF2563EB)
private val AwayAccent = Color(0xFFDC2626)
private const val FRESH_VISIBLE_MS = 20_000L

private enum class Kind { GOAL, OWN_GOAL, MISSED_PENALTY, YELLOW, SECOND_YELLOW, RED, SUBSTITUTION, VAR, OTHER }

/** Egy esemény a feldolgozott, időrendbe tett listában. */
private class TimelineItem(
    val event: MatchEvent,
    val kind: Kind,
    val isHome: Boolean,
    val scoreAfter: Pair<Int, Int>?,
    val key: String
)

/**
 * Esemény-idővonal: szűrők (gólok / lapok / cserék), összesítő, futó eredmény a
 * gólok mellett, félidő- és hosszabbítás-elválasztó, és az új események kiemelése
 * az automatikus frissítések után.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTimeline(
    events: List<MatchEvent>,
    homeTeam: String,
    awayTeam: String
) {
    val colors = LocalAppColors.current

    var filter by remember { mutableIntStateOf(0) }

    val items = remember(events, homeTeam, awayTeam) { buildItems(events, homeTeam, awayTeam) }

    // Új események észlelése a frissítések között.
    var knownKeys by remember { mutableStateOf<Set<String>?>(null) }
    var freshAt by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

    LaunchedEffect(items) {
        val keys = items.map { it.key }.toSet()
        val known = knownKeys
        if (known != null) {
            val added = keys - known
            if (added.isNotEmpty()) {
                val nowMs = System.currentTimeMillis()
                freshAt = freshAt + added.associateWith { nowMs }
            }
        }
        knownKeys = keys
    }

    val now by produceState(initialValue = System.currentTimeMillis(), key1 = freshAt) {
        while (freshAt.any { System.currentTimeMillis() - it.value < FRESH_VISIBLE_MS }) {
            delay(1000)
            value = System.currentTimeMillis()
        }
        value = System.currentTimeMillis()
    }

    val visible = items.filter { matchesFilter(it.kind, filter) }

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

            Spacer(Modifier.height(10.dp))

            SummaryRow(items, colors)

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Mind", "Gólok", "Lapok", "Cserék").forEachIndexed { index, label ->
                    FilterChip(
                        selected = filter == index,
                        onClick = { filter = index },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            items.lastOrNull()?.let { last ->
                Spacer(Modifier.height(6.dp))
                Text(
                    "Utolsó esemény: ${minuteLabel(last.event.minute)} ${iconFor(last)} ${last.event.player}",
                    color = colors.tertiaryText,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = colors.divider.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))

            if (visible.isEmpty()) {
                Text(
                    "Ebben a szűrőben nincs esemény.",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            var lastPeriod = 1

            visible.forEach { item ->
                val period = periodOf(item.event.minute)
                if (period > lastPeriod) {
                    PeriodMarker(if (period == 2) "Félidő" else "Hosszabbítás", colors)
                    lastPeriod = period
                }

                val fresh = freshAt[item.key]?.let { now - it < FRESH_VISIBLE_MS } == true

                TimelineRow(
                    item = item,
                    fresh = fresh,
                    colors = colors
                )
            }
        }
    }
}

// ================================================================
// ÖSSZESÍTŐ
// ================================================================

@Composable
private fun SummaryRow(items: List<TimelineItem>, colors: AppColorScheme) {
    fun count(isHome: Boolean, vararg kinds: Kind): Int =
        items.count { it.isHome == isHome && it.kind in kinds }

    fun goalsFor(isHome: Boolean): Int =
        items.count { (it.kind == Kind.GOAL && it.isHome == isHome) ||
            (it.kind == Kind.OWN_GOAL && it.isHome != isHome) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "⚽ ${goalsFor(true)}   🟨 ${count(true, Kind.YELLOW, Kind.SECOND_YELLOW)}   " +
                "🟥 ${count(true, Kind.RED, Kind.SECOND_YELLOW)}   🔄 ${count(true, Kind.SUBSTITUTION)}",
            modifier = Modifier.weight(1f),
            color = colors.secondaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "⚽ ${goalsFor(false)}   🟨 ${count(false, Kind.YELLOW, Kind.SECOND_YELLOW)}   " +
                "🟥 ${count(false, Kind.RED, Kind.SECOND_YELLOW)}   🔄 ${count(false, Kind.SUBSTITUTION)}",
            modifier = Modifier.weight(1f),
            color = colors.secondaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun PeriodMarker(text: String, colors: AppColorScheme) {
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
                text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = colors.secondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ================================================================
// SOROK
// ================================================================

@Composable
private fun TimelineRow(
    item: TimelineItem,
    fresh: Boolean,
    colors: AppColorScheme
) {
    val accent = if (item.isHome) HomeAccent else AwayAccent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bal oldal (hazai)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (item.isHome) {
                EventCard(item, fresh, accent, alignEnd = true, colors = colors)
            }
        }

        // Középső perc + futó eredmény
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .width(56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (fresh) accent.copy(alpha = 0.25f) else colors.divider.copy(alpha = 0.35f)
            ) {
                Text(
                    text = minuteLabel(item.event.minute),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    color = colors.primaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            item.scoreAfter?.let { (home, away) ->
                Text(
                    "$home–$away",
                    color = colors.secondaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
            }
        }

        // Jobb oldal (vendég)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (!item.isHome) {
                EventCard(item, fresh, accent, alignEnd = false, colors = colors)
            }
        }
    }
}

@Composable
private fun EventCard(
    item: TimelineItem,
    fresh: Boolean,
    accent: Color,
    alignEnd: Boolean,
    colors: AppColorScheme
) {
    val event = item.event
    val icon = iconFor(item)

    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = if (fresh) 0.28f else 0.10f))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        if (fresh) {
            Text(
                "ÚJ",
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

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
            rememberHu(event.detail)?.takeIf { it.isNotBlank() }
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

// ================================================================
// FELDOLGOZÁS
// ================================================================

private fun buildItems(
    events: List<MatchEvent>,
    homeTeam: String,
    awayTeam: String
): List<TimelineItem> {
    val sorted = events
        .withIndex()
        .sortedWith(compareBy({ minuteKey(it.value.minute) }, { it.index }))
        .map { it.value }

    var homeGoals = 0
    var awayGoals = 0

    return sorted.map { event ->
        val kind = kindOf(event)
        val isHome = isHomeEvent(event, homeTeam, awayTeam)

        var score: Pair<Int, Int>? = null
        when (kind) {
            Kind.GOAL -> {
                if (isHome) homeGoals++ else awayGoals++
                score = homeGoals to awayGoals
            }
            Kind.OWN_GOAL -> {
                if (isHome) awayGoals++ else homeGoals++
                score = homeGoals to awayGoals
            }
            else -> Unit
        }

        TimelineItem(
            event = event,
            kind = kind,
            isHome = isHome,
            scoreAfter = score,
            key = "${event.minute}|${event.player}|${event.type}|${event.team}"
        )
    }
}

private fun matchesFilter(kind: Kind, filter: Int): Boolean =
    when (filter) {
        1 -> kind == Kind.GOAL || kind == Kind.OWN_GOAL || kind == Kind.MISSED_PENALTY || kind == Kind.VAR
        2 -> kind == Kind.YELLOW || kind == Kind.SECOND_YELLOW || kind == Kind.RED
        3 -> kind == Kind.SUBSTITUTION
        else -> true
    }

/** 1 = első félidő, 2 = második félidő, 3 = hosszabbítás. */
private fun periodOf(minute: String): Int {
    val base = minuteKey(minute) / 100
    return when {
        base <= 45 -> 1
        base <= 90 -> 2
        else -> 3
    }
}

private fun minuteLabel(raw: String): String = if (raw.contains("'")) raw else "$raw'"

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

private val varRegex = Regex("""\bvar\b""")

private fun kindOf(event: MatchEvent): Kind {
    val text = "${event.type} ${event.detail.orEmpty()}".lowercase()
    val isPenalty = text.contains("pen") || text.contains("tizenegyes")
    val isMissed = text.contains("miss") || text.contains("kihagy") ||
        text.contains("saved") || text.contains("véd")

    return when {
        varRegex.containsMatchIn(text) -> Kind.VAR
        text.contains("own") || text.contains("öngól") -> Kind.OWN_GOAL
        (text.contains("yellow") && text.contains("red")) || text.contains("második sárga") ->
            Kind.SECOND_YELLOW
        text.contains("sárga") || text.contains("yellow") -> Kind.YELLOW
        text.contains("piros") || text.contains("red") -> Kind.RED
        isPenalty && isMissed -> Kind.MISSED_PENALTY
        text.contains("gól") || text.contains("goal") || isPenalty -> Kind.GOAL
        text.contains("csere") || text.contains("sub") -> Kind.SUBSTITUTION
        else -> Kind.OTHER
    }
}

private fun iconFor(item: TimelineItem): String =
    when (item.kind) {
        Kind.GOAL -> {
            val text = "${item.event.type} ${item.event.detail.orEmpty()}".lowercase()
            if (text.contains("pen") || text.contains("tizenegyes")) "⚽ (11-es)" else "⚽"
        }
        Kind.OWN_GOAL -> "⚽ (öngól)"
        Kind.MISSED_PENALTY -> "❌ (11-es)"
        Kind.YELLOW -> "🟨"
        Kind.SECOND_YELLOW -> "🟨🟥"
        Kind.RED -> "🟥"
        Kind.SUBSTITUTION -> "🔄"
        Kind.VAR -> "📺"
        Kind.OTHER -> "•"
    }
