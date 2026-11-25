package com.example.tippscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.tippscores.data.model.LineupPlayer
import com.example.tippscores.data.model.PlayerProfile
import com.example.tippscores.data.remote.StatpalImages
import com.example.tippscores.ui.components.rememberHu
import com.example.tippscores.ui.theme.AppColorScheme
import com.example.tippscores.ui.theme.LocalAppColors
import java.util.Locale

private val PlayerBlue = Color(0xFF2563EB)
private val PlayerGreen = Color(0xFF16A34A)
private val PlayerAmber = Color(0xFFD97706)
private val PlayerRed = Color(0xFFDC2626)

@Composable
fun EnhancedPlayerCardDialog(
    player: LineupPlayer,
    teamName: String,
    profile: PlayerProfile?,
    profileLoading: Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.cardBackground,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                PlayerDialogHeader(
                    player = player,
                    teamName = teamName,
                    profile = profile,
                    colors = colors
                )

                when {
                    profileLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = PlayerBlue,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Játékosadatok betöltése…",
                                    color = colors.secondaryText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    profile != null -> {
                        val tabs = listOf("Adatlap", "Statisztika", "Karrier")
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = colors.cardBackground,
                            contentColor = PlayerBlue
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = {
                                        Text(
                                            title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            when (selectedTab) {
                                0 -> PlayerBioTab(profile = profile, colors = colors)
                                1 -> PlayerStatsTab(profile = profile, colors = colors)
                                2 -> PlayerCareerTab(profile = profile, colors = colors)
                            }
                        }
                    }

                    player.id == null -> {
                        Text(
                            "Ehhez a mérkőzéshez a StatPal nem ad játékos-azonosítót, így bővebb adat jelenleg nem tölthető be.",
                            color = colors.tertiaryText,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }

                    else -> {
                        Text(
                            "A további adatok jelenleg nem érhetők el ehhez a játékoshoz.",
                            color = colors.tertiaryText,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }
                }

                Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDismiss),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.statusChipBackground
                    ) {
                        Text(
                            "Bezárás",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            color = colors.primaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerDialogHeader(
    player: LineupPlayer,
    teamName: String,
    profile: PlayerProfile?,
    colors: AppColorScheme
) {
    val photoUrl = player.photoUrl?.takeIf { it.isNotBlank() }
        ?: StatpalImages.player(player.id)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        PlayerBlue.copy(alpha = 0.16f),
                        colors.cardBackground
                    )
                )
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "${player.name} fotója",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.dp, PlayerBlue, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .size(72.dp)
                            .border(2.dp, PlayerBlue, CircleShape),
                        shape = CircleShape,
                        color = colors.statusChipBackground
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                player.number?.takeIf { it.isNotBlank() } ?: "👤",
                                fontSize = 24.sp,
                                color = colors.primaryText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                val ratingVal = profile?.rating?.toDoubleOrNull()
                if (ratingVal != null && ratingVal > 0.0) {
                    val badgeColor = when {
                        ratingVal >= 8.0 -> PlayerBlue
                        ratingVal >= 7.0 -> PlayerGreen
                        ratingVal >= 6.0 -> PlayerAmber
                        else -> PlayerRed
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", ratingVal),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name + if (player.isCaptain) " ©" else "",
                    color = colors.primaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = listOfNotNull(
                        teamName.takeIf { it.isNotBlank() },
                        player.number?.takeIf { it.isNotBlank() }?.let { "#$it" },
                        (profile?.position ?: player.position)?.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    color = colors.secondaryText,
                    fontSize = 11.sp
                )
                val marketVal = formatPlayerMarketValue(profile?.marketValueEur)
                if (!marketVal.isNullOrBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PlayerGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "💶 $marketVal",
                            color = PlayerGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerBioTab(profile: PlayerProfile, colors: AppColorScheme) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PlayerDetailRow("Életkor", profile.age?.let { "$it év" }, colors)
        PlayerDetailRow("Nemzetiség", rememberHu(profile.nationality), colors)
        PlayerDetailRow("Születési dátum", profile.birthdate, colors)
        PlayerDetailRow("Születési hely", profile.birthplace, colors)
        PlayerDetailRow("Magasság", profile.height?.let { "$it cm" }, colors)
        PlayerDetailRow("Testsúly", profile.weight?.let { "$it kg" }, colors)
        PlayerDetailRow("Erősebb láb", translateFootHu(profile.preferredFoot), colors)
        PlayerDetailRow("Piaci érték", formatPlayerMarketValue(profile.marketValueEur), colors)
    }
}

@Composable
private fun PlayerStatsTab(profile: PlayerProfile, colors: AppColorScheme) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = profile.seasonLabel?.let { "Szezon – $it" } ?: "Szezon statisztikák",
            color = colors.tertiaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatMiniBox("Meccs", profile.appearances ?: "–", colors, Modifier.weight(1f))
            StatMiniBox("Gól", profile.goals ?: "–", colors, Modifier.weight(1f))
            StatMiniBox("Gólpassz", profile.assists ?: "–", colors, Modifier.weight(1f))
            StatMiniBox("Perc", profile.minutesPlayed ?: "–", colors, Modifier.weight(1f))
        }

        HorizontalDivider(color = colors.divider)

        PlayerDetailRow("Kezdőként", profile.startingLineups, colors)
        PlayerDetailRow("Átlag értékelés", profile.rating?.toDoubleOrNull()?.let { String.format(Locale.US, "%.2f", it) } ?: profile.rating, colors)
        PlayerDetailRow("Lövések (kapura)", formatPair(profile.shotsTotal, profile.shotsOnTarget), colors)
        PlayerDetailRow("Kulcspasszok", profile.keyPasses, colors)
        PlayerDetailRow("Cselek (sikeres / összes)", formatRatio(profile.dribbleSuccess, profile.dribbleAttempts), colors)
        PlayerDetailRow("Párharcok (nyert / összes)", formatRatio(profile.duelsWon, profile.duelsTotal), colors)
        PlayerDetailRow("Szerelések", profile.tackles, colors)
        PlayerDetailRow("Labdaszerzések", profile.interceptions, colors)
        PlayerDetailRow("Szabálytalanság (elköv. / kiharcolt)", formatRatio(profile.foulsCommitted, profile.foulsDrawn), colors)
        PlayerDetailRow("Tizenegyes (gól / kihagyott)", formatRatio(profile.penScored, profile.penMissed), colors)
        PlayerDetailRow("Lapok (sárga / piros)", formatRatio(profile.yellowCards, profile.redCards), colors)
        PlayerDetailRow("Védések", profile.saves, colors)
        PlayerDetailRow("Kapott gólok", profile.goalsConceded?.takeIf { profile.saves != null }, colors)
    }
}

@Composable
private fun StatMiniBox(
    label: String,
    value: String,
    colors: AppColorScheme,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = colors.statusChipBackground
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Text(label, color = colors.tertiaryText, fontSize = 9.sp)
        }
    }
}

