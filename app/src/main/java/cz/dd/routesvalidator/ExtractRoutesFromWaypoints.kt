package cz.dd.routesvalidator

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val SAME_PLACE_THRESHOLD_DISTANCE_METERS = 50
private const val IS_A_PLACE_CONSECUTIVE_POINTS_THRESHOLD = 3
private const val EARTH_RADIUS = 6371

fun calculateDistanceKilometers(placeA: Coordinates, placeB: Coordinates): Double {
    val changeInLatitude = Math.toRadians(placeB.latitude - placeA.latitude)
    val changeInLongitude = Math.toRadians(placeB.longitude - placeA.longitude)

    val haversine = sin(changeInLatitude / 2).pow(2.0) +
            sin(changeInLongitude / 2).pow(2.0) *
            cos(Math.toRadians(placeA.latitude)) *
            cos(Math.toRadians(placeB.latitude))

    return 2 * EARTH_RADIUS * asin(sqrt(haversine))
}

class ExtractRoutesFromWaypoints (private val capturedWaypoints: List<Coordinates>) {

    fun extract(): List<Route> {
        if (capturedWaypoints.size < 2) return emptyList()

        val extractedRoutes = mutableListOf<Route>()
        var lastPlace = capturedWaypoints[0]
        var previousWaypoint = capturedWaypoints[0]
        var samePlaceCounter = 0
        var currentRouteWaypoints = mutableListOf<Coordinates>()
        for ((i, waypoint) in capturedWaypoints.withIndex()) {
            if (i == 0) continue
            if (areTheSamePlace(waypoint, previousWaypoint)) {
                samePlaceCounter++
                previousWaypoint = waypoint
                continue
            }
            if (samePlaceCounter < IS_A_PLACE_CONSECUTIVE_POINTS_THRESHOLD) {
                currentRouteWaypoints.add(previousWaypoint)
                samePlaceCounter = 0
                previousWaypoint = waypoint
                continue
            }
            if (i - 1 != samePlaceCounter) {
                extractedRoutes.add(Route(lastPlace, previousWaypoint, currentRouteWaypoints))
            }
            samePlaceCounter = 0
            previousWaypoint = waypoint
            lastPlace = previousWaypoint
            currentRouteWaypoints = mutableListOf()
        }
        if (currentRouteWaypoints.isNotEmpty()) {
            currentRouteWaypoints.removeLast()
        }
        if (!areTheSamePlace(lastPlace, previousWaypoint)) {
            extractedRoutes.add(Route(lastPlace, previousWaypoint, currentRouteWaypoints))
        }

        return extractedRoutes
    }

    private fun areTheSamePlace(placeA: Coordinates, placeB: Coordinates): Boolean {
        return calculateDistanceKilometers(placeA, placeB) * 1000 < SAME_PLACE_THRESHOLD_DISTANCE_METERS
    }
}