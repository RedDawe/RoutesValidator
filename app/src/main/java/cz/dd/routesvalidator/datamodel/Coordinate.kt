package cz.dd.routesvalidator.datamodel

class Coordinate (val latitude: Double, val longitude: Double) {
    override fun toString(): String {
        return StringBuilder().append(latitude).append(",").append(longitude).toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Coordinate

        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false

        return true
    }

    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        return result
    }


}