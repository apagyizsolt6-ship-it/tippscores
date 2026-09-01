package com.example.tippscores.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    onMatchClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {

    val status =
        translateStatus(
            match.status,
            match.isLive
        )

    val statusColor =
        when {

            match.isLive ->
                Color(0xFFDC2626)

            match.status.uppercase() == "FT" ->
                Color(0xFF16A34A)

            match.status.uppercase() == "AET" ->
                Color(0xFFEA580C)

            else ->
                Color(0xFF64748B)
        }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onMatchClick(
                        match.id
                    )
                },

        color =
            if (match.isLive)
                Color(0xFFFFF7F7)
            else
                Color.White
    ) {

        Column {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                // ==================================================
                // CSILLAG + STÁTUSZ
                // ==================================================

                Column(
                    modifier =
                        Modifier.width(48.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .clickable {
                                    onToggleFavorite(match.id)
                                },

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                if (match.isFavorite)
                                    Icons.Filled.Star
                                else
                                    Icons.Outlined.Star,

                            contentDescription =
                                "Kedvenc",

                            tint =
                                if (match.isFavorite)
                                    Color(0xFFF59E0B)
                                else
                                    Color(0xFF94A3B8),

                            modifier =
                                Modifier.size(18.dp)
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )

                    Surface(
                        shape =
                            RoundedCornerShape(5.dp),

                        color =
                            if (match.isLive)
                                Color(0xFFFFE4E6)
                            else
                                Color(0xFFF1F5F9)
                    ) {

                        Text(
                            text = status,

                            color =
                                statusColor,

                            fontSize = 10.sp,

                            fontWeight =
                                if (match.isLive)
                                    FontWeight.Bold
                                else
                                    FontWeight.Medium,

                            modifier =
                                Modifier.padding(
                                    horizontal = 5.dp,
                                    vertical = 2.dp
                                )
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                // ==================================================
                // CSAPATOK
                // ==================================================

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    TeamItem(
                        name =
                            match.homeTeam,

                        logoUrl =
                            match.homeTeamLogo,

                        justScored =
                            match.homeJustScored
                    )

                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )

                    TeamItem(
                        name =
                            match.awayTeam,

                        logoUrl =
                            match.awayTeamLogo,

                        justScored =
                            match.awayJustScored
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                // ==================================================
                // EREDMÉNY
                // ==================================================

                Column(
                    modifier =
                        Modifier.width(28.dp),

                    horizontalAlignment =
                        Alignment.End
                ) {

                    Text(
                        text =
                            match.homeScore
                                ?.toString()
                                ?: "-",

                        color =
                            if (match.isLive)
                                Color(0xFFDC2626)
                            else
                                Color(0xFF111827),

                        fontSize = 14.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            match.awayScore
                                ?.toString()
                                ?: "-",

                        color =
                            if (match.isLive)
                                Color(0xFFDC2626)
                            else
                                Color(0xFF111827),

                        fontSize = 14.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )

                // ==================================================
                // EXTRA IKONOK
                // ==================================================

                Column(
                    modifier =
                        Modifier.width(28.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center
                ) {

                    if (match.hasVideoHighlight) {

                        Icon(
                            imageVector =
                                Icons.Filled.PlayArrow,

                            contentDescription =
                                "Videó",

                            tint =
                                Color(0xFF2563EB),

                            modifier =
                                Modifier.size(20.dp)
                        )
                    }

                    if (
                        match.tipPrediction != null &&
                        match.tipConfidence != null
                    ) {

                        Surface(
                            modifier =
                                Modifier.padding(
                                    top = 3.dp
                                ),

                            shape =
                                RoundedCornerShape(5.dp),

                            color =
                                Color(0xFFDCFCE7)
                        ) {

                            Text(
                                text =
                                    "${match.tipConfidence}%",

                                color =
                                    Color(0xFF166534),

                                fontSize = 8.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                modifier =
                                    Modifier.padding(
                                        horizontal = 3.dp,
                                        vertical = 1.dp
                                    )
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier =
                    Modifier.padding(
                        horizontal = 10.dp
                    ),

                thickness = 0.5.dp,

                color =
                    Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
private fun TeamItem(
    name: String,
    logoUrl: String,
    justScored: Boolean = false
) {

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        if (logoUrl.isNotBlank()) {

            AsyncImage(
                model = logoUrl,

                contentDescription = name,

                modifier =
                    Modifier.size(20.dp)
            )

        } else {

            Surface(
                modifier =
                    Modifier.size(20.dp),

                shape =
                    RoundedCornerShape(5.dp),

                color =
                    Color(0xFFEFF6FF)
            ) {

                Text(
                    text = "⚽",

                    fontSize = 10.sp
                )
            }
        }

        Spacer(
            modifier =
                Modifier.width(8.dp)
        )

        Text(
            text = name,

            color =
                Color(0xFF1E293B),

            fontSize = 13.sp,

            fontWeight =
                FontWeight.SemiBold,

            maxLines = 1
        )

        if (justScored) {

            Spacer(
                modifier =
                    Modifier.width(6.dp)
            )

            GoalFlashBadge()
        }
    }
}

// ================================================================
// "NAGY ESÉLY" VILLOGÓ JELZÉS (gólesemény, mint az eredmenyek.com-nál)
// ================================================================

@Composable
private fun GoalFlashBadge() {

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "goalFlash"
        )

    val flashAlpha by
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 550,
                            easing = LinearEasing
                        ),
                    repeatMode =
                        RepeatMode.Reverse
                ),
            label = "goalFlashAlpha"
        )

    Surface(
        shape =
            RoundedCornerShape(5.dp),

        color =
            Color(0xFFEF4444).copy(
                alpha = flashAlpha
            )
    ) {

        Text(
            text =
                "NAGY ESÉLY",

            color =
                Color.White.copy(
                    alpha = flashAlpha
                ),

            fontSize = 8.sp,

            fontWeight =
                FontWeight.ExtraBold,

            modifier =
                Modifier.padding(
                    horizontal = 5.dp,
                    vertical = 2.dp
                )
        )
    }
}

private fun translateStatus(
    rawStatus: String,
    isLive: Boolean
): String {

    if (isLive) {

        return when {

            rawStatus.matches(
                Regex("^\\d{1,3}'$")
            ) ->
                rawStatus

            rawStatus.equals(
                "HT",
                ignoreCase = true
            ) ->
                "SZÜNET"

            rawStatus.equals(
                "1H",
                ignoreCase = true
            ) ->
                "ÉLŐ"

            rawStatus.equals(
                "2H",
                ignoreCase = true
            ) ->
                "ÉLŐ"

            else ->
                "ÉLŐ"
        }
    }

    return when (
        rawStatus.trim().uppercase()
    ) {

        "FT" ->
            "Vége"

        "AET" ->
            "Hossz."

        "PEN" ->
            "11-esek"

        "POSTP." ->
            "Elhalasztva"

        "POSTP" ->
            "Elhalasztva"

        "POSTPONED" ->
            "Elhalasztva"

        "CANCELLED" ->
            "Törölve"

        "CANC" ->
            "Törölve"

        "ABD" ->
            "Félbeszakadt"

        "NS" ->
            "Még nem kezdődött"

        else ->
            rawStatus
    }
}
