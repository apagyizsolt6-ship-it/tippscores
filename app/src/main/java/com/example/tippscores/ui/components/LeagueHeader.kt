package com.example.tippscores.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
fun LeagueHeader(
    country: String,
    leagueName: String
) {

    val headerColor =
        when (
            country.uppercase()
        ) {

            "ENGLAND" ->
                Color(0xFFEFF6FF)

            "SPAIN" ->
                Color(0xFFFFF7ED)

            "ITALY" ->
                Color(0xFFF0FDF4)

            "GERMANY" ->
                Color(0xFFFEF2F2)

            "FRANCE" ->
                Color(0xFFF5F3FF)

            else ->
                Color(0xFFFFFBEB)
        }

    Surface(
        modifier =
            Modifier.fillMaxWidth(),

        color =
            headerColor
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
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
                    "🏆 ",

                fontSize = 11.sp
            )

            Text(
                text =
                    country,

                color =
                    Color(0xFF64748B),

                fontSize = 11.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    "  •  ",

                color =
                    Color(0xFFCBD5E1),

                fontSize = 10.sp
            )

            Text(
                text =
                    leagueName,

                color =
                    Color(0xFF0F172A),

                fontSize = 11.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}
