package com.example.tippscores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.ui.theme.LocalAppColors

@Composable
fun LeagueHeader(
    country: String,
    leagueName: String,
    flag: String = "",
    isCollapsed: Boolean = false,
    isFeatured: Boolean = false,
    onToggleCollapse: () -> Unit = {},
    onToggleFeatured: () -> Unit = {}
) {

    val colors = LocalAppColors.current

    // Világos módban országonként eltérő pasztell háttér, sötét
    // módban egyetlen egységes sötét kártyaszín - így nem kell 5
    // külön sötét pasztell változatot is karbantartani.
    val fallbackColor =
        when (
            country.uppercase()
        ) {

            "ENGLAND", "ANGLIA" ->
                Color(0xFFEFF6FF)

            "SPAIN", "SPANYOLORSZÁG" ->
                Color(0xFFFFF7ED)

            "ITALY", "OLASZORSZÁG" ->
                Color(0xFFF0FDF4)

            "GERMANY", "NÉMETORSZÁG" ->
                Color(0xFFFEF2F2)

            "FRANCE", "FRANCIAORSZÁG" ->
                Color(0xFFF5F3FF)

            else ->
                Color(0xFFFFFBEB)
        }.let { light ->
            if (colors.isDark) {
                colors.headerFallback
            } else {
                light
            }
        }

    // "Üveghatás": áttetsző kék színátmenet, sötétebb kékkel a
    // jobb alsó sarok felé - ettől tűnik üvegesnek/rétegesnek.
    val featuredBrush =
        Brush.linearGradient(
            colors = listOf(
                Color(0xE63B82F6),
                Color(0xCC1D4ED8)
            )
        )

    val backgroundModifier =
        if (isFeatured) {
            Modifier.background(featuredBrush)
        } else {
            Modifier.background(fallbackColor)
        }

    val textColor =
        if (isFeatured) Color.White else colors.primaryText

    val countryTextColor =
        if (isFeatured) Color(0xFFDCEBFF) else colors.secondaryText

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(backgroundModifier)
                .clickable(onClick = onToggleCollapse)
                .padding(
                    horizontal = 12.dp,
                    vertical = 6.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically,

        horizontalArrangement =
            Arrangement.Start
    ) {

        Text(
            text =
                (flag.ifBlank { "🏆" }) + " ",

            fontSize = 11.sp
        )

        Text(
            text =
                country,

            color =
                countryTextColor,

            fontSize = 11.sp,

            fontWeight =
                FontWeight.Bold
        )

        Text(
            text =
                "  •  ",

            color =
                if (isFeatured) Color(0xFFBFDBFE) else colors.tertiaryText,

            fontSize = 10.sp
        )

        Text(
            text =
                leagueName,

            color =
                textColor,

            fontSize = 11.sp,

            fontWeight =
                FontWeight.Bold,

            maxLines = 1,

            modifier =
                Modifier.weight(1f)
        )

        Text(
            text =
                if (isFeatured) "★" else "☆",

            color =
                if (isFeatured) Color(0xFFFBBF24) else colors.tertiaryText,

            fontSize = 15.sp,

            fontWeight =
                FontWeight.Bold,

            modifier =
                Modifier
                    .padding(horizontal = 4.dp)
                    .clickable(onClick = onToggleFeatured)
        )

        Text(
            text =
                if (isCollapsed) "▸" else "▾",

            color =
                if (isFeatured) Color(0xFFDCEBFF) else colors.tertiaryText,

            fontSize = 12.sp,

            fontWeight =
                FontWeight.Bold,

            modifier =
                Modifier.padding(start = 4.dp)
        )
    }
}
