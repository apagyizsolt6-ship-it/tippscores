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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.data.model.CoachProfile
import com.example.tippscores.data.remote.StatpalImages
import com.example.tippscores.data.repository.CoachProfileRepository
import com.example.tippscores.ui.components.RemoteImage
import com.example.tippscores.ui.components.rememberHu
import com.example.tippscores.ui.theme.AppColorScheme
import com.example.tippscores.ui.theme.LocalAppColors

private val CoachAccent = Color(0xFF2563EB)
private val ActiveGreen = Color(0xFF16A34A)
private val TrophyGold = Color(0xFFD97706)

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

    LaunchedEffect(coachId) {
        loading = true
        profile = try { repository.fetch(coachId, coachName) } catch (e: Exception) { null }
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
                    modifier = Modifier.padding(end = 14.dp).clickable(onClick = onBack)
                )
                Text("Edzőprofil", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CoachHeader(
                    imageUrl = StatpalImages.coach(coachId),
                    name = profile?.name ?: coachName,
                    subtitle = listOfNotNull(
                        profile?.team?.takeIf { it.isNotBlank() },
                        rememberHu(profile?.nationality)?.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    colors = colors
                )
            }

            val p = profile
            if (loading) {
                item {
                    CoachCard(title = null, colors = colors) {
                        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(30.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("Edzőadatok betöltése…", color = colors.secondaryText, fontSize = 12.sp)
                        }
                    }
                }
            } else if (p == null) {
                item {
                    CoachCard(title = null, colors = colors) {
                        Text("Az edző profiladatai jelenleg nem érhetők el.", modifier = Modifier.fillMaxWidth(), color = colors.secondaryText, textAlign = TextAlign.Center)
                    }
                }
            } else {
                // Kiemelt KPI mutatók
                item {
                    val totalTrophies = p.trophies.sumOf { it.count?.toIntOrNull() ?: 1 }
                    val titlesWon = p.trophies.filter {
                        val s = it.status.orEmpty().lowercase()
                        s.contains("winner") || s.contains("1st") || s.contains("champion")
                    }.sumOf { it.count?.toIntOrNull() ?: 1 }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CoachKpiTile("Klubok", "${p.career.size}", CoachAccent, colors, Modifier.weight(1f))
                        CoachKpiTile("Trófeák", "$totalTrophies", TrophyGold, colors, Modifier.weight(1f))
                        CoachKpiTile("Bajnoki/Kupa", "$titlesWon", ActiveGreen, colors, Modifier.weight(1f))
                    }
                }

                item {
                    CoachCard(title = "Személyes adatlap", colors = colors) {
                        CoachInfoRow("Teljes név", p.fullName, colors)
                        CoachInfoRow("Életkor", p.age?.let { "$it év" }, colors)
                        CoachInfoRow("Születési dátum", p.birthdate, colors)
                        CoachInfoRow(
                            "Születési hely",
                            listOfNotNull(p.birthplace, rememberHu(p.birthcountry)).distinct().joinToString(", ").takeIf { it.isNotBlank() },
                            colors
                        )
                        CoachInfoRow("Nemzetiség", rememberHu(p.nationality), colors)
                        CoachInfoRow("Magasság", p.height, colors)
                        CoachInfoRow("Testsúly", p.weight, colors)
                        CoachInfoRow("Aktuális csapat", p.team, colors)
                    }
                }

                if (p.career.isNotEmpty()) {
                    item {
                        CoachCard(title = "Edzői pályafutás (Idővonal)", colors = colors) {
                            p.career.forEachIndexed { idx, stop ->
                                val isCurrent = stop.to.isNullOrBlank()
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (isCurrent) ActiveGreen else CoachAccent)
                                        )
                                        if (idx < p.career.lastIndex) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(22.dp)
                                                    .background(colors.divider)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stop.teamName, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            if (isCurrent) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(color = ActiveGreen.copy(alpha = 0.16f), shape = RoundedCornerShape(4.dp)) {
                                                    Text("AKTÍV", color = ActiveGreen, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            "${rememberHu(stop.from) ?: "?"} – ${rememberHu(stop.to?.takeIf { it.isNotBlank() }) ?: "jelenleg"}",
                                            color = colors.secondaryText,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (p.trophies.isNotEmpty()) {
                    item {
                        CoachCard(title = "Trófeavitrin", colors = colors) {
                            p.trophies.forEach { trophy ->
                                val isWinner = trophy.status.orEmpty().lowercase().let {
                                    it.contains("winner") || it.contains("1st") || it.contains("champion")
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (isWinner) "🏆" else "🥈", fontSize = 18.sp, modifier = Modifier.width(28.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            listOfNotNull(trophy.league, rememberHu(trophy.country)?.let { "($it)" }).joinToString(" "),
                                            color = colors.primaryText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        if (!trophy.seasons.isNullOrBlank()) {
                                            Text(trophy.seasons, color = colors.tertiaryText, fontSize = 10.sp)
                                        }
                                    }
                                    Text(
                                        listOfNotNull(rememberHu(trophy.status), trophy.count?.let { "×$it" }).joinToString(" "),
                                        color = if (isWinner) TrophyGold else colors.secondaryText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachKpiTile(label: String, value: String, accent: Color, colors: AppColorScheme, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = colors.cardBackground) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accent, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(label, color = colors.secondaryText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CoachHeader(imageUrl: String?, name: String, subtitle: String, colors: AppColorScheme) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = colors.cardBackground) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(CoachAccent.copy(alpha = 0.14f), Color.Transparent)))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RemoteImage(
                url = imageUrl,
                contentDescription = "$name fotója",
                modifier = Modifier.size(84.dp).clip(CircleShape).border(2.dp, CoachAccent, CircleShape),
                contentScale = ContentScale.Crop,
                fallback = { Text("🧑‍💼", fontSize = 52.sp) }
            )
            Spacer(Modifier.height(10.dp))
            Text(name, color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, textAlign = TextAlign.Center)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(subtitle, color = colors.secondaryText, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun CoachCard(title: String?, colors: AppColorScheme, content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = colors.cardBackground) {
        Column(modifier = Modifier.padding(15.dp)) {
            if (title != null) {
                Text(title, color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.height(7.dp))
            }
            content()
        }
    }
}

@Composable
private fun CoachInfoRow(label: String, value: String?, colors: AppColorScheme) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = colors.secondaryText, fontSize = 12.sp)
        Text(value, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.padding(start = 12.dp))
    }
}
