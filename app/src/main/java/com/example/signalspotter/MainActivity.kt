package com.example.signalspotter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.signalspotter.theme.SignalSpotterTheme
import com.example.signalspotter.ui.main.HomeScreen
import com.example.signalspotter.ui.main.MainViewModel
import com.example.signalspotter.ui.main.TripDetailScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SignalSpotterTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          AppRoot()
        }
      }
    }
  }
}

@Composable
private fun AppRoot(viewModel: MainViewModel = viewModel()) {
  var openTripId by rememberSaveable { mutableStateOf<Long?>(null) }
  val trips by viewModel.trips.collectAsStateWithLifecycle()
  val openTrip = trips.firstOrNull { it.id == openTripId }

  if (openTrip != null) {
    BackHandler { openTripId = null }
    TripDetailScreen(
      trip = openTrip,
      onBack = { openTripId = null },
      viewModel = viewModel,
      modifier = Modifier.safeDrawingPadding(),
    )
  } else {
    HomeScreen(
      onOpenTrip = { openTripId = it },
      modifier = Modifier.safeDrawingPadding(),
      viewModel = viewModel,
    )
  }
}
