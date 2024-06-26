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
import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset


private const val LOCATION_CAPTURE_TAG = "LOCATION_CAPTURE_TAG"

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
            waypointsManager = WaypointsManager.getInstance(context)
            mapsAPIConnector = MapsAPIConnector.getInstance()

            if (locationCapturingManager!!.keepCapturing) {
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
        } catch (e: Exception) {
            Log.e("CaptureLocationWorker", "Error capturing location", e)
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
            locationCapturingManager!!.mainActivity?.runOnUiThread { locationCapturingManager!!.mainActivity?.removedCorePermissionCallback() }
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun captureLocation() {
//        Uncomment to test the app without the need of moving

//        val a = Coordinate(38.8976, -77.0366, 0)
//        val b = Coordinate(39.9496, -75.1503, 1000)
//        val c1 = Coordinate(40.689361, -74.044705, 20000)
//        val c2 = Coordinate(40.689426, -74.044542, 300000)
//        val d = Coordinate(40.703996, -74.064266, 4000000)
//        val minDay = LocalDate.of(1970, 1, 1).toEpochDay()
//        val maxDay = LocalDate.of(2015, 12, 31).toEpochDay()
//        appendRoute(SUSPECTED_ROUTES_FILE_NAME, Route(a, b, listOf(c1), LocalDateTime.ofEpochSecond(ThreadLocalRandom.current().nextLong(minDay, maxDay), 0, ZoneOffset.UTC)), context)
//        appendRoute(SUSPECTED_ROUTES_FILE_NAME, Route(a, b, listOf(c1, c2), LocalDateTime.ofEpochSecond(ThreadLocalRandom.current().nextLong(minDay, maxDay), 0, ZoneOffset.UTC)), context)
//        appendRoute(SUSPECTED_ROUTES_FILE_NAME, Route(a, b, listOf(c1, c2, d), LocalDateTime.ofEpochSecond(ThreadLocalRandom.current().nextLong(minDay, maxDay), 0, ZoneOffset.UTC)), context)
//        appendRoute(SUSPECTED_ROUTES_FILE_NAME, Route(a, b, listOf(c1, c2, d, c1, c2, d), LocalDateTime.ofEpochSecond(ThreadLocalRandom.current().nextLong(minDay, maxDay), 0, ZoneOffset.UTC)), context)
//        appendRoute(SUSPECTED_ROUTES_FILE_NAME, Route(a, b, listOf(c1, c2, d, c1, c2, d, c1), LocalDateTime.ofEpochSecond(ThreadLocalRandom.current().nextLong(minDay, maxDay), 0, ZoneOffset.UTC)), context)
//        locationCapturingManager!!.mainActivity?.runOnUiThread { locationCapturingManager!!.mainActivity?.addedNewSuspectedRouteCallback() }


        if (!checkCorePermission()) return
        fusedLocationClient!!.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location: Location? ->
            if (location != null) {
                val now = LocalDateTime.now()
                Log.i("CaptureLocationWorker", "Captured location: ${location.latitude}, ${location.longitude} at $now")
                processPotentialRoute(waypointsManager!!.processWaypoint(Coordinate(location.latitude, location.longitude, now.toEpochSecond(ZoneOffset.UTC)), context))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun captureLocationAndFinishCapturing() {
        if (!checkCorePermission()) return
        fusedLocationClient!!.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location: Location? ->
            if (location != null) {
                val now = LocalDateTime.now()
                Log.i("CaptureLocationWorker", "Captured location: ${location.latitude}, ${location.longitude} at $now")
                processPotentialRoute(waypointsManager!!.processWaypoint(Coordinate(location.latitude, location.longitude, now.toEpochSecond(ZoneOffset.UTC)), context))
                processPotentialRoute(waypointsManager!!.finishAddingWaypoints(context))
            }
        }
    }

    private fun processPotentialRoute(potentialRoute: Route?) {
        if (potentialRoute == null) return

        val optimalWaypoints: List<Coordinate>
        try {
            optimalWaypoints = mapsAPIConnector!!.fetchOptimalWaypointsForRoute(potentialRoute, locationCapturingManager!!.travelMode)
            Log.i("CaptureLocationWorker", "Optimal waypoints: $optimalWaypoints")
        } catch (e: Exception) {
            appendRoute(TO_BE_PROCESSES_ROUTES_FILE_NAME, potentialRoute, context)
            return
        }

        if (isRouteShortest(potentialRoute, optimalWaypoints)) return

        appendRoute(SUSPECTED_ROUTES_FILE_NAME, potentialRoute, context)
        locationCapturingManager!!.mainActivity?.runOnUiThread { locationCapturingManager!!.mainActivity?.addedNewSuspectedRouteCallback() }
    }
}