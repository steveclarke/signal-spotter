package com.example.signalspotter.data

import kotlinx.serialization.Serializable

/** One logging outing: everything captured between Start and Stop. */
@Serializable
data class Trip(
  val id: Long,
  val startedAtMillis: Long,
  val endedAtMillis: Long? = null,
  val label: String? = null,
  val spots: List<LoggedSpot> = emptyList(),
  val track: List<TrackPoint> = emptyList(),
) {
  val isActive: Boolean
    get() = endedAtMillis == null
}

/** A breadcrumb along the path travelled (recorded continuously while logging). */
@Serializable
data class TrackPoint(val latitude: Double, val longitude: Double, val timestampMillis: Long)
