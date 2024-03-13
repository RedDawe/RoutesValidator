package cz.dd.routesvalidator.datamodel

import java.time.LocalDateTime

class Route(
    val origin: Coordinate,
    val destination: Coordinate,
    val waypoints: List<Coordinate>,
    val finishTime: LocalDateTime
)  {

    fun csvLine(): String {
        val stringBuilder = StringBuilder().append(origin)
            .append(",")
            .append(destination)
            .append(",")
            .append(finishTime)
        for (waypoint in waypoints) {
            stringBuilder.append(",")
            stringBuilder.append(waypoint)
        }
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

    override fun toString(): String {
        return "Route(origin=$origin, destination=$destination, waypoints=$waypoints, finishTime=$finishTime)"
    }
}