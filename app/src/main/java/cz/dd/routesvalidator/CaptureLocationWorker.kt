package cz.dd.routesvalidator

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.maps.model.TravelMode
import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route
import java.time.LocalDateTime
import kotlin.random.Random

class CaptureLocationWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCapturingManager: LocationCapturingManager? = null
    private var waypointsManager: WaypointsManager? = null
    private var mapsAPIConnector: MapsAPIConnector? = null

    override suspend fun doWork(): Result {
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            locationCapturingManager = LocationCapturingManager.getInstance()
            waypointsManager = WaypointsManager.getInstance()
            mapsAPIConnector = MapsAPIConnector.getInstance()

            if (locationCapturingManager?.keepCapturing == true) {
                val nextLocationCapture: OneTimeWorkRequest = OneTimeWorkRequestBuilder<CaptureLocationWorker>()
                    .setInitialDelay(WAYPOINT_LOCATION_CAPTURE_DELAY)
                    .build()
                WorkManager.getInstance(context).enqueue(nextLocationCapture)

                captureLocation()
            } else {
                captureLocationAndFinishCapturing()
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("CaptureLocationWorker", e.message, e)
            return Result.success()
        }
    }

    private fun checkCorePermission() : Boolean {
        val missingPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ).filter { ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (missingPermissions.isNotEmpty()) {
            locationCapturingManager?.mainActivity?.removedCorePermissionCallback()
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun captureLocation() {
        if (!checkCorePermission()) return
        fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)?.addOnSuccessListener { location: Location? ->
            if (location != null) {
                processPotentialRoute(waypointsManager?.processWaypoint(Coordinate(location.latitude, location.longitude)))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun captureLocationAndFinishCapturing() {
        if (!checkCorePermission()) return
        fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)?.addOnSuccessListener { location: Location? ->
            if (location != null) {
                processPotentialRoute(waypointsManager?.processWaypoint(Coordinate(location.latitude, location.longitude)))
                processPotentialRoute(waypointsManager?.finishAddingWaypoints())
            }
        }
    }

    private fun processPotentialRoute(potentialRoute: Route?) {
        if (potentialRoute != null) {
            val optimalWaypoints = mapsAPIConnector?.fetchOptimalWaypointsForRoute(potentialRoute, locationCapturingManager?.travelMode ?: TravelMode.WALKING)
            if (optimalWaypoints != null && !isRouteShortest(potentialRoute, optimalWaypoints)) {
                appendSuspectedRoute(potentialRoute, context)
                locationCapturingManager?.mainActivity?.addedNewSuspectedRouteCallback()
            }
        }
    }
}