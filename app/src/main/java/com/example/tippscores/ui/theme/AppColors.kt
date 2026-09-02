package com.example.tippscores.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * A háttér-/szövegszínek, amik sötét módban megváltoznak. A státusz-
 * színek (piros=élő, zöld=vége, sárga=kedvenc csillag, kék=akcentus)
 * SZÁNDÉKOSAN nincsenek itt - azok elég kontrasztosak ahhoz, hogy
 * mindkét módban jól olvashatók maradjanak, nem kellett külön
 * sötét/világos verziót csinálni belőlük.
 */
data class AppColorScheme(
    val isDark: Boolean,
    val screenBackground: Color,
    val cardBackground: Color,
    val cardBackgroundLive: Color,
    val headerFallback: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val divider: Color,
    val chipBackground: Color,
    val statusChipBackground: Color,
    val topBar: Color
)

val LightAppColors = AppColorScheme(
    isDark = false,
    screenBackground = Color(0xFFF5F7FB),
    cardBackground = Color.White,
    cardBackgroundLive = Color(0xFFFFF7F7),
    headerFallback = Color(0xFFFFFBEB),
    primaryText = Color(0xFF0F172A),
    secondaryText = Color(0xFF64748B),
    tertiaryText = Color(0xFF94A3B8),
    divider = Color(0xFFE2E8F0),
    chipBackground = Color(0xFFEFF6FF),
    statusChipBackground = Color(0xFFF1F5F9),
    topBar = Color(0xFF10182E)
)

val DarkAppColors = AppColorScheme(
    isDark = true,
    screenBackground = Color(0xFF0B1220),
    cardBackground = Color(0xFF141C2E),
    cardBackgroundLive = Color(0xFF2A1418),
    headerFallback = Color(0xFF1C2438),
    primaryText = Color(0xFFE7ECF5),
    secondaryText = Color(0xFF9AA7BE),
    tertiaryText = Color(0xFF6B7793),
    divider = Color(0xFF232D45),
    chipBackground = Color(0xFF1B2A44),
    statusChipBackground = Color(0xFF1E293B),
    topBar = Color(0xFF0A0F1E)
)

val LocalAppColors =
    staticCompositionLocalOf { LightAppColors }

@Composable
fun TippScoresTheme(
    darkMode: Boolean,
    content: @Composable () -> Unit
) {

    val colors =
        if (darkMode) DarkAppColors else LightAppColors

    CompositionLocalProvider(
        LocalAppColors provides colors
    ) {
        content()
    }
}
