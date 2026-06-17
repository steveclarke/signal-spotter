package com.example.signalspotter.ui.main

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.signalspotter.R
import com.example.signalspotter.data.LoggedSpot
import com.example.signalspotter.data.TrackPoint
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** Default view when there are no points — central Newfoundland. */
private val DEFAULT_CENTER = GeoPoint(49.0, -56.0)
private const val DEFAULT_ZOOM = 6.0
private const val SPOT_ZOOM = 13.0

@Composable
fun SpotsMap(
  spots: List<LoggedSpot>,
  track: List<TrackPoint> = emptyList(),
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val mapView =
    remember {
      MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        setDestroyMode(false)
        isTilesScaledToDpi = true
        setBackgroundColor(AndroidColor.parseColor("#E6EFE9"))
      }
    }

  DisposableEffect(Unit) {
    mapView.onResume()
    onDispose { mapView.onPause() }
  }

  // Build overlays and frame the camera ONCE per set of points. Doing camera work
  // in the per-frame `update` block would fight the user's panning (flicker).
  DisposableEffect(track, spots) {
    mapView.overlays.clear()

    val cleaned = cleanTrack(track)

    // Path first, so the spot markers draw on top of it.
    if (cleaned.size >= 2) {
      mapView.overlays.add(
        Polyline(mapView).apply {
          setPoints(cleaned.map { GeoPoint(it.latitude, it.longitude) })
          outlinePaint.color = AndroidColor.parseColor("#16A34A")
          outlinePaint.strokeWidth = 10f
          outlinePaint.strokeCap = Paint.Cap.ROUND
          outlinePaint.strokeJoin = Paint.Join.ROUND
          outlinePaint.isAntiAlias = true
        }
      )
    }
    val pinIcon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
    spots.forEach { s ->
      mapView.overlays.add(
        Marker(mapView).apply {
          position = GeoPoint(s.latitude, s.longitude)
          icon = pinIcon
          setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
          title = "${s.carrier} · ±${s.accuracyMeters.toInt()} m"
        }
      )
    }

    val points =
      cleaned.map { GeoPoint(it.latitude, it.longitude) } +
        spots.map { GeoPoint(it.latitude, it.longitude) }
    mapView.post { frameCamera(mapView, points) }
    mapView.invalidate()
    onDispose {}
  }

  AndroidView(factory = { mapView }, modifier = modifier.clipToBounds())
}

/**
 * Drops GPS spikes from a track so the line follows the real path. Modern points
 * carry accuracy (already filtered when recorded), so we only need a gentle
 * speed cap; legacy points (accuracy unknown) get an aggressive walk-speed cap.
 */
private fun cleanTrack(track: List<TrackPoint>): List<TrackPoint> {
  if (track.size < 3) return track
  val maxSpeed = if (track.any { it.accuracyMeters > 0f }) 40.0 else 10.0
  val out = ArrayList<TrackPoint>(track.size)
  out.add(track[0])
  val results = FloatArray(1)
  for (i in 1 until track.size) {
    val prev = out.last()
    val dt = (track[i].timestampMillis - prev.timestampMillis) / 1000.0
    if (dt <= 0) continue
    android.location.Location.distanceBetween(
      prev.latitude,
      prev.longitude,
      track[i].latitude,
      track[i].longitude,
      results,
    )
    if (results[0] / dt <= maxSpeed) out.add(track[i])
  }
  return out
}

private fun frameCamera(map: MapView, points: List<GeoPoint>) {
  when {
    points.isEmpty() -> {
      map.controller.setZoom(DEFAULT_ZOOM)
      map.controller.setCenter(DEFAULT_CENTER)
    }
    points.size == 1 -> {
      map.controller.setZoom(SPOT_ZOOM)
      map.controller.setCenter(points[0])
    }
    else -> {
      val box = BoundingBox.fromGeoPointsSafe(points)
      runCatching { map.zoomToBoundingBox(box, false, 96) }
    }
  }
}
