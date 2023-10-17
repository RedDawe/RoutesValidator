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
import kotlin.random.Random

class CaptureLocationWorker(private val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun doWork(): Result {
        if (LocationCapturingManager.getInstance().keepCapturing) {
            val nextLocationCapture: OneTimeWorkRequest = OneTimeWorkRequestBuilder<CaptureLocationWorker>()
                .setInitialDelay(WAYPOINT_LOCATION_CAPTURE_DELAY)
                .build()
            WorkManager.getInstance(context).enqueue(nextLocationCapture)

            captureLocation()
        }
        return Result.success()
    }



    @SuppressLint("MissingPermission")
    private fun captureLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val potentialRoute = WaypointsManager.getInstance().processWaypoint(Coordinate(location.latitude, location.longitude))
                if (potentialRoute != null && !isRouteShortest(potentialRoute, MapsAPIConnector.getInstance().fetchOptimalWaypointsForRoute(potentialRoute))) {
                    if (LocationCapturingManager.getInstance().keepCapturing) {
                        appendSuspectedRoute(potentialRoute, context)
                    }
                }
            }
        }
    }
}