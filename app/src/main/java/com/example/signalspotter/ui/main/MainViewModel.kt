package com.example.signalspotter.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.signalspotter.SignalSpotterApp
import com.example.signalspotter.service.SignalLoggerService

class MainViewModel(app: Application) : AndroidViewModel(app) {
  private val repository = (app as SignalSpotterApp).repository

  val trips = repository.trips
  val activeTrip = repository.activeTrip
  val isLogging = repository.isLogging
  val debug = repository.debug

  fun startLogging() = SignalLoggerService.start(getApplication())

  fun stopLogging() = SignalLoggerService.stop(getApplication())

  fun rename(id: Long, label: String?) = repository.rename(id, label)

  fun delete(id: Long) = repository.delete(id)
}
