package com.example.tippscores.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.tippscores.data.model.MatchStatistic
import com.example.tippscores.ui.theme.AppColorScheme
import com.example.tippscores.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private val HomeColor = Color(0xFF2563EB)
private val AwayColor = Color(0xFFDC2626)
private const val CHANGE_VISIBLE_MS = 20_000L

/** Egy mutató legutóbbi változása az automatikus frissítések között. */
private class StatChange(
    val homeDelta: Float,
    val awayDelta: Float,
    val at: Long
)

/**
 * Élő statisztika: kiemelt mutatók, labdabirtoklás, témák szerinti csoportok,
 * animált arányosító sávok, és a változások kiemelése (▲ +1) a frissítések után.
 */
@Composable
fun LiveStatisticsSection(
    stats: List<MatchStatistic>,
    homeTeam: String,
    awayTeam: String
) {
    val colors = LocalAppColors.current

    var previous by remember { mutableStateOf<Map<String, Pair<Float?, Float?>>?>(null) }
    var changes by remember { mutableStateOf<Map<String, StatChange>>(emptyMap()) }

    LaunchedEffect(stats) {
        val current = stats.associate { it.label to (numberOf(it.home) to numberOf(it.away)) }
        val before = previous
        val nowMs = System.currentTimeMillis()

        if (before != null) {
            val updated = changes.filterValues { nowMs - it.at < CHANGE_VISIBLE_MS }.toMutableMap()
            current.forEach { (label, pair) ->
                val old = before[label] ?: return@forEach
                val homeDelta = delta(old.first, pair.first)
                val awayDelta = delta(old.second, pair.second)
                if (homeDelta != 0f || awayDelta != 0f) {
                    updated[label] = StatChange(homeDelta, awayDelta, nowMs)
                }
            }
            changes = updated
        }
        previous = current
    }

    // A kiemelések lejárata miatt másodpercenként újrarajzolunk, amíg van aktív változás.
    val now by produceState(initialValue = System.currentTimeMillis(), key1 = changes) {
        while (changes.any { System.currentTimeMillis() - it.value.at < CHANGE_VISIBLE_MS }) {
            delay(1000)
            value = System.currentTimeMillis()
        }
        value = System.currentTimeMillis()
    }

    fun activeChange(label: String): StatChange? =
        changes[label]?.takeIf { now - it.at < CHANGE_VISIBLE_MS }

    val possession = stats.firstOrNull { categoryOf(it.label) == Category.POSSESSION }
    val others = stats.filter { it !== possession }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        KeyStatTiles(stats, homeTeam, awayTeam, colors)

        possession?.let {
            LivePossessionCard(it, homeTeam, awayTeam, colors)
        }

        listOf(
            Category.ATTACK to "Támadás",
            Category.PASSING to "Passzok",
            Category.DISCIPLINE to "Szabálytalanságok és lapok",
            Category.OTHER to "Egyéb mutatók"
        ).forEach { (category, title) ->
            val group = others.filter { categoryOf(it.label) == category }
            if (group.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardBackground
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            title,
                            color = colors.primaryText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        group.forEachIndexed { index, stat ->
                            LiveStatRow(stat, activeChange(stat.label), colors)
                            if (index != group.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = colors.divider.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================================================================
// KIEMELT MUTATÓK
// ================================================================

@Composable
private fun KeyStatTiles(
    stats: List<MatchStatistic>,
    homeTeam: String,
    awayTeam: String,
    colors: AppColorScheme
) {
    fun find(vararg parts: String): MatchStatistic? =
        stats.firstOrNull { stat ->
            val lower = stat.label.lowercase()
            parts.any { lower.contains(it) }
        }

    val tiles = listOfNotNull(
        stats.firstOrNull {
            val l = it.label.lowercase()
            (l.contains("lövés") || l.contains("shots")) && !l.contains("kaput") &&
                !l.contains("target") && !l.contains("pontoss")
        },
        find("kaput", "on target"),
        find("szöglet", "corner"),
        find("sárga", "yellow")
    )

    if (tiles.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    homeTeam,
                    color = HomeColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    awayTeam,
                    color = AwayColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tiles.forEach { stat ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.divider.copy(alpha = 0.25f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${stat.home} – ${stat.away}",
                                color = colors.primaryText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                stat.label,
                                color = colors.tertiaryText,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================================================================
// LABDABIRTOKLÁS
// ================================================================

@Composable
private fun LivePossessionCard(
    stat: MatchStatistic,
    homeTeam: String,
    awayTeam: String,
    colors: AppColorScheme
) {
    val home = numberOf(stat.home) ?: 50f
    val away = numberOf(stat.away) ?: 50f
    val total = (home + away).takeIf { it > 0f } ?: 100f

    val animatedHome by animateFloatAsState(
        targetValue = (home / total).coerceIn(0.08f, 0.92f),
        animationSpec = tween(durationMillis = 700),
        label = "possessionHome"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Labdabirtoklás",
                color = colors.secondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(homeTeam, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(awayTeam, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Box(
                    modifier = Modifier
                        .weight(animatedHome)
                        .fillMaxHeight()
                        .background(HomeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${home.roundToInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f - animatedHome)
                        .fillMaxHeight()
                        .background(AwayColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${away.roundToInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ================================================================
// EGY MUTATÓ SORA
// ================================================================

@Composable
private fun LiveStatRow(
    stat: MatchStatistic,
    change: StatChange?,
    colors: AppColorScheme
) {
    val homeNum = numberOf(stat.home)
    val awayNum = numberOf(stat.away)
    val total = (homeNum ?: 0f) + (awayNum ?: 0f)
    val targetRatio = if (total > 0f) (homeNum ?: 0f) / total else 0.5f

    val ratio by animateFloatAsState(
        targetValue = targetRatio.coerceIn(0.03f, 0.97f),
        animationSpec = tween(durationMillis = 700),
        label = "statRatio"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.width(76.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stat.home,
                    color = if ((homeNum ?: 0f) > (awayNum ?: 0f)) HomeColor else colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (change != null && change.homeDelta != 0f) {
                    ChangeBadge(change.homeDelta, HomeColor)
                }
            }

            Text(
                stat.label,
                color = colors.secondaryText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            Row(
                modifier = Modifier.width(76.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (change != null && change.awayDelta != 0f) {
                    ChangeBadge(change.awayDelta, AwayColor)
                }
                Text(
                    stat.away,
                    color = if ((awayNum ?: 0f) > (homeNum ?: 0f)) AwayColor else colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        if (homeNum != null && awayNum != null && total > 0f) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .weight(ratio)
                        .fillMaxHeight()
                        .background(HomeColor)
                )
                Box(
                    modifier = Modifier
                        .weight(1f - ratio)
                        .fillMaxHeight()
                        .background(AwayColor.copy(alpha = 0.55f))
                )
            }
        }
    }
}

@Composable
private fun ChangeBadge(deltaValue: Float, color: Color) {
    val text = (if (deltaValue > 0f) "▲" else "▼") + formatDelta(abs(deltaValue))

    Text(
        text,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 3.dp),
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

// ================================================================
// SEGÉDEK
// ================================================================

private enum class Category { POSSESSION, ATTACK, PASSING, DISCIPLINE, OTHER }

private fun categoryOf(label: String): Category {
    val l = label.lowercase()
    return when {
        l.contains("labdabirtok") || l.contains("possession") -> Category.POSSESSION
        l.contains("szabálytalan") || l.contains("lap") || l.contains("foul") ||
            l.contains("card") || l.contains("yellow") || l.contains("red") -> Category.DISCIPLINE
        l.contains("pass") -> Category.PASSING
        l.contains("lövés") || l.contains("lövések") || l.contains("kapu") || l.contains("shot") ||
            l.contains("szöglet") || l.contains("corner") || l.startsWith("les") ||
            l.contains("offside") || l.contains("xg") || l.contains("várható gól") ||
            l.contains("attack") || l.contains("támad") -> Category.ATTACK
        else -> Category.OTHER
    }
}

/** "55%" -> 55, "3" -> 3, "–" -> null. */
private fun numberOf(raw: String): Float? =
    Regex("""-?\d+(?:[.,]\d+)?""").find(raw)?.value?.replace(',', '.')?.toFloatOrNull()

private fun delta(old: Float?, new: Float?): Float =
    if (old != null && new != null) new - old else 0f

private fun formatDelta(value: Float): String =
    if (value >= 1f && value == value.roundToInt().toFloat()) {
        value.roundToInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", value)
    }
