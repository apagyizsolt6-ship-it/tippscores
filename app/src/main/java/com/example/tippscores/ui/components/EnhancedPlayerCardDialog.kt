package com.example.tippscores.ui.components

import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.tippscores.data.model.LineupPlayer
import com.example.tippscores.data.model.PlayerProfile
import com.example.tippscores.ui.theme.AppColorScheme
import com.example.tippscores.ui.theme.LocalAppColors
import java.util.Locale

private val PlayerBlue = Color(0xFF2563EB)

@Composable
fun EnhancedPlayerCardDialog(
    player: LineupPlayer,
    teamName: String,
    profile: PlayerProfile?,
    profileLoading: Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    var tab by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.cardBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!player.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = player.photoUrl,
                            contentDescription = "${player.name} fotója",
                            modifier = Modifier.size(72.dp).clip(CircleShape).border(2.dp, PlayerBlue, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(72.dp).border(2.dp, PlayerBlue, CircleShape),
                            shape = CircleShape,
                            color = colors.statusChipBackground
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(player.number?.takeIf { it.isNotBlank() } ?: "👤", fontSize = 24.sp, color = colors.primaryText, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.size(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            player.name + if (player.isCaptain) " (C)" else "",
                            color = colors.primaryText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Text(
                            listOfNotNull(
                                teamName.takeIf { it.isNotBlank() },
                                player.number?.takeIf { it.isNotBlank() }?.let { "#$it" },
                                (profile?.position ?: player.position)?.takeIf { it.isNotBlank() }
                            ).joinToString(" • "),
                            color = colors.secondaryText,
                            fontSize = 11.sp
                        )
                        val r = profile?.rating?.toDoubleOrNull()
                        if (r != null) {
                            Spacer(Modifier.height(4.dp))
                            val badgeColor = when {
                                r >= 8.0 -> PlayerBlue
                                r >= 7.0 -> Color(0xFF16A34A)
                                r >= 6.0 -> Color(0xFFD97706)
                                else -> Color(0xFFDC2626)
                            }
                            Surface(color = badgeColor, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "Szezon átlag: %.2f".format(Locale.US, r),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                when {
                    profileLoading -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PlayerBlue, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        }
                    }
                    profile != null -> {
                        TabRow(selectedTabIndex = tab, containerColor = colors.cardBackground, contentColor = PlayerBlue) {
                            listOf("Adatlap", "Statisztika", "Karrier").forEachIndexed { idx, label ->
                                Tab(selected = tab == idx, onClick = { tab = idx }, text = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) })
                            }
                        }
                        Spacer(Modifier.height(10.dp))

                        when (tab) {
                            0 -> {
                                PlayerRowItem("Kor", profile.age, colors)
                                PlayerRowItem("Nemzetiség", rememberHu(profile.nationality), colors)
                                PlayerRowItem("Születési dátum", profile.birthdate, colors)
                                PlayerRowItem("Születési hely", profile.birthplace, colors)
                                PlayerRowItem("Magasság", profile.height?.let { "$it cm" }, colors)
                                PlayerRowItem("Testsúly", profile.weight?.let { "$it kg" }, colors)
                                PlayerRowItem("Erősebb láb", translateFoot(profile.preferredFoot), colors)
                                PlayerRowItem("Piaci érték", formatValue(profile.marketValueEur), colors)
                            }
                            1 -> {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                    StatBox("Meccs", profile.appearances ?: "-", colors)
                                    StatBox("Gól", profile.goals ?: "0", colors)
                                    StatBox("Assziszt", profile.assists ?: "0", colors)
                                    StatBox("Perc", profile.minutesPlayed ?: "-", colors)
                                }
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(color = colors.divider)
                                Spacer(Modifier.height(8.dp))
                                PlayerRowItem("Kezdőként", profile.startingLineups, colors)
                                PlayerRowItem("Lövések (kapura)", pairFormat(profile.shotsTotal, profile.shotsOnTarget), colors)
                                PlayerRowItem("Kulcspasszok", profile.keyPasses, colors)
                                PlayerRowItem("Cselek (sikeres / összes)", ratioFormat(profile.dribbleSuccess, profile.dribbleAttempts), colors)
                                PlayerRowItem("Párharcok (nyert / összes)", ratioFormat(profile.duelsWon, profile.duelsTotal), colors)
                                PlayerRowItem("Szerelések / Labdaszerzés", pairFormat(profile.tackles, profile.interceptions), colors)
                                PlayerRowItem("Sárga / Piros lap", pairFormat(profile.yellowCards, profile.redCards), colors)
                                PlayerRowItem("Védések", profile.saves, colors)
                            }
                            2 -> {
                                PlayerRowItem("Karrier meccsek", profile.careerAppearances, colors)
                                PlayerRowItem("Karrier percek", profile.careerMinutes, colors)
                                PlayerRowItem("Karrier gólpasszok", profile.careerAssists, colors)
                                PlayerRowItem("Karrier átlagértékelés", profile.careerRating?.toDoubleOrNull()?.let { "%.2f".format(it) }, colors)

                                if (profile.trophies.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("Trófeák", color = PlayerBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    profile.trophies.forEach { t ->
                                        PlayerRowItem("🏆 ${t.league}", listOfNotNull(rememberHu(t.status), t.count?.let { "×$it" }).joinToString(" "), colors)
                                    }
                                }
                                if (profile.transfers.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("Átigazolások", color = PlayerBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    profile.transfers.take(6).forEach { tr ->
                                        PlayerRowItem(tr.date, listOfNotNull(listOfNotNull(tr.from, tr.to).joinToString(" → "), tr.price).joinToString(" • "), colors)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Text("Részletes játékosadatok jelenleg nem érhetők el.", color = colors.tertiaryText, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss),
                    shape = RoundedCornerShape(10.dp),
                    color = colors.statusChipBackground
                ) {
                    Text(
                        "Bezárás",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
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
private fun StatBox(label: String, value: String, colors: AppColorScheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Text(label, color = colors.tertiaryText, fontSize = 10.sp)
    }
}

@Composable
private fun PlayerRowItem(label: String, value: String?, colors: AppColorScheme) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = colors.tertiaryText, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = colors.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

private fun pairFormat(a: String?, b: String?): String? = when {
    a.isNullOrBlank() -> null
    b.isNullOrBlank() -> a
    else -> "$a ($b)"
}

private fun ratioFormat(a: String?, b: String?): String? = when {
    a.isNullOrBlank() && b.isNullOrBlank() -> null
    b.isNullOrBlank() -> a
    a.isNullOrBlank() -> "– / $b"
    else -> "$a / $b"
}

private fun translateFoot(foot: String?): String? = when (foot?.trim()?.lowercase(Locale.ROOT)) {
    "left" -> "Bal"
    "right" -> "Jobb"
    "both" -> "Mindkettő"
    null, "" -> null
    else -> foot
}

private fun formatValue(raw: String?): String? {
    val v = raw?.toLongOrNull() ?: return null
    return when {
        v >= 1_000_000 -> "%.1f M €".format(v / 1_000_000.0)
        v >= 1_000 -> "%.0f E €".format(v / 1_000.0)
        else -> "$v €"
    }
}
