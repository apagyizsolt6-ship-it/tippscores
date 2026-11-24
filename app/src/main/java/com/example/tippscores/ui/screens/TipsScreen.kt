package com.example.tippscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.tippscores.ui.theme.AppColorScheme
import com.example.tippscores.ui.theme.LocalAppColors

private val Good = Color(0xFF16A34A)
private val Bad = Color(0xFFDC2626)
private val Pending = Color(0xFF6B7280)

/** A saját tippek áttekintése: pontszám, találati arány, előzmények. */
@Composable
fun TipsScreen(
    tipStore: TipStore,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current

    var tips by remember { mutableStateOf(tipStore.all()) }
    var stats by remember { mutableStateOf(tipStore.stats()) }

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
                    "←",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(end = 14.dp)
                        .clickable(onClick = onBack)
                )
                Text(
                    "Tippjáték",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                StatsCard(stats, colors)
            }

            if (tips.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.cardBackground
                    ) {
                        Text(
                            "Még nincs tipped. Nyiss meg egy mérkőzést, és a „Tipp” fülön add le az első tippedet!",
                            modifier = Modifier.padding(18.dp),
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(items = tips, key = { it.matchId }) { tip ->
                TipRow(
                    tip = tip,
                    colors = colors,
                    onDelete = {
                        tipStore.remove(tip.matchId)
                        tips = tipStore.all()
                        stats = tipStore.stats()
                    }
                )
            }
        }
    }
}

@Composable
private fun StatsCard(stats: com.example.tippscores.data.local.TipStats, colors: AppColorScheme) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${stats.points} pont",
                color = colors.primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            )
            Text(
                "${stats.total} tipp • ${stats.settled} kiértékelve",
                color = colors.secondaryText,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Metric("Találati arány", "${stats.hitRatePercent}%", colors)
                Metric("Telitalálat", stats.exactHits.toString(), colors)
                Metric("Nyerő széria", stats.currentStreak.toString(), colors)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, colors: AppColorScheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text(label, color = colors.tertiaryText, fontSize = 10.sp)
    }
}

@Composable
private fun TipRow(tip: Tip, colors: AppColorScheme, onDelete: () -> Unit) {
    val points = tip.points
    val accent = when {
        points == null -> Pending
        points > 0 -> Good
        else -> Bad
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.cardBackground
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${tip.homeTeam} – ${tip.awayTeam}",
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (tip.league.isNotBlank()) {
                    Text(tip.league, color = colors.tertiaryText, fontSize = 10.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tipp: ${tip.predictedHome} – ${tip.predictedAway}" +
                        if (tip.isSettled) "   •   Végeredmény: ${tip.finalHome} – ${tip.finalAway}" else "",
                    color = colors.secondaryText,
                    fontSize = 11.sp
                )
                if (!tip.isSettled) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Törlés",
                        color = Bad,
                        fontSize = 10.sp,
                        modifier = Modifier.clickable(onClick = onDelete)
                    )
                }
            }

            Surface(shape = RoundedCornerShape(10.dp), color = accent) {
                Text(
                    if (points == null) "függő" else "$points p",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
