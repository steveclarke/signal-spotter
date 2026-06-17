package com.example.signalspotter.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.signalspotter.data.Trip
import kotlinx.coroutines.delay

private val Green = Color(0xFF16A34A)
private val GreenDeep = Color(0xFF166534)
private val GreenTint = Color(0xFFECFDF3)
private val GreenBright = Color(0xFF22C55E)
private val Danger = Color(0xFFB91C1C)

@Composable
fun HomeScreen(
  onOpenTrip: (Long) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainViewModel = viewModel(),
) {
  val context = LocalContext.current
  val trips by viewModel.trips.collectAsStateWithLifecycle()
  val activeTrip by viewModel.activeTrip.collectAsStateWithLifecycle()
  val isLogging by viewModel.isLogging.collectAsStateWithLifecycle()
  val debug by viewModel.debug.collectAsStateWithLifecycle()

  val permissions = remember { requiredPermissions() }
  var pendingStart by remember { mutableStateOf(false) }
  val launcher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      result ->
      if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true && pendingStart) {
        viewModel.startLogging()
      }
      pendingStart = false
    }

  fun start() {
    if (hasAllPermissions(context, permissions)) viewModel.startLogging()
    else {
      pendingStart = true
      launcher.launch(permissions)
    }
  }

  Column(modifier = modifier.fillMaxSize().padding(horizontal = 18.dp)) {
    Text(
      "Signal Spotter",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
    )

    if (isLogging && activeTrip != null) {
      RecordingCard(activeTrip!!, debug, onStop = { viewModel.stopLogging() })
    } else {
      Button(
        onClick = { start() },
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Green),
      ) {
        StatusDot(GreenBright)
        Spacer(Modifier.size(10.dp))
        Text("Start a trip", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
      }
      Spacer(Modifier.height(12.dp))
      Text(
        "Not recording — tap Start before you head out.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Spacer(Modifier.height(20.dp))

    if (isLogging && activeTrip != null) {
      SectionHeader("This trip so far", activeTrip!!.spotCountLabel())
      TripSpots(activeTrip!!)
    } else if (trips.isEmpty()) {
      EmptyState()
    } else {
      val totalSpots = trips.sumOf { it.spots.size }
      val tripWord = if (trips.size == 1) "trip" else "trips"
      SectionHeader("Your trips", "${trips.size} $tripWord · $totalSpots spots")
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(trips, key = { it.id }) { trip -> TripRow(trip, onClick = { onOpenTrip(trip.id) }) }
      }
    }
  }
}

@Composable
private fun RecordingCard(trip: Trip, debug: com.example.signalspotter.data.DebugStatus, onStop: () -> Unit) {
  // Tick once a second so the timer advances.
  var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(trip.id) {
    while (true) {
      now = System.currentTimeMillis()
      delay(1000)
    }
  }
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = GreenTint),
  ) {
    Column(Modifier.padding(20.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(Danger)
        Spacer(Modifier.size(8.dp))
        Text(
          "RECORDING TRIP",
          color = GreenDeep,
          fontWeight = FontWeight.SemiBold,
          fontSize = 13.sp,
        )
      }
      Text(
        formatElapsed(trip.durationMillis(now)),
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(Modifier.weight(1f), "${trip.spots.size}", "spots logged")
        val signal =
          when (debug.inService) {
            true -> "Yes"
            false -> "No"
            null -> "—"
          }
        val sub =
          if (debug.inService == true && debug.carrier.isNotBlank() && debug.carrier != "Unknown")
            "signal · ${debug.carrier}"
          else "signal"
        StatTile(Modifier.weight(1f), signal, sub, dot = debug.inService)
        val gps = debug.lastAccuracyM?.let { "±${it.toInt()}m" } ?: "…"
        StatTile(Modifier.weight(1f), gps, "GPS fix")
      }
      Spacer(Modifier.height(16.dp))
      OutlinedButton(
        onClick = onStop,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
      ) {
        Text("Stop & save trip", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

@Composable
private fun StatTile(modifier: Modifier, value: String, label: String, dot: Boolean? = null) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
  ) {
    Column(Modifier.padding(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (dot != null) {
          StatusDot(if (dot) GreenBright else Color(0xFFD1D5DB))
          Spacer(Modifier.size(6.dp))
        }
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
      }
      Text(
        label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}

@Composable
private fun TripRow(trip: Trip, onClick: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
  ) {
    Row(
      Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(GreenTint),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("${trip.spots.size}", color = GreenDeep, fontWeight = FontWeight.Bold, fontSize = 18.sp)
          Text("spots", color = Green, fontSize = 9.sp)
        }
      }
      Column(Modifier.weight(1f)) {
        Text(trip.displayTitle(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(
          tripMeta(trip),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 3.dp),
        )
      }
      Text("›", color = Color(0xFFCBD5E1), fontSize = 22.sp)
    }
  }
}

@Composable
private fun TripSpots(trip: Trip) {
  if (trip.spots.isEmpty()) {
    Text(
      "Waiting for your first signal. When the phone reconnects, the spot drops here.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    return
  }
  LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    items(trip.spots.reversed()) { spot -> SpotRow(trip.spots.indexOf(spot) + 1, spot) }
  }
}

@Composable
private fun SectionHeader(title: String, trailing: String) {
  Row(
    Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    Text(trailing, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
  }
  Spacer(Modifier.height(12.dp))
}

@Composable
private fun EmptyState() {
  Column(
    Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text("No trips yet", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    Spacer(Modifier.height(8.dp))
    Text(
      "Tap Start a trip, then head out. Every time your phone regains a signal, it drops a spot.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
internal fun StatusDot(color: Color) {
  Box(Modifier.size(11.dp).clip(CircleShape).background(color))
}

internal fun tripMeta(trip: Trip): String {
  val parts = mutableListOf<String>()
  // The title already shows the date when there's no custom label, so don't repeat it.
  if (!trip.label.isNullOrBlank()) parts.add(formatDateTime(trip.startedAtMillis))
  trip.endedAtMillis?.let { parts.add(formatDuration(it - trip.startedAtMillis)) }
  parts.add(trip.spotCountLabel())
  return parts.joinToString(" · ")
}

private fun requiredPermissions(): Array<String> {
  val list =
    mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    list.add(Manifest.permission.POST_NOTIFICATIONS)
  }
  return list.toTypedArray()
}

private fun hasAllPermissions(context: Context, permissions: Array<String>): Boolean =
  permissions.all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
  }
