package com.example.tippscores.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import java.util.Date
import java.util.Locale

@Composable
fun MatchListScreen(
    matches: List<Match>,
    errorMessage: String?,
    isLoading: Boolean,
    statpalKey: String,
    highlightlyKey: String,
    selectedOffset: Int,
    onRefresh: () -> Unit,
    onOffsetSelected: (Int) -> Unit,
    onSaveKeys: (String, String) -> Unit,
    onMatchClick: (String) -> Unit
) {

    var showSettingsDialog by
        remember {
            mutableStateOf(false)
        }

    if (showSettingsDialog) {

        SettingsDialog(
            initialStatpalKey = statpalKey,
            initialHighlightlyKey = highlightlyKey,

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

    val today =
        remember {
            Calendar.getInstance()
        }

    Scaffold(

        topBar = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF0B1224)
                    )
            ) {

                Row(
                    modifier = Modifier
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
                            text = "⚽ Foci",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Mérkőzések",
                            color = Color(0xFFB8C4D9),
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        IconButton(
                            onClick = onRefresh
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
                                showSettingsDialog = true
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

                DateSelector(
                    selectedOffset = selectedOffset,
                    today = today,
                    onOffsetSelected = onOffsetSelected
                )
            }
        },

        bottomBar = {

            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {

                NavigationBarItem(
                    selected = true,
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
                    selected = false,
                    onClick = {},
                    icon = {
                        Text("🔴")
                    },
                    label = {
                        Text(
                            "ÉLŐ",
                            fontSize = 10.sp
                        )
                    }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = {
                        Text("⭐")
                    },
                    label = {
                        Text(
                            "Kedvencek",
                            fontSize = 10.sp
                        )
                    }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = {
                        Text("🏆")
                    },
                    label = {
                        Text(
                            "Bajnokság",
                            fontSize = 10.sp
                        )
                    }
                )
            }
        }

    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Color(0xFFF7F9FC)
                )
        ) {

            when {

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
                                Color(0xFF4F46E5)
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

                            fontSize = 13.sp
                        )
                    }
                }

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
                            text = "⚠️ Hiba történt!",
                            color =
                                Color(0xFFDC2626),

                            fontSize = 18.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )

                        Text(
                            text =
                                errorMessage,

                            color =
                                Color(0xFF64748B),

                            fontSize = 13.sp,

                            textAlign =
                                TextAlign.Center
                        )

                        Spacer(
                            modifier =
                                Modifier.height(18.dp)
                        )

                        Button(
                            onClick = onRefresh
                        ) {

                            Text(
                                "Újrapróbálás"
                            )
                        }
                    }
                }

                matches.isEmpty() -> {

                    Column(
                        modifier =
                            Modifier.fillMaxSize(),

                        horizontalAlignment =
                            Alignment.CenterHorizontally,

                        verticalArrangement =
                            Arrangement.Center
                    ) {

                        Text(
                            text = "⚽",
                            fontSize = 42.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )

                        Text(
                            text =
                                "Nincs mérkőzés ezen a napon.",

                            color =
                                Color(0xFF475569),

                            fontSize = 16.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "Válassz másik napot a naptárban.",

                            color =
                                Color(0xFF94A3B8),

                            fontSize = 12.sp
                        )
                    }
                }

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

                    Column(
                        modifier =
                            Modifier.fillMaxSize()
                    ) {

                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 10.dp,
                                        vertical = 8.dp
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
                                            horizontal = 14.dp,
                                            vertical = 10.dp
                                        ),

                                horizontalArrangement =
                                    Arrangement.SpaceBetween,

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    text =
                                        "📋 ${matches.size} mérkőzés",

                                    color =
                                        Color(0xFF334155),

                                    fontSize = 13.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Text(
                                    text =
                                        formatSelectedDate(
                                            selectedOffset
                                        ),

                                    color =
                                        Color(0xFF4F46E5),

                                    fontSize = 12.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }

                        LazyColumn(
                            modifier =
                                Modifier.fillMaxSize()
                        ) {

                            grouped.forEach {
                                    (_, matchGroup) ->

                                val first =
                                    matchGroup.first()

                                stickyHeader {

                                    LeagueHeader(
                                        country =
                                            first.leagueCountry,

                                        leagueName =
                                            first.leagueName
                                    )
                                }

                                items(
                                    items = matchGroup,
                                    key = {
                                        it.id
                                    }
                                ) { match ->

                                    MatchRow(
                                        match = match,
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
// DÁTUMVÁLASZTÓ
// ============================================================

@Composable
private fun DateSelector(
    selectedOffset: Int,
    today: Calendar,
    onOffsetSelected: (Int) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = 10.dp
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = {
                    if (selectedOffset > -7) {
                        onOffsetSelected(
                            selectedOffset - 1
                        )
                    }
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ArrowBack,

                    contentDescription =
                        "Előző nap",

                    tint =
                        if (selectedOffset > -7)
                            Color.White
                        else
                            Color(0xFF475569)
                )
            }

            Icon(
                imageVector =
                    Icons.Default.CalendarToday,

                contentDescription =
                    "Naptár",

                tint =
                    Color(0xFF93C5FD)
            )

            Spacer(
                modifier =
                    Modifier.width(6.dp)
            )

            Text(
                text =
                    if (selectedOffset == 0)
                        "MA"
                    else
                        formatSelectedDate(
                            selectedOffset
                        ),

                color =
                    Color.White,

                fontSize = 13.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    if (selectedOffset < 7) {
                        onOffsetSelected(
                            selectedOffset + 1
                        )
                    }
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ArrowForward,

                    contentDescription =
                        "Következő nap",

                    tint =
                        if (selectedOffset < 7)
                            Color.White
                        else
                            Color(0xFF475569)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 8.dp
                ),

            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            (-7..7).forEach { offset ->

                val calendar =
                    (today.clone() as Calendar)
                        .apply {
                            add(
                                Calendar.DAY_OF_YEAR,
                                offset
                            )
                        }

                DateChip(
                    offset = offset,
                    calendar = calendar,
                    selected =
                        offset == selectedOffset,

                    onClick = {
                        onOffsetSelected(
                            offset
                        )
                    }
                )
            }
        }
    }
}

// ============================================================
// DÁTUM CHIP
// ============================================================

@Composable
private fun DateChip(
    offset: Int,
    calendar: Calendar,
    selected: Boolean,
    onClick: () -> Unit
) {

    val dayName =
        SimpleDateFormat(
            "EEE",
            Locale("hu", "HU")
        )
            .format(
                calendar.time
            )
            .replaceFirstChar {
                it.uppercase()
            }

    val dayNumber =
        SimpleDateFormat(
            "d",
            Locale.getDefault()
        )
            .format(
                calendar.time
            )

    Surface(
        onClick = onClick,

        shape =
            RoundedCornerShape(10.dp),

        color =
            if (selected)
                Color(0xFF4F46E5)
            else
                Color(0xFF1E293B)
    ) {

        Column(
            modifier =
                Modifier
                    .width(52.dp)
                    .padding(
                        vertical = 7.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    if (offset == 0)
                        "MA"
                    else
                        dayName,

                color =
                    if (selected)
                        Color.White
                    else
                        Color(0xFFCBD5E1),

                fontSize = 10.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text = dayNumber,

                color =
                    Color.White,

                fontSize = 15.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

// ============================================================
// DÁTUM FORMÁZÁSA
// ============================================================

private fun formatSelectedDate(
    offset: Int
): String {

    val calendar =
        Calendar.getInstance()

    calendar.add(
        Calendar.DAY_OF_YEAR,
        offset
    )

    val formatter =
        SimpleDateFormat(
            "yyyy. MM. dd.",
            Locale("hu", "HU")
        )

    return if (offset == 0) {
        "Ma"
    } else {
        formatter.format(
            calendar.time
        )
    }
}
