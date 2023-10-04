package cz.dd.routesvalidator

import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route
import java.io.File
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val SUSPECTED_ROUTES_FILE_NAME = "suspectedRoutes.csv"
private const val SAME_WAYPOINT_THRESHOLD_DISTANCE_METERS = 20
private const val EARTH_RADIUS = 6371

fun isRouteShortest(route: Route, optimalWaypoints: List<Coordinate>): Boolean {
    for (optimalWaypoint in optimalWaypoints) {
        var foundMatching = false
        for (actuallWaypoint in route.waypoints) {
            if (calculateDistanceKilometers(actuallWaypoint, optimalWaypoint) * 1000 < SAME_WAYPOINT_THRESHOLD_DISTANCE_METERS) {
                foundMatching = true
            }
        }
        if (!foundMatching) return false
    }
    return true
}

fun calculateDistanceKilometers(placeA: Coordinate, placeB: Coordinate): Double {
    val changeInLatitude = Math.toRadians(placeB.latitude - placeA.latitude)
    val changeInLongitude = Math.toRadians(placeB.longitude - placeA.longitude)

    val haversine = sin(changeInLatitude / 2).pow(2.0) +
            sin(changeInLongitude / 2).pow(2.0) *
            cos(Math.toRadians(placeA.latitude)) *
            cos(Math.toRadians(placeB.latitude))

    return 2 * EARTH_RADIUS * asin(sqrt(haversine))
}

fun appendSuspectedRoute(route: Route) {
    val suspectedRoutes = File(SUSPECTED_ROUTES_FILE_NAME)
    suspectedRoutes.appendText(route.csvString())
}

fun loadSuspectedRoutes(): List<Route> {
    val routes = mutableListOf<Route>()

    val file = File(SUSPECTED_ROUTES_FILE_NAME)
    if (!file.exists()) return emptyList()
    for (line in file.readLines()) {
        val valueList = line.split(",")
        val waypoints = mutableListOf<Coordinate>()
        for (i in 4 until valueList.size) {
            waypoints.add(Coordinate(valueList[i].toDouble(), valueList[i + 1].toDouble()))
        }
        routes.add(Route(Coordinate(valueList[0].toDouble(), valueList[1].toDouble()), Coordinate(valueList[2].toDouble(), valueList[3].toDouble()), waypoints))
    }

    return routes
}