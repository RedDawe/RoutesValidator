package cz.dd.routesvalidator.datamodel

class Coordinate (val latitude: Double, val longitude: Double) {
    override fun toString(): String {
        return StringBuilder().append(latitude).append(",").append(longitude).toString()
    }
}