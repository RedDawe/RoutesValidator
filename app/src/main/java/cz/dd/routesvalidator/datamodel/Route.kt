package cz.dd.routesvalidator.datamodel

import java.time.LocalDateTime

class Route(
    val origin: Coordinate,
    val destination: Coordinate,
    val waypoints: List<Coordinate>,
    val finishTime: LocalDateTime
) {
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
}