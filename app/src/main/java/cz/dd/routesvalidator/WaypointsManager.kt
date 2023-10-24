package cz.dd.routesvalidator

import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route
import java.time.LocalDateTime

class WaypointsManager private constructor() {
    // TODO: all should be synced, ideally on methods
    private var currentWaypoint: Coordinate? = null
    private var currentWaypointOccurrences = 0
    private var isFirstWaypoint = true
    private var currentWaypoints: MutableList<Coordinate> = mutableListOf()
    private var lastPlaceOfStay: Coordinate? = null

    companion object {

        @Volatile
        private var instance: WaypointsManager? = null

        fun getInstance(): WaypointsManager {
            return instance ?: synchronized(this) {
                instance ?: WaypointsManager().also { instance = it }
            }
        }

        fun getNewInstanceForTests(): WaypointsManager {
            return WaypointsManager()
        }
    }

    fun processWaypoint(waypoint: Coordinate): Route? {
        var result: Route? = null

        val currentWaypointImmutableCopy = currentWaypoint
        val lastPlaceOfStayImmutableCopy = lastPlaceOfStay
        if (currentWaypointImmutableCopy == null) {
            currentWaypointOccurrences = 1
            assert(isFirstWaypoint)
            currentWaypoint = waypoint

        } else if (areTheSamePlace(waypoint, currentWaypointImmutableCopy)) {
            currentWaypointOccurrences++

        } else if (!isFirstWaypoint && currentWaypointOccurrences < IS_A_PLACE_OF_STAY_CONSECUTIVE_WAYPOINTS_THRESHOLD) {
            currentWaypoints.add(currentWaypointImmutableCopy)
            currentWaypointOccurrences = 1
            isFirstWaypoint = false
            currentWaypoint = waypoint

        } else if (lastPlaceOfStayImmutableCopy == null) {
            lastPlaceOfStay = currentWaypointImmutableCopy
            currentWaypointOccurrences = 1
            isFirstWaypoint = false
            assert(currentWaypoints.isEmpty())
            currentWaypoint = waypoint

        } else {
            result =
                Route(lastPlaceOfStayImmutableCopy, currentWaypointImmutableCopy, currentWaypoints, LocalDateTime.now())
            currentWaypoints = mutableListOf()
            lastPlaceOfStay = currentWaypointImmutableCopy
            currentWaypointOccurrences = 1
            isFirstWaypoint = false
            currentWaypoint = waypoint

        }
        return result
    }

    fun finishAddingWaypoints(): Route? {
        val currentWaypointImmutableCopy = currentWaypoint
        val lastPlaceOfStayImmutableCopy = lastPlaceOfStay
        if (currentWaypointImmutableCopy == null || lastPlaceOfStayImmutableCopy == null) return null
        val result =
            Route(lastPlaceOfStayImmutableCopy, currentWaypointImmutableCopy, currentWaypoints, LocalDateTime.now())

        reset()

        return result
    }

    private fun areTheSamePlace(placeA: Coordinate, placeB: Coordinate): Boolean {
        return calculateDistanceKilometers(placeA, placeB) * 1000 < SAME_PLACE_OF_STAY_THRESHOLD_DISTANCE_METERS
    }

    fun reset() {
        currentWaypoint = null
        currentWaypointOccurrences = 0
        isFirstWaypoint = true
        currentWaypoints = mutableListOf()
        lastPlaceOfStay = null
    }
}