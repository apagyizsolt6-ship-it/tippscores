package com.example.tippscores.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMatchClick(match.id) }
            .padding(vertical = 6.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // CS ILLAG & IDŐ
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = match.status,
                    color = if (match.isLive) Color(0xFFDC2626) else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = if (match.isLive) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // CSAPATOK
            Column(modifier = Modifier.weight(1f)) {
                TeamItem(name = match.homeTeam, logoUrl = match.homeTeamLogo)
                Spacer(modifier = Modifier.height(4.dp))
                TeamItem(name = match.awayTeam, logoUrl = match.awayTeamLogo)
            }

            // EREDMÉNY
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = match.homeScore?.toString() ?: "-",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (match.isLive) Color(0xFFDC2626) else Color.Unspecified
                )
                Text(
                    text = match.awayScore?.toString() ?: "-",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (match.isLive) Color(0xFFDC2626) else Color.Unspecified
                )
            }

            // DISZKRÉT IKONOK (Tipp + Videó)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.width(60.dp)
            ) {
                if (match.hasVideoHighlight) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Videó",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (match.tipPrediction != null) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "${match.tipConfidence}%",
                            color = Color(0xFF166534),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 6.dp),
            thickness = 0.5.dp,
            color = Color.LightGray.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun TeamItem(name: String, logoUrl: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = logoUrl,
            contentDescription = name,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = name, fontSize = 13.sp, maxLines = 1)
    }
}
