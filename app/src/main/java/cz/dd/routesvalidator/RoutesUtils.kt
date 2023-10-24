package cz.dd.routesvalidator

import android.content.Context
import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route
import java.time.LocalDateTime
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val SUSPECTED_ROUTES_FILE_NAME = "suspectedRoutes.csv"

fun isRouteShortest(route: Route, optimalWaypoints: List<Coordinate>): Boolean {
    for (optimalWaypoint in optimalWaypoints) {
        var foundMatching = false
        for (actuallWaypoint in route.waypoints) {
            if (calculateDistanceKilometers(
                    actuallWaypoint,
                    optimalWaypoint
                ) * 1000 < SAME_WAYPOINT_THRESHOLD_DISTANCE_METERS
            ) {
                foundMatching = true
                break
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

    return 2 * EARTH_RADIUS_KILOMETERS * asin(sqrt(haversine))
}

fun appendSuspectedRoute(route: Route, context: Context) { // TODO: notify user
    val existingSuspectedRoutes = loadSuspectedRoutes(context)

    context.openFileOutput(SUSPECTED_ROUTES_FILE_NAME, Context.MODE_PRIVATE).use {
        for (existingSuspectedRoute in existingSuspectedRoutes) {
            it.write(existingSuspectedRoute.csvLine().toByteArray())
        }
        it.write(route.csvLine().toByteArray())
    }
}

fun resetFile(context: Context) {
    context.openFileOutput(SUSPECTED_ROUTES_FILE_NAME, Context.MODE_PRIVATE).use {
        it.write(System.lineSeparator().toByteArray())
    }
}

fun loadSuspectedRoutes(context: Context): List<Route> {
    if (!context.fileList().contains(SUSPECTED_ROUTES_FILE_NAME)) return emptyList()

    val routes = mutableListOf<Route>()
    for (line in context.openFileInput(SUSPECTED_ROUTES_FILE_NAME).bufferedReader().readLines()) {
        if (line.isBlank()) return emptyList()
        val valueList = line.trim().split(",")
        val waypoints = mutableListOf<Coordinate>()
        for (i in 5 until valueList.size) {
            waypoints.add(Coordinate(valueList[i].toDouble(), valueList[i + 1].toDouble()))
        }
        routes.add(
            Route(
                Coordinate(valueList[0].toDouble(), valueList[1].toDouble()),
                Coordinate(valueList[2].toDouble(), valueList[3].toDouble()),
                waypoints,
                LocalDateTime.parse(valueList[4])
            )
        )
    }

    return routes
}

fun deleteMatchingRoutes(route: Route, context: Context) {
    val existingSuspectedRoutes = loadSuspectedRoutes(context)

    context.openFileOutput(SUSPECTED_ROUTES_FILE_NAME, Context.MODE_PRIVATE).use {
        for (existingSuspectedRoute in existingSuspectedRoutes) {
            if (existingSuspectedRoute != route) {
                it.write(existingSuspectedRoute.csvLine().toByteArray())
            }
        }
    }
}
