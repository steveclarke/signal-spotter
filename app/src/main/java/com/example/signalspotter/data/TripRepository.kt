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
 * Single source of truth for trips. Backed by a JSON file in private storage.
 * Shared by the foreground service (writer) and the UI (reader).
 */
class TripRepository(context: Context) {
  private val file = File(context.filesDir, "trips.json")
  private val legacyFile = File(context.filesDir, "spots.json")
  private val json = Json { ignoreUnknownKeys = true }

  private val _trips = MutableStateFlow<List<Trip>>(emptyList())
  val trips: StateFlow<List<Trip>> = _trips.asStateFlow()

  private val _activeTrip = MutableStateFlow<Trip?>(null)
  val activeTrip: StateFlow<Trip?> = _activeTrip.asStateFlow()

  private val _isLogging = MutableStateFlow(false)
  val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

  private val _debug = MutableStateFlow(DebugStatus())
  val debug: StateFlow<DebugStatus> = _debug.asStateFlow()

  init {
    val loaded =
      runCatching {
          if (file.exists()) json.decodeFromString<List<Trip>>(file.readText()) else emptyList()
        }
        .getOrDefault(emptyList())
        .toMutableList()

    // One-time migration: wrap a legacy flat spots.json into a single trip.
    if (loaded.isEmpty() && legacyFile.exists()) {
      val legacy =
        runCatching { json.decodeFromString<List<LoggedSpot>>(legacyFile.readText()) }
          .getOrDefault(emptyList())
      if (legacy.isNotEmpty()) {
        val start = legacy.minOf { it.timestampMillis }
        val end = legacy.maxOf { it.timestampMillis }
        loaded.add(Trip(id = start, startedAtMillis = start, endedAtMillis = end, spots = legacy))
      }
    }

    // Finalize any trip left open by a killed process so it lands in history.
    for (i in loaded.indices) {
      val t = loaded[i]
      if (t.endedAtMillis == null) {
        loaded[i] =
          t.copy(endedAtMillis = t.spots.maxOfOrNull { it.timestampMillis } ?: t.startedAtMillis)
      }
    }
    commit(loaded)
  }

  private var lastTrackPersistMillis = 0L

  @Synchronized
  private fun commit(list: List<Trip>, persist: Boolean = true) {
    val sorted = list.sortedByDescending { it.startedAtMillis }
    _trips.value = sorted
    _activeTrip.value = sorted.firstOrNull { it.isActive }
    if (persist) runCatching { file.writeText(json.encodeToString(sorted)) }
  }

  @Synchronized
  fun startTrip(nowMillis: Long) {
    val finalized =
      _trips.value.map {
        if (it.isActive)
          it.copy(
            endedAtMillis = it.spots.maxOfOrNull { s -> s.timestampMillis } ?: it.startedAtMillis
          )
        else it
      }
    commit(finalized + Trip(id = nowMillis, startedAtMillis = nowMillis))
  }

  @Synchronized
  fun addSpot(spot: LoggedSpot) {
    val active = _activeTrip.value ?: return
    commit(_trips.value.map { if (it.id == active.id) it.copy(spots = it.spots + spot) else it })
  }

  /** Appends a breadcrumb to the active trip. Persistence is throttled (~10s) to keep disk I/O low; the final point is flushed on [endActiveTrip]. */
  @Synchronized
  fun addTrackPoint(lat: Double, lon: Double, atMillis: Long) {
    val active = _activeTrip.value ?: return
    val point = TrackPoint(lat, lon, atMillis)
    val updated =
      _trips.value.map { if (it.id == active.id) it.copy(track = it.track + point) else it }
    val persist = atMillis - lastTrackPersistMillis >= 10_000L
    if (persist) lastTrackPersistMillis = atMillis
    commit(updated, persist = persist)
  }

  @Synchronized
  fun endActiveTrip(nowMillis: Long) {
    commit(_trips.value.map { if (it.isActive) it.copy(endedAtMillis = nowMillis) else it })
  }

  @Synchronized
  fun rename(id: Long, label: String?) {
    val clean = label?.trim()?.takeIf { it.isNotEmpty() }
    commit(_trips.value.map { if (it.id == id) it.copy(label = clean) else it })
  }

  @Synchronized
  fun delete(id: Long) {
    commit(_trips.value.filterNot { it.id == id })
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

/** Live telemetry for the recording card (not persisted). */
data class DebugStatus(
  val inService: Boolean? = null,
  val carrier: String = "",
  val lastLat: Double? = null,
  val lastLon: Double? = null,
  val lastAccuracyM: Float? = null,
  val lastFixAtMillis: Long? = null,
  val serviceStateChanges: Int = 0,
)
