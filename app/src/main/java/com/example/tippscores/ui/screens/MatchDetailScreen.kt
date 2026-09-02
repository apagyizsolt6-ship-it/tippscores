package com.example.tippscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.data.model.Match
import com.example.tippscores.ui.theme.LocalAppColors

@Composable
fun MatchDetailScreen(
    match: Match?,
    favoriteTeamNames: Set<String>,
    onBack: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleFollowTeam: (String) -> Unit
) {

    val colors = LocalAppColors.current

    Scaffold(

        containerColor =
            colors.screenBackground,

        topBar = {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.topBar)
                        .padding(
                            horizontal = 12.dp,
                            vertical = 14.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "←",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier
                            .padding(end = 14.dp)
                            .clickable(onClick = onBack)
                )

                Text(
                    text = "Mérkőzés részletei",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

    ) { paddingValues ->

        if (match == null) {

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.Center
            ) {

                Text(
                    text = "⚽",
                    fontSize = 36.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Ez a mérkőzés már nem érhető el.",
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            return@Scaffold
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(14.dp)
        ) {

            // ================================================
            // BAJNOKSÁG
            // ================================================

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = match.leagueCountryFlag.ifBlank { "🏆" },
                    fontSize = 16.sp
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = match.leagueCountry,
                    color = colors.secondaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Text(
                    text = "  •  ",
                    color = colors.tertiaryText,
                    fontSize = 12.sp
                )

                Text(
                    text = match.leagueName,
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // ================================================
            // CSAPATOK + EREDMÉNY
            // ================================================

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(16.dp),

                color =
                    if (match.isLive) colors.cardBackgroundLive else colors.cardBackground
            ) {

                Column(
                    modifier =
                        Modifier.padding(18.dp)
                ) {

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.statusChipBackground
                    ) {

                        Text(
                            text = match.status,
                            color =
                                if (match.isLive) Color(0xFFDC2626) else colors.secondaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier =
                                Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                )
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    TeamDetailRow(
                        name = match.homeTeam,
                        score = match.homeScore,
                        isFollowed = favoriteTeamNames.contains(match.homeTeam),
                        primaryText = colors.primaryText,
                        onFollowClick = { onToggleFollowTeam(match.homeTeam) }
                    )

                    Spacer(Modifier.height(14.dp))

                    TeamDetailRow(
                        name = match.awayTeam,
                        score = match.awayScore,
                        isFollowed = favoriteTeamNames.contains(match.awayTeam),
                        primaryText = colors.primaryText,
                        onFollowClick = { onToggleFollowTeam(match.awayTeam) }
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Koppints egy csapatnévre a követéshez (● jelzi a követett csapatot)",
                        color = colors.tertiaryText,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ================================================
            // KEDVENC MECCS KAPCSOLÓ
            // ================================================

            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onToggleFavorite(match.id) },

                shape = RoundedCornerShape(14.dp),

                color = colors.cardBackground
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            if (match.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,

                        contentDescription = "Kedvenc meccs",

                        tint =
                            if (match.isFavorite) Color(0xFFF59E0B) else colors.tertiaryText
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text =
                            if (match.isFavorite) "Eltávolítás a kedvencekből" else "Hozzáadás a kedvencekhez",

                        color = colors.primaryText,

                        fontWeight = FontWeight.SemiBold,

                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Részletes esemény-idővonal (percenkénti gólok, lapok) ehhez a mérkőzéshez jelenleg nem elérhető.",
                color = colors.tertiaryText,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TeamDetailRow(
    name: String,
    score: Int?,
    isFollowed: Boolean,
    primaryText: Color,
    onFollowClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onFollowClick),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text =
                (if (isFollowed) "● " else "") + name,

            color =
                if (isFollowed) Color(0xFF2563EB) else primaryText,

            fontWeight = FontWeight.Bold,

            fontSize = 17.sp,

            modifier =
                Modifier.weight(1f)
        )

        Text(
            text = score?.toString() ?: "-",
            color = primaryText,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp
        )
    }
}
