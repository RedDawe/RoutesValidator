package cz.dd.routesvalidator

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route
import java.time.LocalDateTime
import kotlin.random.Random

class CaptureLocationWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationCapturingManager = LocationCapturingManager.getInstance()
    private val waypointsManager = WaypointsManager.getInstance()
    private val mapsAPIConnector = MapsAPIConnector.getInstance()

    override suspend fun doWork(): Result {
        if (locationCapturingManager.keepCapturing) {
            val nextLocationCapture: OneTimeWorkRequest = OneTimeWorkRequestBuilder<CaptureLocationWorker>()
                .setInitialDelay(WAYPOINT_LOCATION_CAPTURE_DELAY)
                .build()
            WorkManager.getInstance(context).enqueue(nextLocationCapture)

            captureLocation()
        } else {
            captureLocationAndFinishCapturing()
        }
        return Result.success()
    }

    private fun checkCorePermission() : Boolean {
        val missingPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ).filter { ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (missingPermissions.isNotEmpty()) {
            locationCapturingManager.mainActivity?.removedCorePermissionCallback()
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun captureLocation() {
        if (!checkCorePermission()) return
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
//            val a = Coordinate(38.8976, -77.0366)
//            val b = Coordinate(39.9496, -75.1503)
//            appendSuspectedRoute(Route(a, b, emptyList(), LocalDateTime.now()), context)
//            locationCapturingManager.mainActivity?.addedNewSuspectedRouteCallback()
//            processPotentialRoute(Route(a, b, emptyList(), LocalDateTime.now()))
            // TODO: Remove testing code

            if (location != null) {
                processPotentialRoute(waypointsManager.processWaypoint(Coordinate(location.latitude, location.longitude)))
            }
        }
    }

    @SuppressLint("MissingPermission") // TODO: What if permission removed
    private fun captureLocationAndFinishCapturing() {
        if (!checkCorePermission()) return
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                processPotentialRoute(waypointsManager.processWaypoint(Coordinate(location.latitude, location.longitude)))
                processPotentialRoute(waypointsManager.finishAddingWaypoints())
            }
        }
    }

    private fun processPotentialRoute(potentialRoute: Route?) {
        if (potentialRoute != null &&
            !isRouteShortest(potentialRoute, mapsAPIConnector.fetchOptimalWaypointsForRoute(potentialRoute))
        ) {
            appendSuspectedRoute(potentialRoute, context)
            locationCapturingManager.mainActivity?.addedNewSuspectedRouteCallback()
        }
    }
}