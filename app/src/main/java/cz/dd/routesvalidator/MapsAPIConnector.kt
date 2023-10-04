package cz.dd.routesvalidator

import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.LatLng
import com.google.maps.model.TravelMode
import com.google.maps.model.Unit
import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route

class MapsAPIConnector: AutoCloseable {
    private val context: GeoApiContext = GeoApiContext.Builder()
        .apiKey(BuildConfig.GOOGLE_MAPS_API_KEY)
        .build()

    fun fetchOptimalWaypointsForRoute(route: Route): List<Coordinate> {
        val directionsResult = fetchOptimalDirection(route)

        val waypoints = mutableListOf<Coordinate>()
        for (step in directionsResult.routes[0].legs[0].steps) {
            waypoints.add(Coordinate(step.endLocation.lat, step.endLocation.lng))
        }
        return waypoints
    }

    private fun fetchOptimalDirection(route: Route): DirectionsResult {
        val directionsResult = DirectionsApi.newRequest(context)
            .origin(LatLng(route.origin.latitude, route.origin.longitude))
            .destination(LatLng(route.destination.latitude, route.destination.longitude))
            .mode(TravelMode.TRANSIT)
            .units(Unit.METRIC)
            .await()

        return directionsResult
    }

    override fun close() {
        context.shutdown()
    }
}