@Composable
private fun PlayerCareerTab(profile: PlayerProfile, colors: AppColorScheme) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val hasCareer = listOf(
            profile.careerAppearances, profile.careerMinutes, profile.careerAssists,
            profile.careerShotsTotal, profile.careerPenScored, profile.careerRating
        ).any { !it.isNullOrBlank() }

        if (hasCareer) {
            Text("Klubkarrier összesen", color = colors.tertiaryText, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            PlayerDetailRow("Mérkőzések", profile.careerAppearances, colors)
            PlayerDetailRow("Percek a pályán", profile.careerMinutes, colors)
            PlayerDetailRow("Gólpasszok", profile.careerAssists, colors)
            PlayerDetailRow("Lövések (kapura)", formatPair(profile.careerShotsTotal, profile.careerShotsOnTarget), colors)
            PlayerDetailRow("Tizenegyesgólok", profile.careerPenScored, colors)
            PlayerDetailRow("Átlag értékelés", profile.careerRating?.toDoubleOrNull()?.let { String.format(Locale.US, "%.2f", it) } ?: profile.careerRating, colors)
        }

        if (profile.trophies.isNotEmpty()) {
            if (hasCareer) HorizontalDivider(color = colors.divider)
            Text("Trófeák (${profile.trophies.size})", color = colors.tertiaryText, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            profile.trophies.forEach { trophy ->
                PlayerDetailRow(
                    "🏆 ${trophy.league}",
                    listOfNotNull(rememberHu(trophy.status), trophy.count?.let { "×$it" })
                        .joinToString(" ")
                        .takeIf { it.isNotBlank() },
                    colors
                )
            }
        }

        if (profile.transfers.isNotEmpty()) {
            if (hasCareer || profile.trophies.isNotEmpty()) HorizontalDivider(color = colors.divider)
            Text("Átigazolások", color = colors.tertiaryText, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            profile.transfers.take(10).forEach { tr ->
                PlayerDetailRow(
                    tr.date,
                    listOfNotNull(
                        listOfNotNull(tr.from, tr.to).joinToString(" → ").takeIf { it.isNotBlank() },
                        rememberHu(tr.type),
                        tr.price
                    ).joinToString(" • "),
                    colors
                )
            }
        }

        if (!hasCareer && profile.trophies.isEmpty() && profile.transfers.isEmpty()) {
            Text(
                "Nincs további karrier- vagy átigazolási adat.",
                color = colors.tertiaryText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun PlayerDetailRow(label: String, value: String?, colors: AppColorScheme) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = colors.primaryText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private fun formatPair(total: String?, inner: String?): String? =
    when {
        total.isNullOrBlank() -> null
        inner.isNullOrBlank() -> total
        else -> "$total ($inner)"
    }

private fun formatRatio(first: String?, second: String?): String? =
    when {
        first.isNullOrBlank() && second.isNullOrBlank() -> null
        second.isNullOrBlank() -> first
        first.isNullOrBlank() -> "– / $second"
        else -> "$first / $second"
    }

private fun translateFootHu(foot: String?): String? =
    when (foot?.trim()?.lowercase(Locale.ROOT)) {
        "left" -> "Bal"
        "right" -> "Jobb"
        "both" -> "Mindkettő"
        null, "" -> null
        else -> foot
    }

private fun formatPlayerMarketValue(raw: String?): String? {
    val value = raw?.toLongOrNull() ?: return null
    return when {
        value >= 1_000_000 -> String.format(Locale.US, "%.1f M €", value / 1_000_000.0)
        value >= 1_000 -> String.format(Locale.US, "%.0f E €", value / 1_000.0)
        else -> "$value €"
    }
}
