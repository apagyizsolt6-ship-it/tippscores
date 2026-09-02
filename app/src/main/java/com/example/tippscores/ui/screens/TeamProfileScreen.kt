package com.example.tippscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tippscores.data.model.TeamProfile
import com.example.tippscores.data.repository.TeamProfileRepository
import com.example.tippscores.ui.theme.LocalAppColors

@Composable
fun TeamProfileScreen(
    teamName: String,
    fallbackLogoUrl: String,
    repository: TeamProfileRepository,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    var profile by remember(teamName) { mutableStateOf<TeamProfile?>(null) }
    var loading by remember(teamName) { mutableStateOf(true) }
    var error by remember(teamName) { mutableStateOf<String?>(null) }

    LaunchedEffect(teamName) {
        loading = true
        error = null
        profile = try {
            repository.fetch(teamName)
        } catch (e: Exception) {
            null
        }
        if (profile == null) error = "A csapat profiladatai jelenleg nem érhetők el."
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
                        .clickableCompat(onClick = onBack)
                )
                Text(
                    "Csapatprofil",
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ProfileHeader(
                    name = profile?.name ?: teamName,
                    logoUrl = profile?.logoUrl?.takeIf { it.isNotBlank() } ?: fallbackLogoUrl,
                    country = profile?.country,
                    league = profile?.league,
                    colors = colors
                )
            }

            if (loading) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.cardBackground
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(30.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("Csapatadatok betöltése…", color = colors.secondaryText, fontSize = 12.sp)
                        }
                    }
                }
            } else if (error != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.cardBackground
                    ) {
                        Text(
                            error!!,
                            modifier = Modifier.padding(20.dp),
                            color = colors.secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                if (profile?.statistics.orEmpty().isNotEmpty()) {
                    item {
                        SectionCard(title = "Szezonstatisztikák", colors = colors) {
                            profile!!.statistics.forEach { stat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stat.label, color = colors.secondaryText, fontSize = 12.sp)
                                    Text(stat.value, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    SectionCard(title = "Utolsó 5 mérkőzés", colors = colors) {
                        if (profile?.recentMatches.orEmpty().isEmpty()) {
                            Text("Nincs elérhető korábbi mérkőzés.", color = colors.secondaryText, fontSize = 12.sp)
                        } else {
                            profile!!.recentMatches.forEach { game ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(30.dp),
                                        shape = CircleShape,
                                        color = when (game.result) {
                                            "GY" -> Color(0xFFD1FAE5)
                                            "V" -> Color(0xFFFEE2E2)
                                            else -> Color(0xFFFEF3C7)
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(game.result, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                    Spacer(Modifier.size(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(game.opponent, color = colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("${game.homeTeam} – ${game.awayTeam}", color = colors.tertiaryText, fontSize = 9.sp)
                                    }
                                    Text(
                                        "${game.homeScore} – ${game.awayScore}",
                                        color = colors.primaryText,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
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
private fun ProfileHeader(
    name: String,
    logoUrl: String,
    country: String?,
    league: String?,
    colors: com.example.tippscores.ui.theme.AppColorScheme
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (logoUrl.isNotBlank()) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = "$name logó",
                    modifier = Modifier.size(82.dp).clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("⚽", fontSize = 52.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(name, color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, textAlign = TextAlign.Center)
            if (!country.isNullOrBlank() || !league.isNullOrBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    listOfNotNull(country, league).joinToString(" • "),
                    color = colors.secondaryText,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    colors: com.example.tippscores.ui.theme.AppColorScheme,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(title, color = colors.primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Spacer(Modifier.height(7.dp))
            content()
        }
    }
}

private fun Modifier.clickableCompat(onClick: () -> Unit): Modifier =
    clickable(onClick = onClick)
