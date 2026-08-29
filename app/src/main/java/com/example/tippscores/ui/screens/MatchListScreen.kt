package com.example.tippscores.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tippscores.data.model.Match
import com.example.tippscores.ui.components.LeagueHeader
import com.example.tippscores.ui.components.MatchRow
import com.example.tippscores.ui.components.SettingsDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MatchListScreen(
    matches: List<Match>,
    errorMessage: String?,
    isLoading: Boolean,
    statpalKey: String,
    highlightlyKey: String,
    selectedOffset: Int,
    onRefresh: () -> Unit,
    onDateSelected: (Int) -> Unit,
    onSaveKeys: (String, String) -> Unit,
    onMatchClick: (String) -> Unit
) {

    var showSettingsDialog by
        remember {
            mutableStateOf(false)
        }

    // ========================================================
    // BEÁLLÍTÁSOK
    // ========================================================

    if (showSettingsDialog) {

        SettingsDialog(

            initialStatpalKey =
                statpalKey,

            initialHighlightlyKey =
                highlightlyKey,

            onDismiss = {
                showSettingsDialog = false
            },

            onSave = { sKey, hKey ->

                onSaveKeys(
                    sKey,
                    hKey
                )

                showSettingsDialog = false
            }
        )
    }

    // ========================================================
    // KIVÁLASZTOTT DÁTUM
    // ========================================================

    val selectedDate =
        dateForOffset(
            selectedOffset
        )

    val selectedDateText =
        SimpleDateFormat(
            "yyyy. MMMM d.",
            Locale("hu", "HU")
        ).format(
            selectedDate.time
        )

    // ========================================================
    // FŐ KÉPERNYŐ
    // ========================================================

    Scaffold(

        containerColor =
            Color(0xFFF5F7FB),

        // ====================================================
        // FELSŐ SÁV
        // ====================================================

        topBar = {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFF10182E)
                        )
            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            ),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column {

                        Text(
                            text =
                                "⚽ Foci",

                            color =
                                Color.White,

                            fontWeight =
                                FontWeight.ExtraBold,

                            fontSize =
                                17.sp
                        )

                        Text(
                            text =
                                selectedDateText,

                            color =
                                Color(0xFFB9C5DF),

                            fontSize =
                                11.sp
                        )
                    }

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        IconButton(
                            onClick =
                                onRefresh
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Refresh,

                                contentDescription =
                                    "Frissítés",

                                tint =
                                    Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                showSettingsDialog =
                                    true
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Settings,

                                contentDescription =
                                    "Beállítások",

                                tint =
                                    Color.White
                            )
                        }
                    }
                }
            }
        },

        // ====================================================
        // ALSÓ NAVIGÁCIÓ
        // ====================================================

        bottomBar = {

            NavigationBar(

                containerColor =
                    Color.White,

                tonalElevation =
                    8.dp
            ) {

                NavigationBarItem(

                    selected =
                        true,

                    onClick = {},

                    icon = {
                        Text("⚽")
                    },

                    label = {
                        Text(
                            "Összes",
                            fontSize = 10.sp
                        )
                    }
                )

                NavigationBarItem(

                    selected =
                        false,

                    onClick = {},

                    icon = {
                        Text("🔴")
                    },

                    label = {
                        Text(
                            "Élő",
                            fontSize = 10.sp
                        )
                    }
                )

                NavigationBarItem(

                    selected =
                        false,

                    onClick = {},

                    icon = {
                        Text("★")
                    },

                    label = {
                        Text(
                            "Kedvencek",
                            fontSize = 10.sp
                        )
                    }
                )

                NavigationBarItem(

                    selected =
                        false,

                    onClick = {},

                    icon = {
                        Text("🏆")
                    },

                    label = {
                        Text(
                            "Bajnokságok",
                            fontSize = 10.sp
                        )
                    }
                )
            }
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
        ) {

            // =================================================
            // 15 NAPOS NAPTÁR
            // =================================================

            DateSelector(

                selectedOffset =
                    selectedOffset,

                onDateSelected =
                    onDateSelected
            )

            // =================================================
            // DÁTUM / MECCSSZÁM KÁRTYA
            // =================================================

            Surface(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        ),

                shape =
                    RoundedCornerShape(12.dp),

                color =
                    Color.White,

                shadowElevation =
                    1.dp
            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        // Nem használunk Material CalendarToday
                        // ikont, mert nincs benne a jelenlegi
                        // projekt ikoncsomagjában.

                        Text(
                            text =
                                "📅",

                            fontSize =
                                17.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.width(7.dp)
                        )

                        Text(

                            text =
                                if (
                                    selectedOffset == 0
                                ) {
                                    "Mai mérkőzések"
                                } else {
                                    selectedDateText
                                },

                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                13.sp,

                            color =
                                Color(0xFF172033)
                        )
                    }

                    Surface(

                        color =
                            Color(0xFFEFF6FF),

                        shape =
                            RoundedCornerShape(8.dp)
                    ) {

                        Text(

                            text =
                                "${matches.size} meccs",

                            color =
                                Color(0xFF2563EB),

                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                11.sp,

                            modifier =
                                Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                )
                        )
                    }
                }
            }

            // =================================================
            // TARTALOM
            // =================================================

            Box(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
            ) {

                when {

                    // =========================================
                    // BETÖLTÉS
                    // =========================================

                    isLoading -> {

                        Column(

                            modifier =
                                Modifier.fillMaxSize(),

                            horizontalAlignment =
                                Alignment.CenterHorizontally,

                            verticalArrangement =
                                Arrangement.Center
                        ) {

                            CircularProgressIndicator(

                                color =
                                    Color(0xFF2563EB),

                                strokeWidth =
                                    3.dp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            Text(

                                text =
                                    "Mérkőzések betöltése…",

                                color =
                                    Color(0xFF64748B),

                                fontSize =
                                    13.sp
                            )
                        }
                    }

                    // =========================================
                    // HIBA
                    // =========================================

                    !errorMessage.isNullOrEmpty() -> {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally,

                            verticalArrangement =
                                Arrangement.Center
                        ) {

                            Text(

                                text =
                                    "⚠️ Hiba történt",

                                fontWeight =
                                    FontWeight.ExtraBold,

                                color =
                                    Color(0xFFDC2626),

                                fontSize =
                                    18.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(

                                text =
                                    errorMessage,

                                textAlign =
                                    TextAlign.Center,

                                color =
                                    Color(0xFF64748B),

                                fontSize =
                                    12.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(16.dp)
                            )

                            Button(

                                onClick =
                                    onRefresh,

                                shape =
                                    RoundedCornerShape(10.dp),

                                colors =
                                    ButtonDefaults
                                        .buttonColors(
                                            containerColor =
                                                Color(0xFF2563EB)
                                        )
                            ) {

                                Text(
                                    "Újrapróbálás"
                                )
                            }
                        }
                    }

                    // =========================================
                    // NINCS MECCS
                    // =========================================

                    matches.isEmpty() -> {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally,

                            verticalArrangement =
                                Arrangement.Center
                        ) {

                            Text(
                                text =
                                    "⚽",

                                fontSize =
                                    36.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(

                                text =
                                    "Nincs mérkőzés ezen a napon.",

                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    Color(0xFF334155)
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )

                            Text(

                                text =
                                    "Válassz másik napot a naptárban.",

                                color =
                                    Color(0xFF94A3B8),

                                fontSize =
                                    12.sp
                            )
                        }
                    }

                    // =========================================
                    // MECCSLISTA
                    // =========================================

                    else -> {

                        val grouped =
                            matches
                                .groupBy {
                                    "${it.leagueCountry}|${it.leagueName}"
                                }
                                .toList()
                                .sortedBy {
                                    it.first
                                }

                        LazyColumn(

                            modifier =
                                Modifier.fillMaxSize(),

                            contentPadding =
                                PaddingValues(
                                    bottom = 10.dp
                                )
                        ) {

                            grouped.forEach {
                                (_, matchGroup) ->

                                stickyHeader {

                                    LeagueHeader(

                                        country =
                                            matchGroup
                                                .first()
                                                .leagueCountry,

                                        leagueName =
                                            matchGroup
                                                .first()
                                                .leagueName
                                    )
                                }

                                items(

                                    items =
                                        matchGroup,

                                    key = {
                                        it.id
                                    }

                                ) { match ->

                                    MatchRow(

                                        match =
                                            match,

                                        onMatchClick =
                                            onMatchClick
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

// ============================================================
// 15 NAPOS DÁTUMVÁLASZTÓ
// ============================================================

@Composable
private fun DateSelector(

    selectedOffset: Int,

    onDateSelected:
        (Int) -> Unit
) {

    LazyRow(

        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFFF5F7FB)
                )
                .padding(
                    vertical = 8.dp
                ),

        horizontalArrangement =
            Arrangement.spacedBy(6.dp),

        contentPadding =
            PaddingValues(
                horizontal = 8.dp
            )
    ) {

        // ====================================================
        // BALRA
        // ====================================================

        item {

            DateArrow(

                enabled =
                    selectedOffset > -7,

                left =
                    true,

                onClick = {

                    if (
                        selectedOffset > -7
                    ) {

                        onDateSelected(
                            selectedOffset - 1
                        )
                    }
                }
            )
        }

        // ====================================================
        // -7 ... +7
        // ====================================================

        items(
            (-7..7).toList()
        ) { offset ->

            val date =
                dateForOffset(
                    offset
                )

            val dayName =
                SimpleDateFormat(
                    "EEE",
                    Locale("hu", "HU")
                )
                    .format(
                        date.time
                    )
                    .replaceFirstChar {
                        it.uppercase(
                            Locale("hu", "HU")
                        )
                    }

            val dayNumber =
                date.get(
                    Calendar.DAY_OF_MONTH
                ).toString()

            Surface(

                modifier =
                    Modifier
                        .size(
                            width = 52.dp,
                            height = 62.dp
                        )
                        .clickableWithoutRipple {
                            onDateSelected(
                                offset
                            )
                        },

                shape =
                    RoundedCornerShape(12.dp),

                color =
                    if (
                        selectedOffset == offset
                    ) {
                        Color(0xFF2563EB)
                    } else {
                        Color.White
                    },

                shadowElevation =
                    if (
                        selectedOffset == offset
                    ) {
                        3.dp
                    } else {
                        1.dp
                    }
            ) {

                Column(

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(

                        text =
                            dayName.take(2),

                        fontSize =
                            10.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            if (
                                selectedOffset == offset
                            ) {
                                Color.White
                            } else {
                                Color(0xFF64748B)
                            }
                    )

                    Text(

                        text =
                            dayNumber,

                        fontSize =
                            19.sp,

                        fontWeight =
                            FontWeight.ExtraBold,

                        color =
                            if (
                                selectedOffset == offset
                            ) {
                                Color.White
                            } else {
                                Color(0xFF172033)
                            }
                    )

                    Text(

                        text =
                            when {

                                offset == 0 ->
                                    "MA"

                                offset < 0 ->
                                    "−${-offset}"

                                else ->
                                    "+$offset"
                            },

                        fontSize =
                            8.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            if (
                                selectedOffset == offset
                            ) {
                                Color(0xFFDCEBFF)
                            } else {
                                Color(0xFF94A3B8)
                            }
                    )
                }
            }
        }

        // ====================================================
        // JOBBRA
        // ====================================================

        item {

            DateArrow(

                enabled =
                    selectedOffset < 7,

                left =
                    false,

                onClick = {

                    if (
                        selectedOffset < 7
                    ) {

                        onDateSelected(
                            selectedOffset + 1
                        )
                    }
                }
            )
        }
    }
}

// ============================================================
// NYÍL
// ============================================================

@Composable
private fun DateArrow(

    enabled: Boolean,

    left: Boolean,

    onClick: () -> Unit
) {

    Surface(

        modifier =
            Modifier
                .size(
                    width = 42.dp,
                    height = 62.dp
                )
                .clickableWithoutRipple(
                    enabled =
                        enabled,

                    onClick =
                        onClick
                ),

        shape =
            RoundedCornerShape(12.dp),

        color =
            if (enabled) {
                Color.White
            } else {
                Color(0xFFE2E8F0)
            }
    ) {

        Box(
            contentAlignment =
                Alignment.Center
        ) {

            Icon(

                imageVector =
                    if (left) {
                        Icons.Default.ChevronLeft
                    } else {
                        Icons.Default.ChevronRight
                    },

                contentDescription =
                    if (left) {
                        "Előző nap"
                    } else {
                        "Következő nap"
                    },

                tint =
                    if (enabled) {
                        Color(0xFF2563EB)
                    } else {
                        Color(0xFF94A3B8)
                    }
            )
        }
    }
}

// ============================================================
// DÁTUM
// ============================================================

private fun dateForOffset(
    offset: Int
): Calendar {

    return Calendar
        .getInstance()
        .apply {

            set(
                Calendar.HOUR_OF_DAY,
                12
            )

            set(
                Calendar.MINUTE,
                0
            )

            set(
                Calendar.SECOND,
                0
            )

            set(
                Calendar.MILLISECOND,
                0
            )

            add(
                Calendar.DAY_OF_YEAR,
                offset.coerceIn(
                    -7,
                    7
                )
            )
        }
}

// ============================================================
// KATTINTÁS RIPPLE NÉLKÜL
// ============================================================

private fun Modifier.clickableWithoutRipple(

    enabled: Boolean = true,

    onClick: () -> Unit
): Modifier {

    return if (enabled) {

        this.then(
            Modifier.clickableNoRipple(
                onClick
            )
        )

    } else {

        this
    }
}

// ============================================================
// CLICKABLE NO RIPPLE
// ============================================================

private fun Modifier.clickableNoRipple(
    onClick: () -> Unit
): Modifier {

    return clickable(

        indication = null,

        interactionSource =
            androidx.compose.foundation
                .interaction
                .MutableInteractionSource(),

        onClick =
            onClick
    )
}
