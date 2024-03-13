package cz.dd.routesvalidator

import android.content.Context
import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

const val SUSPECTED_ROUTES_FILE_NAME = "suspectedRoutes.csv"
const val TO_BE_PROCESSES_ROUTES_FILE_NAME = "toBeProcessedRoutes.csv"

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

fun appendRoute(fileName: String, route: Route, context: Context) {
    val existingSuspectedRoutes = loadRoutes(fileName, context)

    context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
        for (existingSuspectedRoute in existingSuspectedRoutes) {
            it.write(existingSuspectedRoute.csvLine().toByteArray())
        }
        it.write(route.csvLine().toByteArray())
    }
}

fun resetFile(fileName: String, context: Context) {
    context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
        it.write(System.lineSeparator().toByteArray())
    }
}

fun loadRoutes(fileName: String, context: Context): List<Route> {
    if (!context.fileList().contains(fileName)) return emptyList()

    val routes = mutableListOf<Route>()
    for (line in context.openFileInput(fileName).bufferedReader().readLines()) {
        if (line.isBlank()) return emptyList()
        val valueList = line.trim().split(",")
        val waypoints = mutableListOf<Coordinate>()

        for (i in 7 until valueList.size - 1 step 3) {
            waypoints.add(Coordinate(valueList[i].toDouble(), valueList[i + 1].toDouble(), valueList[i + 2].toLong()))
        }

        routes.add(
            Route(
                Coordinate(valueList[0].toDouble(), valueList[1].toDouble(), valueList[2].toLong()),
                Coordinate(valueList[3].toDouble(), valueList[4].toDouble(), valueList[5].toLong()),
                waypoints,
                LocalDateTime.parse(valueList[6])
            )
        )
    }

    return routes
}

fun deleteMatchingRoutes(fileName: String, route: Route, context: Context) {
    val existingSuspectedRoutes = loadRoutes(fileName, context)

    context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
        for (existingSuspectedRoute in existingSuspectedRoutes) {
            if (existingSuspectedRoute != route) {
                it.write(existingSuspectedRoute.csvLine().toByteArray())
            }
        }
    }
}

val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM")


