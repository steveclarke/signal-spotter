package com.example.signalspotter.export

import com.example.signalspotter.data.LoggedSpot
import com.example.signalspotter.data.TrackPoint
import java.time.Instant
import java.time.format.DateTimeFormatter

/** Builds a GPX 1.1 document: signal spots as waypoints, the path as a track. */
object GpxExporter {
  private val iso = DateTimeFormatter.ISO_INSTANT

  fun build(spots: List<LoggedSpot>, track: List<TrackPoint> = emptyList()): String {
    val sb = StringBuilder()
    sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
    sb.append(
        """<gpx version="1.1" creator="Signal Spotter" """ +
          """xmlns="http://www.topografix.com/GPX/1/1">"""
      )
      .append('\n')
    spots.forEachIndexed { index, spot ->
      val time = iso.format(Instant.ofEpochMilli(spot.timestampMillis))
      sb.append("""  <wpt lat="${spot.latitude}" lon="${spot.longitude}">""").append('\n')
      sb.append("    <time>").append(time).append("</time>").append('\n')
      sb.append("    <name>")
        .append(xml("Signal ${index + 1} (${spot.carrier})"))
        .append("</name>")
        .append('\n')
      sb.append("    <desc>")
        .append(xml("Accuracy ${spot.accuracyMeters.toInt()} m"))
        .append("</desc>")
        .append('\n')
      sb.append("  </wpt>").append('\n')
    }
    if (track.size >= 2) {
      sb.append("  <trk>").append('\n')
      sb.append("    <name>Path travelled</name>").append('\n')
      sb.append("    <trkseg>").append('\n')
      track.forEach { p ->
        val time = iso.format(Instant.ofEpochMilli(p.timestampMillis))
        sb.append("""      <trkpt lat="${p.latitude}" lon="${p.longitude}">""").append('\n')
        sb.append("        <time>").append(time).append("</time>").append('\n')
        sb.append("      </trkpt>").append('\n')
      }
      sb.append("    </trkseg>").append('\n')
      sb.append("  </trk>").append('\n')
    }
    sb.append("</gpx>").append('\n')
    return sb.toString()
  }

  private fun xml(value: String): String =
    value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
}
