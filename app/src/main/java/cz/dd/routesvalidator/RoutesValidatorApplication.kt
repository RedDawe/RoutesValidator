package cz.dd.routesvalidator

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class RoutesValidatorApplication : Application() {

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)

            LocationCapturingManager.getInstance().mainActivity?.let {

                loadRoutes(TO_BE_PROCESSES_ROUTES_FILE_NAME, it).forEach { potentialRoute ->

                    LocationCapturingManager.restore(it)
                    val optimalWaypoints = MapsAPIConnector.getInstance().fetchOptimalWaypointsForRoute(
                        potentialRoute,
                        LocationCapturingManager.getInstance().travelMode
                    )

                    if (isRouteShortest(potentialRoute, optimalWaypoints)) return

                    appendRoute(SUSPECTED_ROUTES_FILE_NAME, potentialRoute, it)
                    it.addedNewSuspectedRouteCallback()
                }
                resetFile(TO_BE_PROCESSES_ROUTES_FILE_NAME, it)
            }
        }
    }

    override fun onCreate() {
        super.onCreate();

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        val connectivityManager = getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
}