package com.example.signalspotter.ui.main

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.signalspotter.data.Trip
import com.example.signalspotter.export.shareTripAsGpx

private val Green = Color(0xFF16A34A)

@Composable
fun TripDetailScreen(
  trip: Trip,
  onBack: () -> Unit,
  viewModel: MainViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var showMap by remember { mutableStateOf(true) }
  var menuOpen by remember { mutableStateOf(false) }
  var showRename by remember { mutableStateOf(false) }
  var showDelete by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxSize()) {
    // Header
    Row(
      Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        "‹",
        fontSize = 30.sp,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onBack).padding(horizontal = 10.dp),
      )
      Column(Modifier.weight(1f)) {
        Text(trip.displayTitle(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
          tripMeta(trip),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp),
        )
      }
      Box {
        Text(
          "⋯",
          fontSize = 26.sp,
          modifier =
            Modifier.clip(RoundedCornerShape(10.dp)).clickable { menuOpen = true }.padding(horizontal = 12.dp, vertical = 2.dp),
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
          DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; showRename = true })
          DropdownMenuItem(text = { Text("Delete trip") }, onClick = { menuOpen = false; showDelete = true })
        }
      }
    }

    // Map / List toggle
    SegmentedToggle(showMap, onChange = { showMap = it }, modifier = Modifier.padding(horizontal = 18.dp))
    Spacer(Modifier.height(14.dp))

    Box(Modifier.weight(1f).fillMaxWidth()) {
      if (trip.spots.isEmpty()) {
        Text(
          "No spots were logged on this trip — you stayed in (or out of) coverage the whole time.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 18.dp),
        )
      } else if (showMap) {
        SpotsMap(trip.spots, modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp))
      } else {
        LazyColumn(
          Modifier.fillMaxSize().padding(horizontal = 18.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          itemsIndexed(trip.spots) { i, spot -> SpotRow(i + 1, spot) }
        }
      }
    }

    // Action bar
    Row(
      Modifier.fillMaxWidth().padding(18.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Button(
        onClick = { shareTripAsGpx(context, trip, trip.displayTitle()) },
        enabled = trip.spots.isNotEmpty(),
        modifier = Modifier.weight(1f).height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Green),
      ) {
        Text("Export GPX", fontWeight = FontWeight.SemiBold)
      }
      OutlinedButton(
        onClick = { showRename = true },
        modifier = Modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
      ) {
        Text("Rename")
      }
    }
  }

  if (showRename) {
    var text by remember { mutableStateOf(trip.label ?: "") }
    AlertDialog(
      onDismissRequest = { showRename = false },
      title = { Text("Name this trip") },
      text = {
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          singleLine = true,
          placeholder = { Text(formatDateTime(trip.startedAtMillis)) },
        )
      },
      confirmButton = {
        TextButton(onClick = { viewModel.rename(trip.id, text); showRename = false }) { Text("Save") }
      },
      dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
    )
  }

  if (showDelete) {
    AlertDialog(
      onDismissRequest = { showDelete = false },
      title = { Text("Delete this trip?") },
      text = { Text("\"${trip.displayTitle()}\" and its ${trip.spots.size} spots will be removed. This can't be undone.") },
      confirmButton = {
        TextButton(onClick = { viewModel.delete(trip.id); showDelete = false; onBack() }) {
          Text("Delete", color = Color(0xFFB91C1C))
        }
      },
      dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
    )
  }
}

@Composable
private fun SegmentedToggle(showMap: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
  Row(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFECECEB)).padding(4.dp)) {
    SegmentButton("Map", showMap, Modifier.weight(1f)) { onChange(true) }
    SegmentButton("List", !showMap, Modifier.weight(1f)) { onChange(false) }
  }
}

@Composable
private fun SegmentButton(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
  Box(
    modifier
      .clip(RoundedCornerShape(9.dp))
      .background(if (active) Color.White else Color.Transparent)
      .clickable(onClick = onClick)
      .padding(vertical = 9.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      label,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      color = if (active) Color(0xFF18181B) else MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
