package com.example.signalspotter

import android.app.Application
import com.example.signalspotter.data.SpotRepository
import org.osmdroid.config.Configuration
import java.io.File

/** Owns the single app-wide [SpotRepository]. Registered in the manifest. */
class SignalSpotterApp : Application() {
  lateinit var repository: SpotRepository
    private set

  override fun onCreate() {
    super.onCreate()
    repository = SpotRepository(this)

    // osmdroid: identify ourselves to tile servers and keep tiles in private cache.
    Configuration.getInstance().apply {
      userAgentValue = packageName
      osmdroidBasePath = File(cacheDir, "osmdroid")
      osmdroidTileCache = File(osmdroidBasePath, "tiles")
    }
  }
}
