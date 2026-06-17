package com.example.signalspotter.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.signalspotter.data.LoggedSpot
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Default view when there are no spots yet — central Newfoundland. */
private val DEFAULT_CENTER = GeoPoint(49.0, -56.0)
private const val DEFAULT_ZOOM = 6.0
private const val SPOT_ZOOM = 12.0

@Composable
fun SpotsMap(spots: List<LoggedSpot>, modifier: Modifier = Modifier) {
  val mapView = rememberMapView()

  AndroidView(
    factory = { mapView },
    modifier = modifier,
    update = { map ->
      map.overlays.clear()
      spots.forEach { spot ->
        val marker = Marker(map)
        marker.position = GeoPoint(spot.latitude, spot.longitude)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "${spot.carrier} · ±${spot.accuracyMeters.toInt()} m"
        map.overlays.add(marker)
      }
      val controller = map.controller
      if (spots.isEmpty()) {
        controller.setZoom(DEFAULT_ZOOM)
        controller.setCenter(DEFAULT_CENTER)
      } else {
        val last = spots.last()
        controller.setZoom(SPOT_ZOOM)
        controller.setCenter(GeoPoint(last.latitude, last.longitude))
      }
      map.invalidate()
    },
  )
}

@Composable
private fun rememberMapView(): MapView {
  val context = androidx.compose.ui.platform.LocalContext.current
  val mapView =
    androidx.compose.runtime.remember {
      MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        setDestroyMode(false)
      }
    }
  DisposableEffect(Unit) {
    mapView.onResume()
    onDispose { mapView.onPause() }
  }
  return mapView
}
