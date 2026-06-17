package com.example.signalspotter.ui.main

import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.signalspotter.data.LoggedSpot
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Default view when there are no spots — central Newfoundland. */
private val DEFAULT_CENTER = GeoPoint(49.0, -56.0)
private const val DEFAULT_ZOOM = 6.0
private const val SPOT_ZOOM = 13.0

@Composable
fun SpotsMap(spots: List<LoggedSpot>, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val mapView =
    remember {
      MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        setDestroyMode(false)
        isTilesScaledToDpi = true
        // Opaque backing so panning never flashes the page background through.
        setBackgroundColor(AndroidColor.parseColor("#E6EFE9"))
      }
    }

  DisposableEffect(Unit) {
    mapView.onResume()
    onDispose { mapView.onPause() }
  }

  // Build markers and frame the camera ONCE per set of spots. Doing this in the
  // AndroidView `update` block (per recomposition) would re-center on every frame
  // and fight the user's panning — the source of the flicker.
  DisposableEffect(spots) {
    mapView.overlays.clear()
    spots.forEach { s ->
      mapView.overlays.add(
        Marker(mapView).apply {
          position = GeoPoint(s.latitude, s.longitude)
          setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
          title = "${s.carrier} · ±${s.accuracyMeters.toInt()} m"
        }
      )
    }
    // Frame once the view has a measured size (post runs after layout).
    mapView.post { frameCamera(mapView, spots) }
    mapView.invalidate()
    onDispose {}
  }

  AndroidView(factory = { mapView }, modifier = modifier.clipToBounds())
}

private fun frameCamera(map: MapView, spots: List<LoggedSpot>) {
  when {
    spots.isEmpty() -> {
      map.controller.setZoom(DEFAULT_ZOOM)
      map.controller.setCenter(DEFAULT_CENTER)
    }
    spots.size == 1 -> {
      map.controller.setZoom(SPOT_ZOOM)
      map.controller.setCenter(GeoPoint(spots[0].latitude, spots[0].longitude))
    }
    else -> {
      val box = BoundingBox.fromGeoPointsSafe(spots.map { GeoPoint(it.latitude, it.longitude) })
      runCatching { map.zoomToBoundingBox(box, false, 96) }
    }
  }
}
