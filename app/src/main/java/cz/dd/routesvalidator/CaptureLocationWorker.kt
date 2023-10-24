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
        }
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun captureLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val a = Coordinate(38.8976, -77.0366) // TODO: remove testing code
                val b = Coordinate(39.9496, -75.1503)
                appendSuspectedRoute(Route(a, b, emptyList(), LocalDateTime.now()), context)

                val potentialRoute = waypointsManager.processWaypoint(Coordinate(location.latitude, location.longitude))
                if (potentialRoute != null && !isRouteShortest(
                        potentialRoute,
                        mapsAPIConnector.fetchOptimalWaypointsForRoute(potentialRoute)
                    )
                ) {
                    if (locationCapturingManager.keepCapturing) {
                        appendSuspectedRoute(potentialRoute, context)
                    }
                }
            }
        }
    }
}