package cz.dd.routesvalidator

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

private const val CAPTURE_LOCATION_REQUEST_TAG = "CAPTURE_LOCATION_REQUEST_TAG"

class MainActivity : ComponentActivity() {

    private lateinit var trackingSwitch: Switch

    private fun explanationMessage(permission: String): String {
        val baseMessage = """
            The main purpose of the app is tracking your movement throughout the day. For that reason we need your
            location. We are now going to ask you for the permissions necessary to access your location.
        """.trimIndent() + System.lineSeparator() + System.lineSeparator()

        if (permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return baseMessage + "Please click \"While using the app\", otherwise the app cannot function, then turn the tracking switch back on again"
        }
        if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return baseMessage + "Please click \"Change to precise location\", otherwise the app cannot function, then turn the tracking switch back on again"
        }
        if (permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            return baseMessage + "Please click \"Allow all the time\" and then come back to the app, otherwise the app cannot function, then turn the tracking switch back on again"
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

        resetFile(this)

        trackingSwitch = findViewById<Switch>(R.id.trackingSwitch)
        trackingSwitch.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) {
                if (doPermission()) {
                    WaypointsManager.getInstance().reset()
                    val locationCaptureRequest = OneTimeWorkRequestBuilder<CaptureLocationWorker>()
                        .build()
                    LocationCapturingManager.getInstance().keepCapturing = true
                    WorkManager.getInstance(this).enqueue(locationCaptureRequest)
                }
            } else {
                // TODO: capture 1 last time
                LocationCapturingManager.getInstance().keepCapturing = false
                WaypointsManager.getInstance().finishAddingWaypoints()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            val suspectedRoutesView = findViewById<LinearLayout>(R.id.suspectedRoutes)
            suspectedRoutesView.removeAllViews()

            val suspectedRoutes = loadSuspectedRoutes(this)
            for (route in suspectedRoutes) {
                val button = Button(this)
                button.text = StringBuilder().append(route.origin)
                    .append(" -> ")
                    .append(route.destination)
                    .toString()
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
}