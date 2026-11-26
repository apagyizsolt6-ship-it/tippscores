package com.example.tippscores.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val RADAR_SIZE_DP = 280f
private const val RADAR_RADIUS_DP = 92f
private const val RADAR_LABEL_RADIUS_DP = 122f

/**
 * Radar (pókháló) diagram. A [values] 0..1 közötti arányok, a [labels]
 * a tengelyek feliratai (több soros is lehet).
 */
@Composable
fun RadarChart(
    labels: List<String>,
    values: List<Float>,
    accent: Color,
    gridColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    val count = labels.size

    Box(
        modifier = modifier.size(RADAR_SIZE_DP.dp),
        contentAlignment = Alignment.Center
    ) {
        if (count >= 3 && values.size == count) {

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width * (RADAR_RADIUS_DP / RADAR_SIZE_DP)

                fun pointAt(index: Int, fraction: Float): Offset {
                    val angle = -PI / 2.0 + 2.0 * PI * index / count
                    return Offset(
                        center.x + (radius * fraction * cos(angle)).toFloat(),
                        center.y + (radius * fraction * sin(angle)).toFloat()
                    )
                }

                for (ring in 1..4) {
                    val path = Path()
                    for (index in 0 until count) {
                        val point = pointAt(index, ring / 4f)
                        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                    }
                    path.close()
                    drawPath(path, gridColor, style = Stroke(width = 1.dp.toPx()))
                }

                for (index in 0 until count) {
                    drawLine(gridColor, center, pointAt(index, 1f), strokeWidth = 1.dp.toPx())
                }

                val dataPath = Path()
                values.forEachIndexed { index, value ->
                    val point = pointAt(index, value.coerceIn(0.04f, 1f))
                    if (index == 0) dataPath.moveTo(point.x, point.y) else dataPath.lineTo(point.x, point.y)
                }
                dataPath.close()

                drawPath(dataPath, accent.copy(alpha = 0.28f))
                drawPath(dataPath, accent, style = Stroke(width = 2.dp.toPx()))

                values.forEachIndexed { index, value ->
                    drawCircle(accent, radius = 3.5.dp.toPx(), center = pointAt(index, value.coerceIn(0.04f, 1f)))
                }
            }

            labels.forEachIndexed { index, label ->
                val angle = -PI / 2.0 + 2.0 * PI * index / count
                val x = (RADAR_LABEL_RADIUS_DP * cos(angle)).toFloat()
                val y = (RADAR_LABEL_RADIUS_DP * sin(angle)).toFloat()

                Text(
                    text = label,
                    modifier = Modifier
                        .width(78.dp)
                        .offset(x = x.dp, y = y.dp),
                    color = labelColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp
                )
            }
        }
    }
}

/** Három színű (győzelem / döntetlen / vereség) arányos sáv. */
@Composable
fun ResultBar(
    wins: Int,
    draws: Int,
    losses: Int,
    modifier: Modifier = Modifier,
    barHeight: Int = 10,
    emptyColor: Color = Color(0xFF94A3B8)
) {
    val total = wins + draws + losses

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight.dp)
            .clip(RoundedCornerShape((barHeight / 2).dp))
    ) {
        if (total <= 0) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(emptyColor.copy(alpha = 0.35f))
            )
        } else {
            if (wins > 0) {
                Box(
                    modifier = Modifier
                        .weight(wins.toFloat())
                        .fillMaxSize()
                        .background(Color(0xFF16A34A))
                )
            }
            if (draws > 0) {
                Box(
                    modifier = Modifier
                        .weight(draws.toFloat())
                        .fillMaxSize()
                        .background(Color(0xFFD97706))
                )
            }
            if (losses > 0) {
                Box(
                    modifier = Modifier
                        .weight(losses.toFloat())
                        .fillMaxSize()
                        .background(Color(0xFFDC2626))
                )
            }
        }
    }
}

/** Vízszintes arányos sáv (0..1), pl. percentilishez vagy időszakos gólokhoz. */
@Composable
fun FractionBar(
    fraction: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    barHeight: Int = 8
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight.dp)
            .clip(RoundedCornerShape((barHeight / 2).dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(barHeight.dp)
                .clip(RoundedCornerShape((barHeight / 2).dp))
                .background(color)
        )
    }
}
