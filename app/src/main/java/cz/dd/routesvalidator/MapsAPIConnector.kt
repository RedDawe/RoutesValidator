package cz.dd.routesvalidator

import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.LatLng
import com.google.maps.model.TravelMode
import com.google.maps.model.Unit
import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route

class MapsAPIConnector private constructor(): AutoCloseable {
    private val context: GeoApiContext = GeoApiContext.Builder()
        .apiKey(BuildConfig.GOOGLE_MAPS_API_KEY)
        .build()

    companion object {

        @Volatile
        private var instance: MapsAPIConnector? = null

        fun getInstance(): MapsAPIConnector {
            return instance ?: synchronized(this) {
                instance ?: MapsAPIConnector().also { instance = it }
            }
        }
    }

    fun fetchOptimalWaypointsForRoute(route: Route, travelMode: TravelMode): List<Coordinate> {
        val directionsResult = fetchOptimalDirection(route, travelMode)

        val waypoints = mutableListOf<Coordinate>()
        for (step in directionsResult.routes[0].legs[0].steps) {
            waypoints.add(Coordinate(step.endLocation.lat, step.endLocation.lng))
        }
        return waypoints
    }

    private fun fetchOptimalDirection(route: Route, travelMode: TravelMode): DirectionsResult {
        return DirectionsApi.newRequest(context)
            .origin(LatLng(route.origin.latitude, route.origin.longitude))
            .destination(LatLng(route.destination.latitude, route.destination.longitude))
            .mode(travelMode)
            .units(Unit.METRIC)
            .await()
    }

    override fun close() {
        context.shutdown()
    }
}