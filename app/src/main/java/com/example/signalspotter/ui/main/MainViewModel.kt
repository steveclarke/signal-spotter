package com.example.signalspotter.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.signalspotter.SignalSpotterApp
import com.example.signalspotter.service.SignalLoggerService

class MainViewModel(app: Application) : AndroidViewModel(app) {
  private val repository = (app as SignalSpotterApp).repository

  val spots = repository.spots
  val isLogging = repository.isLogging

  fun startLogging() = SignalLoggerService.start(getApplication())

  fun stopLogging() = SignalLoggerService.stop(getApplication())

  fun clear() = repository.clear()
}
