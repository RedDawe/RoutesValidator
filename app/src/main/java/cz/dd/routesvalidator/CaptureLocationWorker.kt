package cz.dd.routesvalidator

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.common.util.concurrent.ListenableFuture
import cz.dd.routesvalidator.datamodel.Coordinate

class CaptureLocationWorker(private val appContext: Context, private val workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)
    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        val mainActivity = appContext as MainActivity

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val potentialRoute = mainActivity.waypointsManager.processWaypoint(Coordinate(location.latitude, location.longitude))
                if (potentialRoute != null) {
                    mainActivity.capturedNewRoute(potentialRoute)
                }
            }
        }

        return Result.success()
    }
}