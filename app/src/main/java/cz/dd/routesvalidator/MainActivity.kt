package cz.dd.routesvalidator

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

private const val NEW_ROUTE_NOTIFICATION_CHANNEL_ID = "NEW_SUSPECTED_ROUTE_ADDED"

private const val CORE_PERMISSION_REMOVED_CHANNEL_ID = "CORE_PERMISSION_REMOVED"

class MainActivity : ComponentActivity() {

    private lateinit var trackingSwitch: Switch
    private val waypointsManager = WaypointsManager.getInstance()
    private val locationCapturingManager = LocationCapturingManager.getInstance()

    private fun explanationMessage(permission: String): String {
        val baseMessage = """
            The main purpose of the app is tracking your movement throughout the day. For that reason we need your
            location. We are now going to ask you for the permissions necessary to access your location.
        """.trimIndent() + System.lineSeparator() + System.lineSeparator()

        if (permission == Manifest.permission.ACCESS_COARSE_LOCATION) {
            return baseMessage + "Please click \"While using the app\", otherwise the app cannot function, then turn the tracking switch back on again"
        }
        if (permission == Manifest.permission.ACCESS_FINE_LOCATION) {
            return baseMessage + "Please click \"Change to precise location\", otherwise the app cannot function, then turn the tracking switch back on again"
        }
        if (permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION) {
            return baseMessage + "Please click \"Allow all the time\" and then come back to the app, otherwise the app cannot function, then turn the tracking switch back on again"
        }
        return ""
    }

    private fun requestCorePermissions(): Boolean {
        val missingPermission = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ).firstOrNull { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
            ?: return true
        AlertDialog.Builder(this)
            .setTitle("Core Permission Needed")
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

        locationCapturingManager.mainActivity = this
        createNotificationChannels()
        requestNotificationPermission()

        trackingSwitch = findViewById(R.id.trackingSwitch)
        trackingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (requestCorePermissions()) {
                    waypointsManager.reset()
                    val locationCaptureRequest = OneTimeWorkRequestBuilder<CaptureLocationWorker>()
                        .build()
                    locationCapturingManager.keepCapturing = true
                    WorkManager.getInstance(this).enqueue(locationCaptureRequest)
                }
            } else {
                locationCapturingManager.keepCapturing = false
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            reloadSuspectedRoutes()
        }
    }

    private fun reloadSuspectedRoutes() {
        val suspectedRoutesView = findViewById<LinearLayout>(R.id.suspectedRoutes)
        suspectedRoutesView.removeAllViews()

        val suspectedRoutes = loadSuspectedRoutes(this)
        if (suspectedRoutes.isEmpty()) {
            suspectedRoutesView.addView(TextView(this).apply { text = "Not optimal routes will appear here" })
        }
        for (route in suspectedRoutes) {
            val buttonsPair = LinearLayout(this)
            buttonsPair.orientation = LinearLayout.HORIZONTAL
            suspectedRoutesView.addView(buttonsPair)

            val openMapsButton = Button(this)
            openMapsButton.text = StringBuilder().append(route.finishTime.toLocalDate())
                .append(System.lineSeparator())
                .append(route.finishTime.toLocalTime())
                .toString()
            openMapsButton.setOnClickListener {
                val gmmIntentUri =
                    Uri.parse("https://www.google.com/maps/dir/?api=1&origin=" + route.origin + "&destination=" + route.destination + "&travelmode=transit")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                startActivity(mapIntent)
            }
            buttonsPair.addView(openMapsButton)

            val deleteButton = ImageButton(this)
            deleteButton.setBackgroundResource(R.drawable.delete)
            deleteButton.setOnClickListener {
                deleteMatchingRoutes(route, this)
                suspectedRoutesView.removeView(buttonsPair)
            }
            buttonsPair.addView(deleteButton)
        }
    }

    fun addedNewSuspectedRouteCallback() {
        reloadSuspectedRoutes()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // TODO: which?
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, NEW_ROUTE_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.new_route)
            .setContentTitle("New suspected route added")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            with(NotificationManagerCompat.from(this)) {
                // constant id means that every notification replaces the previous one
                notify(0, builder.build())
            }
        }
    }

    private fun createNotificationChannels() {
        val newRouteChannel =
            NotificationChannel(NEW_ROUTE_NOTIFICATION_CHANNEL_ID, "New route", NotificationManager.IMPORTANCE_DEFAULT)
                .apply {
                    description = "This channel announces new that a new suspected route has been found."
                }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(newRouteChannel)

        val corePermissionRemovedChannel =
            NotificationChannel(CORE_PERMISSION_REMOVED_CHANNEL_ID, "Core permission removed", NotificationManager.IMPORTANCE_DEFAULT)
                .apply {
                    description = "This channel alerts you if a core permission is removed while tracking is on."
                }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(corePermissionRemovedChannel)
    }

    private fun requestNotificationPermission() {
        if (VERSION.SDK_INT < 33) {
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder(this)
                .setTitle("Option to turn on notifications")
                .setMessage(explanationMessage("This application can notify you when a new suspected not optimal route has been found."))
                .setPositiveButton("OK") { _, _ ->
                    trackingSwitch.isChecked = false
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
                }
                .create()
                .show()
        }
    }

    fun removedCorePermissionCallback() {
        trackingSwitch.isChecked = false
        locationCapturingManager.keepCapturing = false

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // TODO: which?
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CORE_PERMISSION_REMOVED_CHANNEL_ID)
            .setSmallIcon(R.drawable.new_route)
            .setContentTitle("Core permission removed")
            .setContentText("A core permission has been removed and we had to stop tracking. If you wish to turn it on again," +
                    " please go to the app and turn it on again.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            with(NotificationManagerCompat.from(this)) {
                // constant id means that every notification replaces the previous one
                notify(0, builder.build())
            }
        }
    }

}