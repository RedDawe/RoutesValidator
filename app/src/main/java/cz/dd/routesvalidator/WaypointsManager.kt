package cz.dd.routesvalidator

import android.content.Context
import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

private const val WAYPOINTS_MANAGER_FILE_NAME = "waypointsManager.json"

private fun areTheSamePlace(placeA: Coordinate, placeB: Coordinate): Boolean {
    return calculateDistanceKilometers(placeA, placeB) * 1000 < SAME_PLACE_OF_STAY_THRESHOLD_DISTANCE_METERS
}

@Serializable
class WaypointsManager private constructor(
    private val isAPlaceOfStayConsecutiveWaypointsThreshold: Double,
    private var currentWaypoint: Coordinate?,
    private var currentWaypointOccurrences: Int,
    private var isFirstWaypoint: Boolean,
    private var currentWaypoints: MutableList<Coordinate>,
    private var lastPlaceOfStay: Coordinate?
) {
    private constructor(isAPlaceOfStayConsecutiveWaypointsThreshold: Double) :
            this(
                isAPlaceOfStayConsecutiveWaypointsThreshold,
                null,
                0,
                true,
                mutableListOf(),
                null
            )

    companion object {

        @Volatile
        private var instance: WaypointsManager? = null

        fun getInstance(context: Context): WaypointsManager {
            return instance ?: synchronized(this) {
                instance ?: restore(context).also { instance = it }
            }
        }

        fun getNewInstanceForTests(isAPlaceOfStayConsecutiveWaypointsThreshold: Double): WaypointsManager {
            return WaypointsManager(isAPlaceOfStayConsecutiveWaypointsThreshold)
        }

        private fun flushChanges(context: Context) {
            if (instance == null) return
            val json = Json.encodeToString(instance!!)

            context.openFileOutput(WAYPOINTS_MANAGER_FILE_NAME, Context.MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }
        }

        private fun restore(context: Context): WaypointsManager {
            if (!context.fileList().contains(WAYPOINTS_MANAGER_FILE_NAME)) return WaypointsManager(
                IS_A_PLACE_OF_STAY_CONSECUTIVE_WAYPOINTS_THRESHOLD
            )

            val json = context.openFileInput(WAYPOINTS_MANAGER_FILE_NAME).bufferedReader().use { it.readText() }
            return Json.decodeFromString(json)
        }
    }

    @Synchronized
    fun processWaypoint(waypoint: Coordinate, context: Context): Route? {
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

        flushChanges(context)
        return result
    }

    @Synchronized
    fun finishAddingWaypoints(context: Context): Route? {
        val currentWaypointImmutableCopy = currentWaypoint
        val lastPlaceOfStayImmutableCopy = lastPlaceOfStay
        if (currentWaypointImmutableCopy == null || lastPlaceOfStayImmutableCopy == null) return null
        val result =
            Route(lastPlaceOfStayImmutableCopy, currentWaypointImmutableCopy, currentWaypoints, LocalDateTime.now())

        reset(context)

        flushChanges(context)
        return result
    }

    @Synchronized
    fun reset(context: Context) {
        currentWaypoint = null
        currentWaypointOccurrences = 0
        isFirstWaypoint = true
        currentWaypoints = mutableListOf()
        lastPlaceOfStay = null

        flushChanges(context)
    }
}