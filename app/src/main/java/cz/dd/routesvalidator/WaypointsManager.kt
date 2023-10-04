package cz.dd.routesvalidator

import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val SAME_PLACE_THRESHOLD_DISTANCE_METERS = 50
private const val IS_A_PLACE_OF_STAY_CONSECUTIVE_WAYPOINTS_THRESHOLD = 3

class WaypointsManager {
    private var currentWaypoint: Coordinate? = null
    private var currentWaypointOccurrences = 0
    private var isFirstWaypoint = true
    private var currentWaypoints: MutableList<Coordinate> = mutableListOf()
    private var lastPlaceOfStay: Coordinate? = null
    val routes: MutableList<Route> = mutableListOf()

    fun addWaypoint(waypoint: Coordinate) {
        val currentWaypointImmutableCopy = currentWaypoint
        val lastPlaceOfStayImmutableCopy = lastPlaceOfStay
        if (currentWaypointImmutableCopy == null) {
            currentWaypointOccurrences = 1
            assert(isFirstWaypoint)

        } else if (areTheSamePlace(waypoint, currentWaypointImmutableCopy)) {
            currentWaypointOccurrences++

        } else if (!isFirstWaypoint && currentWaypointOccurrences < IS_A_PLACE_OF_STAY_CONSECUTIVE_WAYPOINTS_THRESHOLD) {
            currentWaypoints.add(currentWaypointImmutableCopy)
            currentWaypointOccurrences = 1
            isFirstWaypoint = false

        } else if (lastPlaceOfStayImmutableCopy == null) {
            lastPlaceOfStay = currentWaypointImmutableCopy
            currentWaypointOccurrences = 1
            isFirstWaypoint = false
            assert(currentWaypoints.isEmpty())

        } else {
            routes.add(Route(lastPlaceOfStayImmutableCopy, currentWaypointImmutableCopy, currentWaypoints))
            currentWaypoints = mutableListOf()
            lastPlaceOfStay = currentWaypointImmutableCopy
            currentWaypointOccurrences = 1
            isFirstWaypoint = false

        }
        currentWaypoint = waypoint
    }

    fun finishAddingWaypoints() {
        val currentWaypointImmutableCopy = currentWaypoint
        val lastPlaceOfStayImmutableCopy = lastPlaceOfStay
        if (currentWaypointImmutableCopy == null || lastPlaceOfStayImmutableCopy == null) return
        routes.add(Route(lastPlaceOfStayImmutableCopy, currentWaypointImmutableCopy, currentWaypoints))

        currentWaypoint = null
        currentWaypointOccurrences = 0
        isFirstWaypoint = true
        currentWaypoints = mutableListOf()
        lastPlaceOfStay = null
    }

    private fun areTheSamePlace(placeA: Coordinate, placeB: Coordinate): Boolean {
        return calculateDistanceKilometers(placeA, placeB) * 1000 < SAME_PLACE_THRESHOLD_DISTANCE_METERS
    }
}