package com.example.tippscores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.data.model.MatchPreview
import com.example.tippscores.data.model.OddsMarket
import com.example.tippscores.ui.theme.AppColorScheme
import com.example.tippscores.ui.theme.LocalAppColors
import java.util.Locale

private val Accent = Color(0xFF2563EB)

/**
 * "Előzetes" fül: modell-tipp, szorzók (piaci valószínűségekkel),
 * időjárás, háttérsztorik és a hiányzók.
 */
@Composable
fun PreviewSection(
    preview: MatchPreview?,
    loading: Boolean,
    injuries: List<String>
) {
    val colors = LocalAppColors.current

    Column {
        if (loading) {
            PreviewCard(null, colors) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.height(8.dp))
                    Text("Előzetes adatok betöltése…", color = colors.secondaryText, fontSize = 12.sp)
                }
            }
            return@Column
        }

        val hasContent = (preview != null && !preview.isEmpty) || injuries.isNotEmpty()
        if (!hasContent) {
            PreviewCard(null, colors) {
                Text(
                    "Ehhez a mérkőzéshez jelenleg nincs előzetes adat. A StatPal előzetesei (tipp, szorzók, sztorik, időjárás) főleg a nagyobb mérkőzésekre érhetők el.",
                    color = colors.secondaryText,
                    fontSize = 12.sp
                )
            }
            return@Column
        }

        val prediction = preview?.prediction
        if (prediction != null) {
            PreviewCard("Modell-tipp", colors) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        prediction.pick,
                        modifier = Modifier.weight(1f),
                        color = colors.primaryText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    prediction.odds?.let {
                        Surface(shape = RoundedCornerShape(8.dp), color = Accent) {
                            Text(
                                String.format(Locale.US, "%.2f", it),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                prediction.market?.let {
                    Text("Piac: $it", color = colors.tertiaryText, fontSize = 10.sp)
                }
                if (!prediction.reasoning.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(prediction.reasoning, color = colors.secondaryText, fontSize = 12.sp)
                }
            }
        }

        val odds = preview?.odds
        if (odds != null && odds.markets.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            PreviewCard(
                if (odds.isLive) "Élő szorzók" else "Szorzók és piaci esélyek",
                colors
            ) {
                odds.markets.forEachIndexed { index, market ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    MarketBlock(market, colors)
                }
                if (odds.bookmakerCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${odds.bookmakerCount} fogadóiroda átlaga. A százalék a szorzókból számolt, a fogadóirodai árrés nélküli valószínűség.",
                        color = colors.tertiaryText,
                        fontSize = 10.sp
                    )
                }
            }
        }

        val weather = preview?.weather
        if (weather != null) {
            Spacer(Modifier.height(12.dp))
            PreviewCard("Időjárás a meccs idején", colors) {
                Text(
                    listOfNotNull(weather.temperature, weather.condition).joinToString(" • "),
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                val details = listOfNotNull(
                    weather.feelsLike?.let { "érzet: $it" },
                    weather.wind?.let { "szél: $it" },
                    weather.rainChance?.let { "eső: $it" },
                    weather.humidity?.let { "páratartalom: $it" }
                ).joinToString("   ")
                if (details.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(details, color = colors.secondaryText, fontSize = 11.sp)
                }
            }
        }

        if (injuries.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            PreviewCard("Hiányzók (sérült, eltiltott)", colors) {
                injuries.forEach { line ->
                    Text(
                        "• $line",
                        color = colors.primaryText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        val stories = preview?.storylines.orEmpty()
        if (stories.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            PreviewCard("Háttér és sztorik", colors) {
                stories.forEachIndexed { index, story ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    story.title?.let {
                        Text(it, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(story.text, color = colors.secondaryText, fontSize = 12.sp)
                    val meta = listOfNotNull(story.date, story.tag).joinToString(" • ")
                    if (meta.isNotBlank()) {
                        Text(meta, color = colors.tertiaryText, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketBlock(market: OddsMarket, colors: AppColorScheme) {
    Text(
        market.title,
        color = colors.secondaryText,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp
    )
    Spacer(Modifier.height(6.dp))

    val top = market.outcomes.maxByOrNull { it.probability }

    market.outcomes.forEach { outcome ->
        val isTop = outcome === top

        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    outcome.label,
                    modifier = Modifier.weight(1f),
                    color = colors.primaryText,
                    fontWeight = if (isTop) FontWeight.ExtraBold else FontWeight.Normal,
                    fontSize = 12.sp
                )
                Text(
                    "${(outcome.probability * 100).toInt()}%",
                    color = colors.primaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End
                )
                Text(
                    String.format(Locale.US, "%.2f", outcome.average),
                    color = colors.secondaryText,
                    fontSize = 11.sp,
                    modifier = Modifier.width(46.dp),
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.height(3.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.divider)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(outcome.probability.toFloat().coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isTop) Accent else Color(0xFF94A3B8))
                )
            }

            if (outcome.best > outcome.average + 0.001) {
                Text(
                    "legjobb: ${String.format(Locale.US, "%.2f", outcome.best)}" +
                        (outcome.bestBookmaker?.let { " ($it)" } ?: ""),
                    color = colors.tertiaryText,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(
    title: String?,
    colors: AppColorScheme,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            if (title != null) {
                Text(title, color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}
