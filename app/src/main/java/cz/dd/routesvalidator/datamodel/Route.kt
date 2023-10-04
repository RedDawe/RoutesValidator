package cz.dd.routesvalidator.datamodel

import cz.dd.routesvalidator.datamodel.Coordinate

class Route (val origin: Coordinate, val destination: Coordinate, val waypoints: List<Coordinate>) {
    fun csvString(): String {
        val stringBuilder = StringBuilder().append(origin)
            .append(",")
            .append(destination)
        for (waypoint in waypoints) {
            stringBuilder.append(",")
            stringBuilder.append(waypoint)
        }
        stringBuilder.append(System.lineSeparator())
        return stringBuilder.toString()
    }
}