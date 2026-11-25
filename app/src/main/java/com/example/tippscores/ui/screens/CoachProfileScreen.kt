package com.example.tippscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.tippscores.data.model.CoachProfile
import com.example.tippscores.data.remote.StatpalImages
import com.example.tippscores.data.repository.CoachProfileRepository
import com.example.tippscores.ui.components.RemoteImage
import com.example.tippscores.ui.components.rememberHu
import com.example.tippscores.ui.theme.AppColorScheme
import com.example.tippscores.ui.theme.LocalAppColors
import java.util.Locale

private val AccentBlue = Color(0xFF2563EB)
private val ActiveGreen = Color(0xFF16A34A)
private val GoldAccent = Color(0xFFF59E0B)

@Composable
fun CoachProfileScreen(
    coachId: String,
    coachName: String,
    repository: CoachProfileRepository,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    var profile by remember(coachId) { mutableStateOf<CoachProfile?>(null) }
    var loading by remember(coachId) { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(coachId) {
        loading = true
        profile = try {
            repository.fetch(coachId, coachName)
        } catch (e: Exception) {
            null
        }
        loading = false
    }

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
                    "Edzőprofil",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    ) { paddingValues ->
        val p = profile
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CoachHeroHeader(
                imageUrl = StatpalImages.coach(coachId),
                name = p?.name ?: coachName,
                team = p?.team,
                nationality = rememberHu(p?.nationality),
                age = p?.age,
                colors = colors
            )

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(30.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Edzőadatok betöltése…", color = colors.secondaryText, fontSize = 12.sp)
                    }
                }
            } else if (p == null) {
                Box(modifier = Modifier.padding(16.dp)) {
                    CoachCard(title = null, colors = colors) {
                        Text(
                            "Az edző profiladatai jelenleg nem érhetők el.",
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                CoachKpiRow(profile = p, colors = colors)

                val tabs = listOf(
                    "Adatlap",
                    "Pályafutás (${p.career.size})",
                    "Trófeák (${p.trophies.size})"
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = colors.cardBackground,
                    contentColor = AccentBlue
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> CoachOverviewTab(profile = p, colors = colors)
                    1 -> CoachCareerTab(profile = p, colors = colors)
                    2 -> CoachTrophiesTab(profile = p, colors = colors)
                }
            }
        }
    }
}

