package com.example.tippscores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tippscores.data.model.Match

@Composable
fun MatchRow(
    match: Match,
    onMatchClick: (String) -> Unit
) {
    val accent = when {
        match.isLive -> Color(0xFFEF4444)
        match.status == "Vége" || match.status == "Hosszabbítás" -> Color(0xFF16A34A)
        else -> Color(0xFF2563EB)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clickable { onMatchClick(match.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (match.isLive) Color(0xFFFFF7F7) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.StarBorder,
                    contentDescription = "Kedvenc",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(2.dp))
                Surface(
                    color = accent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = match.status,
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                TeamItem(name = match.homeTeam, logoUrl = match.homeTeamLogo)
                Spacer(modifier = Modifier.size(5.dp))
                TeamItem(name = match.awayTeam, logoUrl = match.awayTeamLogo)
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    text = match.homeScore?.toString() ?: "–",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (match.isLive) accent else Color(0xFF172033)
                )
                Text(
                    text = match.awayScore?.toString() ?: "–",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (match.isLive) accent else Color(0xFF172033)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(28.dp)
            ) {
                if (match.hasVideoHighlight) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Videó",
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (match.tipPrediction != null) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "${match.tipConfidence ?: 0}%",
                            color = Color(0xFF166534),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamItem(name: String, logoUrl: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (logoUrl.isNotBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = name,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
        } else {
            Spacer(modifier = Modifier.width(25.dp))
        }

        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            color = Color(0xFF172033)
        )
    }
}
