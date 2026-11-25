package com.example.tippscores.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.ui.screens.RatingBadge

data class PitchPlayerUiModel(
    val id: String,
    val name: String,
    val number: String,
    val position: String,
    val normX: Float, // 0f..1f (pálya szélesség)
    val normY: Float, // 0f..1f (pálya hosszúság)
    val rating: Float? = null,
    val goals: Int = 0,
    val assists: Int = 0,
    val yellowCard: Boolean = false,
    val redCard: Boolean = false,
    val subbedOutMinute: Int? = null,
    val minutesPlayed: Int = 90,
    val shots: Int = 0,
    val keyPasses: Int = 0,
    val tackles: Int = 0
)

@Composable
fun PitchHeatMapOverlay(
    players: List<PitchPlayerUiModel>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        players.forEach { p ->
            val center = Offset(x = size.width * p.normX, y = size.height * p.normY)
            val intensity = ((p.rating ?: 6.8f) / 10f).coerceIn(0.4f, 1f)
            val radius = size.minDimension * 0.14f * intensity

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF3D00).copy(alpha = 0.42f * intensity),
                        Color(0xFFFFEA00).copy(alpha = 0.22f * intensity),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
}

@Composable
fun PitchPlayerBadgeNode(
    player: PitchPlayerUiModel,
    teamColor: Color,
    onClick: (PitchPlayerUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(68.dp)
            .clickable { onClick(player) }
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Mezszám kör
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(teamColor)
                    .border(1.5.dp, Color.White, CircleShape)
            ) {
                Text(
                    text = player.number,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Jobb felső sarok: Értékelés
            player.rating?.let { r ->
                RatingBadge(
                    rating = r,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 12.dp, y = (-6).dp)
                )
            }

            // Bal alsó sarok: Gól / Lap / Csere jelvény
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-8).dp, y = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                if (player.goals > 0) Text("⚽", fontSize = 10.sp)
                if (player.assists > 0) Text("👟", fontSize = 10.sp)
                if (player.yellowCard) Text("🟨", fontSize = 10.sp)
                if (player.redCard) Text("🟥", fontSize = 10.sp)
                if (player.subbedOutMinute != null) Text("🔻", fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(2.dp))

        // Játékos név címke
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = player.name.substringAfterLast(" "),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMatchQuickSheet(
    player: PitchPlayerUiModel,
    onDismiss: () -> Unit,
    onOpenFullProfile: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("#${player.number} ${player.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Pozíció: ${player.position} • Játékidő: ${player.minutesPlayed}'", style = MaterialTheme.typography.bodyMedium)
                }
                player.rating?.let { RatingBadge(rating = it) }
            }

            HorizontalDivider()

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                QuickStatItem("Gól", "${player.goals}")
                QuickStatItem("Gólpassz", "${player.assists}")
                QuickStatItem("Lövés", "${player.shots}")
                QuickStatItem("Kulcspassz", "${player.keyPasses}")
                QuickStatItem("Szerelés", "${player.tackles}")
            }

            Button(
                onClick = {
                    onDismiss()
                    onOpenFullProfile(player.id)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Teljes játékos profil megnyitása")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun QuickStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