@Composable
private fun CoachHeroHeader(
    imageUrl: String?,
    name: String,
    team: String?,
    nationality: String?,
    age: String?,
    colors: AppColorScheme
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        AccentBlue.copy(alpha = 0.16f),
                        colors.screenBackground
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(
                url = imageUrl,
                contentDescription = "$name fotója",
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(2.dp, AccentBlue, CircleShape)
                    .background(colors.cardBackground),
                contentScale = ContentScale.Crop,
                fallback = {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("🧑‍💼", fontSize = 38.sp)
                    }
                }
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = colors.primaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )

                if (!team.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ActiveGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "⚽ $team",
                            color = ActiveGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                val meta = listOfNotNull(
                    nationality?.takeIf { it.isNotBlank() },
                    age?.takeIf { it.isNotBlank() }?.let { "$it év" }
                ).joinToString(" • ")

                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(meta, color = colors.secondaryText, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CoachKpiRow(profile: CoachProfile, colors: AppColorScheme) {
    val totalClubs = profile.career.size
    val totalTrophies = profile.trophies.sumOf { it.count?.toIntOrNull() ?: 1 }
    val goldTrophies = profile.trophies.filter {
        val st = it.status?.lowercase(Locale.ROOT).orEmpty()
        st.contains("win") || st.contains("1st") || st.contains("champ") || st.contains("győz")
    }.sumOf { it.count?.toIntOrNull() ?: 1 }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KpiTile("Klubok", totalClubs.toString(), AccentBlue, colors, Modifier.weight(1f))
        KpiTile("Összes trófea", totalTrophies.toString(), colors.primaryText, colors, Modifier.weight(1f))
        KpiTile("Aranyérem", goldTrophies.toString(), GoldAccent, colors, Modifier.weight(1f))
    }
}

@Composable
private fun KpiTile(
    label: String,
    value: String,
    accent: Color,
    colors: AppColorScheme,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = colors.cardBackground
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Spacer(Modifier.height(2.dp))
            Text(label, color = colors.tertiaryText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CoachOverviewTab(profile: CoachProfile, colors: AppColorScheme) {
    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CoachCard(title = "Személyes adatlap", colors = colors) {
                CoachInfoRow("Teljes név", profile.fullName, colors)
                CoachInfoRow("Életkor", profile.age?.let { "$it év" }, colors)
                CoachInfoRow("Születési dátum", profile.birthdate, colors)
                CoachInfoRow(
                    "Születési hely",
                    listOfNotNull(profile.birthplace, rememberHu(profile.birthcountry))
                        .distinct()
                        .joinToString(", ")
                        .takeIf { it.isNotBlank() },
                    colors
                )
                CoachInfoRow("Nemzetiség", rememberHu(profile.nationality), colors)
                CoachInfoRow("Magasság", profile.height, colors)
                CoachInfoRow("Testsúly", profile.weight, colors)
                CoachInfoRow("Jelenlegi csapat", profile.team, colors)
            }
        }

        if (profile.career.isNotEmpty()) {
            item {
                val latest = profile.career.first()
                CoachCard(title = "Aktuális / Legutóbbi megbízatás", colors = colors) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(latest.teamName, color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text(
                                "${rememberHu(latest.from) ?: "?"} – ${rememberHu(latest.to?.takeIf { it.isNotBlank() }) ?: "jelenleg"}",
                                color = colors.secondaryText,
                                fontSize = 11.sp
                            )
                        }
                        if (latest.to.isNullOrBlank()) {
                            Surface(shape = RoundedCornerShape(6.dp), color = ActiveGreen) {
                                Text(
                                    "AKTÍV",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachCareerTab(profile: CoachProfile, colors: AppColorScheme) {
    if (profile.career.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nincs rögzített edzői pályafutás.", color = colors.secondaryText, fontSize = 12.sp)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(profile.career) { index, stop ->
            val isCurrent = stop.to.isNullOrBlank()
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = colors.cardBackground
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isCurrent) ActiveGreen else AccentBlue)
                        )
                        if (index < profile.career.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(22.dp)
                                    .background(colors.divider)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stop.teamName,
                            color = colors.primaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${rememberHu(stop.from) ?: "?"} – ${rememberHu(stop.to?.takeIf { it.isNotBlank() }) ?: "jelenleg"}",
                            color = colors.secondaryText,
                            fontSize = 11.sp
                        )
                    }

                    if (isCurrent) {
                        Surface(shape = RoundedCornerShape(6.dp), color = ActiveGreen.copy(alpha = 0.16f)) {
                            Text(
                                "AKTÍV",
                                color = ActiveGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachTrophiesTab(profile: CoachProfile, colors: AppColorScheme) {
    if (profile.trophies.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nincs rögzített trófea.", color = colors.secondaryText, fontSize = 12.sp)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(profile.trophies) { _, trophy ->
            val st = trophy.status?.lowercase(Locale.ROOT).orEmpty()
            val isGold = st.contains("win") || st.contains("1st") || st.contains("champ") || st.contains("győz")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = colors.cardBackground
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isGold) "🏆" else "🥈", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            listOfNotNull(trophy.league, rememberHu(trophy.country)?.let { "($it)" }).joinToString(" "),
                            color = colors.primaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        val sub = listOfNotNull(rememberHu(trophy.status), trophy.seasons).joinToString(" • ")
                        if (sub.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(sub, color = colors.tertiaryText, fontSize = 11.sp)
                        }
                    }
                    trophy.count?.takeIf { it.isNotBlank() }?.let { cnt ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isGold) GoldAccent.copy(alpha = 0.2f) else colors.statusChipBackground
                        ) {
                            Text(
                                "×$cnt",
                                color = if (isGold) GoldAccent else colors.primaryText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachCard(
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
                HorizontalDivider(color = colors.divider)
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}

@Composable
private fun CoachInfoRow(label: String, value: String?, colors: AppColorScheme) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colors.secondaryText, fontSize = 12.sp)
        Text(
            value,
            color = colors.primaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
