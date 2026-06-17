package com.example.signalspotter.data

import kotlinx.serialization.Serializable

/** A single place where the phone regained cell service. */
@Serializable
data class LoggedSpot(
  val timestampMillis: Long,
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Float,
  val carrier: String,
)
