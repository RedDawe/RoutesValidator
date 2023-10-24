package cz.dd.routesvalidator

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
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

    @SuppressLint("MissingPermission")
    private fun captureLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                processPotentialRoute(waypointsManager.processWaypoint(Coordinate(location.latitude, location.longitude)))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun captureLocationAndFinishCapturing() {
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
            locationCapturingManager.mainActivity?.reloadSuspectedRoutes()
        }
    }
}