package cz.dd.routesvalidator

import java.time.Duration

const val EARTH_RADIUS_KILOMETERS = 6371
const val SAME_WAYPOINT_THRESHOLD_DISTANCE_METERS = 50
const val SAME_PLACE_OF_STAY_THRESHOLD_DISTANCE_METERS = 100
private const val MAXIMUM_EXPECTED_SPEED_KMH = 90
private const val MINIMUM_EXPECTED_SPEED_KMH = 2

private const val SECONDS_NEEDED_TO_ESCAPE_WAYPOINT_CIRCLE =
    SAME_WAYPOINT_THRESHOLD_DISTANCE_METERS * 2 / (MAXIMUM_EXPECTED_SPEED_KMH / 3.6)
val WAYPOINT_LOCATION_CAPTURE_DELAY: Duration =
    Duration.ofMillis(SECONDS_NEEDED_TO_ESCAPE_WAYPOINT_CIRCLE.toLong() * 1000)

val IS_A_PLACE_OF_STAY_CONSECUTIVE_WAYPOINTS_THRESHOLD =
    SAME_PLACE_OF_STAY_THRESHOLD_DISTANCE_METERS / (MINIMUM_EXPECTED_SPEED_KMH / 3.6) / WAYPOINT_LOCATION_CAPTURE_DELAY.seconds
