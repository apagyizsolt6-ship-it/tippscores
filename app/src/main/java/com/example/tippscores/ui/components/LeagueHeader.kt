package com.example.tippscores.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LeagueHeader(country: String, leagueName: String) {
    Surface(
        color = Color(0xFFFFFBEB), // Eredmények.com sárgás/bézs sáv
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$country: ",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.Gray
            )
            Text(
                text = leagueName,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.Black
            )
        }
    }
}
