package com.example.signalspotter.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Single source of truth for logged spots. Backed by a JSON file in the app's
 * private storage. Shared by the foreground service (writer) and the UI (reader).
 */
class SpotRepository(context: Context) {
  private val file = File(context.filesDir, "spots.json")
  private val json = Json { ignoreUnknownKeys = true }

  private val _spots = MutableStateFlow<List<LoggedSpot>>(emptyList())
  val spots: StateFlow<List<LoggedSpot>> = _spots.asStateFlow()

  private val _isLogging = MutableStateFlow(false)
  val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

  private val _debug = MutableStateFlow(DebugStatus())
  val debug: StateFlow<DebugStatus> = _debug.asStateFlow()

  init {
    _spots.value =
      runCatching {
        if (file.exists()) json.decodeFromString<List<LoggedSpot>>(file.readText())
        else emptyList()
      }.getOrDefault(emptyList())
  }

  @Synchronized
  fun add(spot: LoggedSpot) {
    val updated = _spots.value + spot
    _spots.value = updated
    runCatching { file.writeText(json.encodeToString(updated)) }
  }

  @Synchronized
  fun clear() {
    _spots.value = emptyList()
    runCatching { if (file.exists()) file.delete() }
  }

  fun setLogging(logging: Boolean) {
    _isLogging.value = logging
  }

  fun onLocation(lat: Double, lon: Double, accuracyM: Float, atMillis: Long) {
    _debug.value =
      _debug.value.copy(
        lastLat = lat,
        lastLon = lon,
        lastAccuracyM = accuracyM,
        lastFixAtMillis = atMillis,
      )
  }

  /** Call only on a genuine service-state change. */
  fun onServiceStateChange(inService: Boolean, carrier: String) {
    val d = _debug.value
    _debug.value =
      d.copy(
        inService = inService,
        carrier = carrier,
        serviceStateChanges = d.serviceStateChanges + 1,
      )
  }

  fun resetDebug() {
    _debug.value = DebugStatus()
  }
}

/** Live telemetry for the in-app debug panel (not persisted). */
data class DebugStatus(
  val inService: Boolean? = null,
  val carrier: String = "",
  val lastLat: Double? = null,
  val lastLon: Double? = null,
  val lastAccuracyM: Float? = null,
  val lastFixAtMillis: Long? = null,
  val serviceStateChanges: Int = 0,
)
