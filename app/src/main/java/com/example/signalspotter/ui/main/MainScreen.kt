package com.example.signalspotter.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.signalspotter.data.LoggedSpot
import com.example.signalspotter.export.shareSpotsAsGpx
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: MainViewModel = viewModel()) {
  val context = LocalContext.current
  val spots by viewModel.spots.collectAsStateWithLifecycle()
  val isLogging by viewModel.isLogging.collectAsStateWithLifecycle()

  val permissions = remember { requiredPermissions() }
  var pendingStart by remember { mutableStateOf(false) }

  val launcher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      result ->
      val locationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
      if (locationGranted && pendingStart) viewModel.startLogging()
      pendingStart = false
    }

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text("Signal Spotter", style = MaterialTheme.typography.headlineSmall)
    Text(
      text =
        if (isLogging) "Logging — drive on. I'll mark every spot you get a signal."
        else "Stopped. Tap Start before you head out.",
      style = MaterialTheme.typography.bodyMedium,
    )

    Button(
      onClick = {
        if (isLogging) {
          viewModel.stopLogging()
        } else if (hasAllPermissions(context, permissions)) {
          viewModel.startLogging()
        } else {
          pendingStart = true
          launcher.launch(permissions)
        }
      },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(if (isLogging) "Stop logging" else "Start logging")
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = "${spots.size} ${if (spots.size == 1) "spot" else "spots"}",
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.titleMedium,
      )
      TextButton(onClick = { shareSpotsAsGpx(context, spots) }, enabled = spots.isNotEmpty()) {
        Text("Export GPX")
      }
      TextButton(onClick = { viewModel.clear() }, enabled = spots.isNotEmpty()) { Text("Clear") }
    }

    HorizontalDivider()

    if (spots.isEmpty()) {
      Text(
        "No spots yet. Tap Start, then drive. Each time your phone grabs a signal, " +
          "the spot shows up here.",
        style = MaterialTheme.typography.bodyMedium,
      )
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(spots.reversed()) { spot -> SpotRow(spot) }
      }
    }
  }
}

@Composable
private fun SpotRow(spot: LoggedSpot) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(formatTime(spot.timestampMillis), style = MaterialTheme.typography.titleSmall)
      Text(
        String.format(Locale.US, "%.5f, %.5f", spot.latitude, spot.longitude),
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        "${spot.carrier} · ±${spot.accuracyMeters.toInt()} m",
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}

private val timeFormatter =
  DateTimeFormatter.ofPattern("EEE MMM d, h:mm a", Locale.US).withZone(ZoneId.systemDefault())

private fun formatTime(millis: Long): String = timeFormatter.format(Instant.ofEpochMilli(millis))

private fun requiredPermissions(): Array<String> {
  val list =
    mutableListOf(
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.ACCESS_COARSE_LOCATION,
    )
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    list.add(Manifest.permission.POST_NOTIFICATIONS)
  }
  return list.toTypedArray()
}

private fun hasAllPermissions(context: Context, permissions: Array<String>): Boolean =
  permissions.all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
  }
