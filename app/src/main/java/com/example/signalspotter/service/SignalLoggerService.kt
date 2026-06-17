package com.example.signalspotter.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.signalspotter.MainActivity
import com.example.signalspotter.SignalSpotterApp
import com.example.signalspotter.data.LoggedSpot

/**
 * Foreground service that watches for the phone regaining cell service and logs
 * the current GPS location each time it happens.
 */
class SignalLoggerService : Service() {
  private lateinit var telephonyManager: TelephonyManager
  private lateinit var locationManager: LocationManager

  private var telephonyCallback: TelephonyCallback? = null
  @Suppress("DEPRECATION")
  private var phoneStateListener: android.telephony.PhoneStateListener? = null

  @Volatile private var lastLocation: Location? = null
  private var inService: Boolean = true
  private var firstStateSeen: Boolean = false

  private var hasTrackPoint = false
  private var lastTrackLat = 0.0
  private var lastTrackLon = 0.0

  private val locationListener =
    LocationListener { location ->
      lastLocation = location
      repository.onLocation(
        location.latitude,
        location.longitude,
        location.accuracy,
        System.currentTimeMillis(),
      )
      maybeAddTrackPoint(location)
    }

  /** Records a breadcrumb when the fix is trustworthy and we've moved far enough. */
  private fun maybeAddTrackPoint(loc: Location) {
    // Drop low-confidence fixes at the source — this is what kills GPS spikes.
    if (loc.hasAccuracy() && loc.accuracy > TRACK_MAX_ACCURACY_M) return
    if (hasTrackPoint) {
      val results = FloatArray(1)
      Location.distanceBetween(lastTrackLat, lastTrackLon, loc.latitude, loc.longitude, results)
      if (results[0] < TRACK_MIN_DISTANCE_M) return
    }
    repository.addTrackPoint(
      loc.latitude,
      loc.longitude,
      if (loc.hasAccuracy()) loc.accuracy else 0f,
      System.currentTimeMillis(),
    )
    lastTrackLat = loc.latitude
    lastTrackLon = loc.longitude
    hasTrackPoint = true
  }

  private val repository
    get() = (application as SignalSpotterApp).repository

  override fun onCreate() {
    super.onCreate()
    telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startForeground(NOTIFICATION_ID, buildNotification())
    repository.resetDebug()
    repository.startTrip(System.currentTimeMillis())
    repository.setLogging(true)
    hasTrackPoint = false
    startLocationUpdates()
    startTelephonyMonitoring()
    return START_STICKY
  }

  private fun hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED

  private fun startLocationUpdates() {
    if (!hasLocationPermission()) return
    lastLocation =
      runCatching { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
        ?: runCatching { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }
          .getOrNull()
    runCatching {
      locationManager.requestLocationUpdates(
        LocationManager.GPS_PROVIDER,
        LOCATION_INTERVAL_MS,
        0f,
        locationListener,
        Looper.getMainLooper(),
      )
    }
    runCatching {
      locationManager.requestLocationUpdates(
        LocationManager.NETWORK_PROVIDER,
        LOCATION_INTERVAL_MS,
        0f,
        locationListener,
        Looper.getMainLooper(),
      )
    }
  }

  private fun startTelephonyMonitoring() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val callback =
        object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
          override fun onServiceStateChanged(serviceState: ServiceState) {
            handleServiceState(serviceState.state)
          }
        }
      telephonyCallback = callback
      telephonyManager.registerTelephonyCallback(ContextCompat.getMainExecutor(this), callback)
    } else {
      @Suppress("DEPRECATION")
      val listener =
        object : android.telephony.PhoneStateListener() {
          @Deprecated("Deprecated in Java")
          override fun onServiceStateChanged(serviceState: ServiceState?) {
            serviceState?.let { handleServiceState(it.state) }
          }
        }
      phoneStateListener = listener
      @Suppress("DEPRECATION")
      telephonyManager.listen(
        listener,
        android.telephony.PhoneStateListener.LISTEN_SERVICE_STATE,
      )
    }
  }

  /** Logs a spot only on a genuine no-service -> in-service transition. */
  private fun handleServiceState(state: Int) {
    val nowInService = state == ServiceState.STATE_IN_SERVICE
    if (!firstStateSeen) {
      firstStateSeen = true
      inService = nowInService
      repository.onServiceStateChange(nowInService, carrierName())
      return
    }
    if (nowInService != inService) {
      repository.onServiceStateChange(nowInService, carrierName())
      if (nowInService) logCurrentSpot()
    }
    inService = nowInService
  }

  private fun carrierName(): String =
    runCatching { telephonyManager.networkOperatorName }
      .getOrNull()
      ?.takeIf { it.isNotBlank() } ?: "Unknown"

  private fun logCurrentSpot() {
    val loc = lastLocation ?: return
    val carrier = carrierName()
    repository.addSpot(
      LoggedSpot(
        timestampMillis = System.currentTimeMillis(),
        latitude = loc.latitude,
        longitude = loc.longitude,
        accuracyMeters = loc.accuracy,
        carrier = carrier,
      )
    )
  }

  override fun onDestroy() {
    runCatching { locationManager.removeUpdates(locationListener) }
    telephonyCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
    @Suppress("DEPRECATION")
    phoneStateListener?.let {
      telephonyManager.listen(it, android.telephony.PhoneStateListener.LISTEN_NONE)
    }
    repository.endActiveTrip(System.currentTimeMillis())
    repository.setLogging(false)
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun buildNotification(): Notification {
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      manager.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, "Signal logging", NotificationManager.IMPORTANCE_LOW)
      )
    }
    val tapIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
      )
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Signal Spotter is logging")
      .setContentText("Watching for cell signal. Tap to open and stop.")
      .setSmallIcon(android.R.drawable.ic_menu_mylocation)
      .setContentIntent(tapIntent)
      .setOngoing(true)
      .build()
  }

  companion object {
    private const val CHANNEL_ID = "signal_logging"
    private const val NOTIFICATION_ID = 1
    private const val LOCATION_INTERVAL_MS = 3000L
    private const val TRACK_MIN_DISTANCE_M = 12f
    private const val TRACK_MAX_ACCURACY_M = 25f

    fun start(context: Context) {
      ContextCompat.startForegroundService(
        context,
        Intent(context, SignalLoggerService::class.java),
      )
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, SignalLoggerService::class.java))
    }
  }
}
