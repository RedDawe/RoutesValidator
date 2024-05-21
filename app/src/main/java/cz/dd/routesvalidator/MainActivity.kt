package cz.dd.routesvalidator

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build.VERSION
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Switch
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.maps.model.TravelMode


private const val NEW_ROUTE_NOTIFICATION_CHANNEL_ID = "NEW_SUSPECTED_ROUTE_ADDED"

private const val CORE_PERMISSION_REMOVED_CHANNEL_ID = "CORE_PERMISSION_REMOVED"

@SuppressLint("UseSwitchCompatOrMaterialCode")
class MainActivity : ComponentActivity() {

    private var waypointsManager: WaypointsManager? = null
    private var locationCapturingManager: LocationCapturingManager? = null
    private var mapsAPIConnector: MapsAPIConnector? = null

    private var trackingSwitch: Switch? = null
    private var travelModeSpinner: Spinner? = null

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
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                trackingSwitch?.isChecked = false
                ActivityCompat.requestPermissions(this, arrayOf(missingPermission), 0)
            }
            .create()
            .show()
        return false
    }

    private fun capitalizeFirstLetter(string: String): String {
        return string.substring(0, 1).uppercase() + string.substring(1).lowercase()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        waypointsManager = WaypointsManager.getInstance(this)
        locationCapturingManager = LocationCapturingManager.getInstance()
        mapsAPIConnector = MapsAPIConnector.getInstance()

        travelModeSpinner = findViewById(R.id.spinner)
        trackingSwitch = findViewById(R.id.trackingSwitch)

        locationCapturingManager?.mainActivity = this
        createNotificationChannels()
        requestNotificationPermission()

        travelModeSpinner?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf(TravelMode.WALKING, TravelMode.TRANSIT, TravelMode.BICYCLING, TravelMode.DRIVING)
                .map { capitalizeFirstLetter(it.toString()) }
        )

        LocationCapturingManager.restore(this)
        val travelMode = locationCapturingManager?.travelMode
        if (travelMode != null) {
            travelModeSpinner?.setSelection(travelModeToIndex(travelMode))
        }

        travelModeSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                locationCapturingManager?.travelMode = indexToTravelMode(position)
                LocationCapturingManager.flushChanges(this@MainActivity)
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
            }
        }


        trackingSwitch?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (requestCorePermissions()) {
                    val locationCaptureRequest = OneTimeWorkRequestBuilder<CaptureLocationWorker>()
                        .build()
                    locationCapturingManager?.keepCapturing = true
                    waypointsManager?.reset(this)
                    locationCapturingManager?.keepCapturing = true
                    LocationCapturingManager.flushChanges(this@MainActivity)
                    WorkManager.getInstance(this).enqueue(locationCaptureRequest)
                    travelModeSpinner?.isEnabled = false
                }
            } else {
                locationCapturingManager?.keepCapturing = false
                LocationCapturingManager.flushChanges(this@MainActivity)
                travelModeSpinner?.isEnabled = true
            }
        }
        LocationCapturingManager.restore(this)
        trackingSwitch?.isChecked = locationCapturingManager?.keepCapturing == true

        val helpButton = findViewById<Button>(R.id.hintButton)
        helpButton.setOnClickListener() {
            AlertDialog.Builder(this)
                .setTitle("Hints (v%s:%s)".format(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME))
                .setMessage("""
                    Please note few important things while using the application:
                    
                    1. The application expects internet connection while tracking is turned on
                    
                    2. Changing travel mode is allowed only while tracking is turned off
                    
                    3. It is recommended to not use too many other applications while tracking is turned on especially on devices with lower ram memory
                """.trimIndent())
                .setPositiveButton("OK") { _, _ -> }
                .create()
                .show()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            reloadSuspectedRoutes()
        }
    }

    private fun reloadSuspectedRoutes() {
        val suspectedRoutes = loadRoutes(SUSPECTED_ROUTES_FILE_NAME, this)

        val suspectedRoutesView = findViewById<ConstraintLayout>(R.id.suspectedRoutes)
        suspectedRoutesView.removeAllViews()
        val suspectedRoutesConstraintSet = ConstraintSet()

        var topConstraint: Button? = null
        for (route in suspectedRoutes) {
            val openMapsButton = Button(this)
            openMapsButton.id = View.generateViewId()
            openMapsButton.text = "Route ending on:${System.lineSeparator()}${dateFormatter.format(route.finishTime)}${System.lineSeparator()}at ${route.finishTime.hour}:${route.finishTime.minute}"
            openMapsButton.setOnClickListener {
                LocationCapturingManager.restore(this@MainActivity)
                val gmmIntentUri =
                    Uri.parse("https://www.google.com/maps/dir/?api=1&origin=" + route.origin + "&destination="
                            + route.destination + "&travelmode=" + locationCapturingManager?.travelMode.toString().lowercase())
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                startActivity(mapIntent)
            }
            openMapsButton.layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            openMapsButton.background.colorFilter = BlendModeColorFilter(Color.parseColor("#80e883"), BlendMode.MULTIPLY)
            suspectedRoutesView.addView(openMapsButton)

            val deleteButton = ImageButton(this)
            deleteButton.id = View.generateViewId()
            deleteButton.setBackgroundResource(R.drawable.delete)
            deleteButton.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete route")
                    .setMessage("Do you really want to delete this route?")
                    .setPositiveButton("Yes") { _, _ ->
                        deleteMatchingRoutes(SUSPECTED_ROUTES_FILE_NAME, route, this)
                        reloadSuspectedRoutes()
                    }
                    .setNegativeButton("No") { _, _ -> }
                    .create()
                    .show()
            }
            suspectedRoutesView.addView(deleteButton)

            suspectedRoutesConstraintSet.clone(suspectedRoutesView)
            suspectedRoutesConstraintSet.connect(openMapsButton.id, ConstraintSet.LEFT, suspectedRoutesView.id, ConstraintSet.LEFT, 10)
            suspectedRoutesConstraintSet.connect(openMapsButton.id, ConstraintSet.RIGHT, deleteButton.id, ConstraintSet.LEFT, 10)
            suspectedRoutesConstraintSet.connect(deleteButton.id, ConstraintSet.RIGHT, suspectedRoutesView.id, ConstraintSet.RIGHT, 10)

            if (topConstraint == null) {
                suspectedRoutesConstraintSet.connect(openMapsButton.id, ConstraintSet.TOP, suspectedRoutesView.id, ConstraintSet.TOP, 10)
                suspectedRoutesConstraintSet.connect(deleteButton.id, ConstraintSet.TOP, suspectedRoutesView.id, ConstraintSet.TOP, 10)
            } else {
                suspectedRoutesConstraintSet.connect(openMapsButton.id, ConstraintSet.TOP, topConstraint.id, ConstraintSet.BOTTOM, 10)
                suspectedRoutesConstraintSet.connect(deleteButton.id, ConstraintSet.TOP, topConstraint.id, ConstraintSet.BOTTOM, 10)
            }
            topConstraint = openMapsButton

            suspectedRoutesConstraintSet.applyTo(suspectedRoutesView)
        }

    }

    fun addedNewSuspectedRouteCallback() {
        reloadSuspectedRoutes()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, NEW_ROUTE_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.new_route)
            .setContentTitle("New suspected route added")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            newRouteChannel
        )

        val corePermissionRemovedChannel =
            NotificationChannel(
                CORE_PERMISSION_REMOVED_CHANNEL_ID,
                "Core permission removed",
                NotificationManager.IMPORTANCE_DEFAULT
            )
                .apply {
                    description = "This channel alerts you if a core permission is removed while tracking is on."
                }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            corePermissionRemovedChannel
        )
    }

    private fun requestNotificationPermission() {
        if (VERSION.SDK_INT < 33) {
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            AlertDialog.Builder(this)
                .setTitle("Option to turn on notifications")
                .setMessage("This application can notify you when a new suspected not optimal route has been found.")
                .setPositiveButton("OK") { _, _ ->
                    trackingSwitch?.isChecked = false
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
                }
                .create()
                .show()
        }
    }

    fun removedCorePermissionCallback() {
        trackingSwitch?.isChecked = false
        locationCapturingManager?.keepCapturing = false
        LocationCapturingManager.flushChanges(this@MainActivity)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CORE_PERMISSION_REMOVED_CHANNEL_ID)
            .setSmallIcon(R.drawable.new_route)
            .setContentTitle("Core permission removed")
            .setContentText(
                "A core permission has been removed and we had to stop tracking. If you wish to turn it on again," +
                        " please go to the app and turn it on again."
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            with(NotificationManagerCompat.from(this)) {
                // constant id means that every notification replaces the previous one
                notify(0, builder.build())
            }
        }
    }
}