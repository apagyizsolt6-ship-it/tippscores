package com.example.tippscores.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Automatikus frissítés állapotsávja: villogó "élő" jelzés, az utolsó
 * frissítés ideje, visszaszámláló a következő frissítésig és kézi frissítés gomb.
 */
@Composable
fun LiveRefreshBar(
    isLive: Boolean,
    intervalSeconds: Int,
    updatedAt: Long?,
    refreshing: Boolean,
    onRefresh: () -> Unit
) {
    val colors = LocalAppColors.current

    // Másodpercenként frissülő óra a visszaszámlálóhoz.
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(1000)
            value = System.currentTimeMillis()
        }
    }

    val transition = rememberInfiniteTransition(label = "livePulse")
    val pulse by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "livePulseAlpha"
    )

    val accent = if (isLive) Color(0xFFDC2626) else Color(0xFF2563EB)

    val secondsLeft = updatedAt?.let {
        ((it + intervalSeconds * 1000L - now) / 1000L).toInt().coerceAtLeast(0)
    }

    val lastUpdate = updatedAt?.let {
        SimpleDateFormat("HH:mm:ss", Locale("hu", "HU")).format(Date(it))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .alpha(if (refreshing) 1f else pulse)
                    .background(accent, CircleShape)
            )

            Spacer(Modifier.width(8.dp))

            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Column {
                    Text(
                        if (isLive) {
                            "ÉLŐ • automatikus frissítés $intervalSeconds mp-enként"
                        } else {
                            "Automatikus frissítés $intervalSeconds mp-enként"
                        },
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        buildString {
                            append(if (lastUpdate != null) "Utoljára: $lastUpdate" else "Betöltés…")
                            if (secondsLeft != null && !refreshing) {
                                append(" • következő: $secondsLeft mp")
                            }
                        },
                        color = colors.tertiaryText,
                        fontSize = 10.sp
                    )
                }
            }

            if (refreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = accent
                )
            } else {
                Text(
                    "⟳",
                    color = accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onRefresh)
                        .padding(horizontal = 6.dp)
                )
            }
        }
    }
}
