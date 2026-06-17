package com.example.signalspotter.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.signalspotter.data.LoggedSpot
import java.util.Locale

/** A single logged spot, numbered. Shared by the home and trip-detail screens. */
@Composable
fun SpotRow(number: Int, spot: LoggedSpot, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF16A34A)),
      contentAlignment = Alignment.Center,
    ) {
      Text("$number", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
    Column {
      Text(
        "${formatTimeOfDay(spot.timestampMillis)} · ${spot.carrier}",
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
      )
      Text(
        String.format(
          Locale.US,
          "%.5f, %.5f · ±%dm",
          spot.latitude,
          spot.longitude,
          spot.accuracyMeters.toInt(),
        ),
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
