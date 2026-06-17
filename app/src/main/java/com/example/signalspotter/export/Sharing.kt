package com.example.signalspotter.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.signalspotter.data.LoggedSpot
import java.io.File

/** Writes the spots to a GPX file in cache and launches a share chooser. */
fun shareSpotsAsGpx(context: Context, spots: List<LoggedSpot>) {
  if (spots.isEmpty()) return
  val gpx = GpxExporter.build(spots)
  val dir = File(context.cacheDir, "exports").apply { mkdirs() }
  val file = File(dir, "signal-spots.gpx")
  file.writeText(gpx)

  val uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  val intent =
    Intent(Intent.ACTION_SEND).apply {
      type = "application/gpx+xml"
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, "Signal Spotter coverage spots")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  context.startActivity(
    Intent.createChooser(intent, "Share GPX").apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  )
}
