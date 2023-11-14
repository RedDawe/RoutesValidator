package cz.dd.routesvalidator

import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route
import java.time.LocalDateTime

// TODO: ideally this class would be saved persistently and loaded for every change
class WaypointsManager private constructor(private val isAPlaceOfStayConsecutiveWaypointsThreshold: Double) {
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
                instance ?: WaypointsManager(IS_A_PLACE_OF_STAY_CONSECUTIVE_WAYPOINTS_THRESHOLD).also { instance = it }
            }
        }

        fun getNewInstanceForTests(isAPlaceOfStayConsecutiveWaypointsThreshold: Double): WaypointsManager {
            return WaypointsManager(isAPlaceOfStayConsecutiveWaypointsThreshold)
        }
    }

    @Synchronized fun processWaypoint(waypoint: Coordinate): Route? {
        var result: Route? = null

        val currentWaypointImmutableCopy = currentWaypoint
        val lastPlaceOfStayImmutableCopy = lastPlaceOfStay
        if (currentWaypointImmutableCopy == null) {
            currentWaypointOccurrences = 1
            assert(isFirstWaypoint)
            currentWaypoint = waypoint

        } else if (areTheSamePlace(waypoint, currentWaypointImmutableCopy)) {
            currentWaypointOccurrences++

        } else if (!isFirstWaypoint && currentWaypointOccurrences < isAPlaceOfStayConsecutiveWaypointsThreshold) {
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

    @Synchronized fun finishAddingWaypoints(): Route? {
        val currentWaypointImmutableCopy = currentWaypoint
        val lastPlaceOfStayImmutableCopy = lastPlaceOfStay
        if (currentWaypointImmutableCopy == null || lastPlaceOfStayImmutableCopy == null) return null
        val result =
            Route(lastPlaceOfStayImmutableCopy, currentWaypointImmutableCopy, currentWaypoints, LocalDateTime.now())

        reset()

        return result
    }

    @Synchronized fun reset() {
        currentWaypoint = null
        currentWaypointOccurrences = 0
        isFirstWaypoint = true
        currentWaypoints = mutableListOf()
        lastPlaceOfStay = null
    }

    private fun areTheSamePlace(placeA: Coordinate, placeB: Coordinate): Boolean {
        return calculateDistanceKilometers(placeA, placeB) * 1000 < SAME_PLACE_OF_STAY_THRESHOLD_DISTANCE_METERS
    }
}