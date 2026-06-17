package com.example.signalspotter.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.signalspotter.data.Trip
import java.io.File

/** Writes a trip's spots to a GPX file in cache and launches a share chooser. */
fun shareTripAsGpx(context: Context, trip: Trip, displayName: String) {
  if (trip.spots.isEmpty() && trip.track.isEmpty()) return
  val gpx = GpxExporter.build(trip.spots, trip.track)
  val dir = File(context.cacheDir, "exports").apply { mkdirs() }
  val safe = displayName.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').ifEmpty { "trip" }
  val file = File(dir, "signal-spotter-$safe.gpx")
  file.writeText(gpx)

  val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  val intent =
    Intent(Intent.ACTION_SEND).apply {
      type = "application/gpx+xml"
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, "Signal Spotter — $displayName")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  context.startActivity(
    Intent.createChooser(intent, "Share GPX").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
  )
}
