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
import com.google.android.gms.location.Priority
import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LOCATION_CAPTURE_TAG = "LOCATION_CAPTURE_TAG"

class CaptureLocationWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private lateinit var waypointsManager: WaypointsManager
    private lateinit var locationCapturingManager: LocationCapturingManager
    private lateinit var mapsAPIConnector: MapsAPIConnector

    override suspend fun doWork(): Result {
        waypointsManager = WaypointsManager.getInstance(context)
        locationCapturingManager = LocationCapturingManager.getInstance()
        mapsAPIConnector = MapsAPIConnector.getInstance()

        if (locationCapturingManager.keepCapturing) {
            val anyOtherWorkerScheduled = withContext(Dispatchers.IO) {
                WorkManager.getInstance(context).getWorkInfosByTag(LOCATION_CAPTURE_TAG).get()
            }.any { it.state == androidx.work.WorkInfo.State.ENQUEUED }
            if (anyOtherWorkerScheduled) {
                return Result.success()
            }

            val nextLocationCapture: OneTimeWorkRequest = OneTimeWorkRequestBuilder<CaptureLocationWorker>()
                .setInitialDelay(WAYPOINT_LOCATION_CAPTURE_DELAY)
                .addTag(LOCATION_CAPTURE_TAG)
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
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location: Location? ->
            if (location != null) {
                processPotentialRoute(waypointsManager.processWaypoint(Coordinate(location.latitude, location.longitude), context))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun captureLocationAndFinishCapturing() {
        if (!checkCorePermission()) return
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location: Location? ->
            if (location != null) {
                processPotentialRoute(waypointsManager.processWaypoint(Coordinate(location.latitude, location.longitude), context))
                processPotentialRoute(waypointsManager.finishAddingWaypoints(context))
            }
        }
    }

    private fun processPotentialRoute(potentialRoute: Route?) {
        if (potentialRoute != null &&
            !isRouteShortest(potentialRoute, mapsAPIConnector.fetchOptimalWaypointsForRoute(potentialRoute, locationCapturingManager.travelMode))
        ) {
            appendSuspectedRoute(potentialRoute, context)
            locationCapturingManager.mainActivity?.addedNewSuspectedRouteCallback()
        }
    }
}