package cz.dd.routesvalidator

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.SyncStateContract.Constants
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route
import java.time.Duration
import java.util.concurrent.TimeUnit

private const val CAPTURE_LOCATION_REQUEST_TAG = "CAPTURE_LOCATION_REQUEST_TAG"

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var trackingSwitch: Switch
    val waypointsManager = WaypointsManager()
    private val mapsAPIConnector = MapsAPIConnector()

    private fun explanationMessage(permission: String): String {
        if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return "Please click \"While using the app\", otherwise the app cannot function, then turn the tracking switch back on again"
        }
        if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return "Please click \"Change to precise location\", otherwise the app cannot function, then turn the tracking switch back on again"
        }
        if (permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            return "Please click \"Allow all the time\" and then come back to the app, otherwise the app cannot function, then turn the tracking switch back on again"
        }
        return ""
    }

    private fun doPermission(): Boolean {
        val missingPermission = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ).firstOrNull { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
            ?: return true
        AlertDialog.Builder(this)
            .setTitle("Location Permission Needed")
            .setMessage(explanationMessage(missingPermission))
            .setPositiveButton("OK") { _, _ ->
                trackingSwitch.isChecked = false
                ActivityCompat.requestPermissions(this, arrayOf(missingPermission), 0)
            }
            .create()
            .show()
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val captureLocationRequest = PeriodicWorkRequestBuilder<CaptureLocationWorker>(10, TimeUnit.SECONDS)
            .addTag(CAPTURE_LOCATION_REQUEST_TAG)
            .build()
        val workManager = WorkManager.getInstance(this)

        trackingSwitch = findViewById<Switch>(R.id.trackingSwitch)
        trackingSwitch.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) {
                if (doPermission()) {
                    workManager.enqueue(captureLocationRequest)
                }
            } else {
                workManager.cancelAllWorkByTag(CAPTURE_LOCATION_REQUEST_TAG)
                waypointsManager.finishAddingWaypoints()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            val suspectedRoutesView = findViewById<LinearLayout>(R.id.suspectedRoutes)
            suspectedRoutesView.removeAllViews()

            val suspectedRoutes = loadSuspectedRoutes()
            for (route in suspectedRoutes) {
                val button = Button(this)
                suspectedRoutesView.addView(button)
                button.setOnClickListener {
                    val gmmIntentUri =
                        Uri.parse("https://www.google.com/maps/dir/?api=1&origin=" + route.origin + "&destination=" + route.destination + "&travelmode=transit")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    startActivity(mapIntent)

                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun captureLocation() {
        if (!doPermission()) return // TODO: try removing permissions after switching switch
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val potentialRoute = waypointsManager.processWaypoint(Coordinate(location.latitude, location.longitude))
                if (potentialRoute != null) {
                    capturedNewRoute(potentialRoute)
                }
            }
        }
    }

    fun capturedNewRoute(route: Route) {
        if (isRouteShortest(route, mapsAPIConnector.fetchOptimalWaypointsForRoute(route))) {
            appendSuspectedRoute(route)
        }
    }
}