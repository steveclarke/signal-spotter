package com.example.signalspotter

import android.app.Application
import com.example.signalspotter.data.SpotRepository

/** Owns the single app-wide [SpotRepository]. Registered in the manifest. */
class SignalSpotterApp : Application() {
  lateinit var repository: SpotRepository
    private set

  override fun onCreate() {
    super.onCreate()
    repository = SpotRepository(this)
  }
}
