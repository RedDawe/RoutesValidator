package cz.dd.routesvalidator

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import cz.dd.routesvalidator.datamodel.Coordinate
import java.time.Duration


class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var trackingSwitch: Switch
    private val waypoints = mutableListOf<Coordinate>()

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
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                captureLocation()
                handler.postDelayed(this, Duration.ofMinutes(1).toMillis())
            }
        }

        trackingSwitch = findViewById<Switch>(R.id.trackingSwitch)
        trackingSwitch.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) {
                if (doPermission())
                    runnable.run()
            } else {
                handler.removeCallbacks(runnable)

                val notMatchingRoutesLayout = findViewById<LinearLayout>(R.id.notMatchingRoutes)
                val button = Button(this)
                notMatchingRoutesLayout.addView(button)
                button.setOnClickListener {
                    val gmmIntentUri =
                        Uri.parse("https://www.google.com/maps/dir/?api=1&origin=" + "20.344,34.34" + "&destination=" + "20.5666,45.345" + "&travelmode=transit")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    startActivity(mapIntent)

                }
            }
        }
    }

    private fun captureLocation() {
        if (!doPermission()) return // TODO: try removing permissions after switching switch
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                waypoints.add(Coordinate(location.latitude, location.longitude))
            }
        }
    }
}