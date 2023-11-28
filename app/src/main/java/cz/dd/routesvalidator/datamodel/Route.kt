package cz.dd.routesvalidator.datamodel

import java.time.LocalDateTime

class Route(
    val origin: Coordinate,
    val destination: Coordinate,
    val waypoints: List<Coordinate>,
    val finishTime: LocalDateTime
)  {
    constructor(origin: Coordinate, destination: Coordinate, waypoints: List<Coordinate>) : // constructor for tests
            this(origin, destination, waypoints, LocalDateTime.now())

    fun csvLine(): String {
        val stringBuilder = StringBuilder().append(origin)
            .append(",")
            .append(destination)
            .append(",")
            .append(finishTime)
//        for (waypoint in waypoints) { // TODO: prettier solution
//            stringBuilder.append(",")
//            stringBuilder.append(waypoint)
//        }
        stringBuilder.append(System.lineSeparator())
        return stringBuilder.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Route

        if (origin != other.origin) return false
        if (destination != other.destination) return false
        if (waypoints != other.waypoints) return false
        if (finishTime != other.finishTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = origin.hashCode()
        result = 31 * result + destination.hashCode()
        result = 31 * result + waypoints.hashCode()
        result = 31 * result + finishTime.hashCode()
        return result
    }
}