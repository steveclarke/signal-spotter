package com.example.signalspotter

import android.app.Application
import com.example.signalspotter.data.TripRepository
import org.osmdroid.config.Configuration
import java.io.File

/** Owns the single app-wide [TripRepository]. Registered in the manifest. */
class SignalSpotterApp : Application() {
  lateinit var repository: TripRepository
    private set

  override fun onCreate() {
    super.onCreate()
    repository = TripRepository(this)

    // osmdroid: identify ourselves to tile servers and keep tiles in private cache.
    Configuration.getInstance().apply {
      userAgentValue = packageName
      osmdroidBasePath = File(cacheDir, "osmdroid")
      osmdroidTileCache = File(osmdroidBasePath, "tiles")
    }
  }
}
