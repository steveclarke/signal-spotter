package com.example.signalspotter.ui.main

import com.example.signalspotter.data.Trip
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateTimeFmt =
  DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US).withZone(ZoneId.systemDefault())
private val timeFmt =
  DateTimeFormatter.ofPattern("h:mm a", Locale.US).withZone(ZoneId.systemDefault())

fun formatDateTime(millis: Long): String = dateTimeFmt.format(Instant.ofEpochMilli(millis))

fun formatTimeOfDay(millis: Long): String = timeFmt.format(Instant.ofEpochMilli(millis))

/** Auto title when the trip has no custom label. */
fun Trip.displayTitle(): String = label?.takeIf { it.isNotBlank() } ?: formatDateTime(startedAtMillis)

/** Human duration like "47 min" or "1 hr 12 min". */
fun formatDuration(millis: Long): String {
  val totalSec = (millis / 1000).coerceAtLeast(0)
  val h = totalSec / 3600
  val m = (totalSec % 3600) / 60
  return when {
    h > 0 -> "$h hr ${m} min"
    m > 0 -> "$m min"
    else -> "${totalSec} sec"
  }
}

/** Stopwatch clock like "00:12:34" for the live recording timer. */
fun formatElapsed(millis: Long): String {
  val totalSec = (millis / 1000).coerceAtLeast(0)
  val h = totalSec / 3600
  val m = (totalSec % 3600) / 60
  val s = totalSec % 60
  return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

fun Trip.durationMillis(nowMillis: Long): Long = (endedAtMillis ?: nowMillis) - startedAtMillis

fun Trip.spotCountLabel(): String = "${spots.size} ${if (spots.size == 1) "spot" else "spots"}"
