package com.example.tippscores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.data.local.Tip
import com.example.tippscores.data.local.TipStore
import com.example.tippscores.data.model.Match
import com.example.tippscores.ui.theme.AppColorScheme
import com.example.tippscores.ui.theme.LocalAppColors

private val Accent = Color(0xFF2563EB)
private val Good = Color(0xFF16A34A)
private val Bad = Color(0xFFDC2626)

/** A meccs "Tipp" füle: eredménytipp leadása, módosítása, kiértékelése. */
@Composable
fun TipSection(
    match: Match,
    tip: Tip?,
    onSave: (Int, Int) -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalAppColors.current
    val editable = TipStore.canTip(match)

    var home by remember(match.id, tip?.predictedHome) {
        mutableIntStateOf(tip?.predictedHome ?: 1)
    }
    var away by remember(match.id, tip?.predictedAway) {
        mutableIntStateOf(tip?.predictedAway ?: 0)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                "Saját tippem",
                color = colors.primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Pontos eredmény: 5 pont • jó győztes és gólkülönbség: 3 pont • csak a jó kimenetel: 2 pont",
                color = colors.tertiaryText,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(14.dp))

            if (!editable && tip == null) {
                Text(
                    "Erre a mérkőzésre már nem lehet tippelni, mert elkezdődött vagy véget ért.",
                    color = colors.secondaryText,
                    fontSize = 12.sp
                )
            } else if (editable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Stepper(
                        teamName = match.homeTeam,
                        value = home,
                        onChange = { home = it },
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "–",
                        color = colors.secondaryText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    Stepper(
                        teamName = match.awayTeam,
                        value = away,
                        onChange = { away = it },
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickPick("1  Hazai", Modifier.weight(1f), colors) { home = 1; away = 0 }
                    QuickPick("X  Döntetlen", Modifier.weight(1f), colors) { home = 1; away = 1 }
                    QuickPick("2  Vendég", Modifier.weight(1f), colors) { home = 0; away = 1 }
                }

                Spacer(Modifier.height(14.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSave(home, away) },
                    shape = RoundedCornerShape(12.dp),
                    color = Accent
                ) {
                    Text(
                        if (tip == null) "Tipp mentése" else "Tipp módosítása",
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                if (tip != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Mentett tipp: ${tip.predictedHome} – ${tip.predictedAway}  (törlés)",
                        color = Bad,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable(onClick = onDelete)
                    )
                }
            } else if (tip != null) {
                Text(
                    "${match.homeTeam} – ${match.awayTeam}",
                    color = colors.secondaryText,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tipped: ${tip.predictedHome} – ${tip.predictedAway}",
                    color = colors.primaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )

                Spacer(Modifier.height(8.dp))

                val finalHome = tip.finalHome ?: match.homeScore
                val finalAway = tip.finalAway ?: match.awayScore

                when {
                    tip.isSettled -> {
                        val points = tip.points ?: 0
                        Text(
                            "Végeredmény: $finalHome – $finalAway   •   $points pont",
                            color = if (points > 0) Good else Bad,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    match.isLive -> Text(
                        "A mérkőzés folyamatban van (${match.homeScore ?: 0} – ${match.awayScore ?: 0}). A kiértékelés a végén automatikusan megtörténik.",
                        color = colors.secondaryText,
                        fontSize = 12.sp
                    )
                    else -> Text(
                        "A kiértékelés a végeredmény után történik meg.",
                        color = colors.secondaryText,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun Stepper(
    teamName: String,
    value: Int,
    onChange: (Int) -> Unit,
    colors: AppColorScheme,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            teamName,
            color = colors.secondaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton("−", colors) { if (value > 0) onChange(value - 1) }
            Text(
                value.toString(),
                modifier = Modifier.width(36.dp),
                color = colors.primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center
            )
            RoundButton("+", colors) { if (value < 15) onChange(value + 1) }
        }
    }
}

@Composable
private fun RoundButton(label: String, colors: AppColorScheme, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(colors.divider, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun QuickPick(
    label: String,
    modifier: Modifier,
    colors: AppColorScheme,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = colors.divider
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 8.dp),
            color = colors.primaryText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}
