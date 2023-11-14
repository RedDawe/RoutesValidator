package cz.dd.routesvalidator

import com.google.maps.model.TravelMode

class LocationCapturingManager {
    companion object {

        @Volatile
        private var instance: LocationCapturingManager? = null

        fun getInstance(): LocationCapturingManager {
            return instance ?: synchronized(this) {
                instance ?: LocationCapturingManager().also { instance = it }
            }
        }
    }

    @Volatile
    var keepCapturing = false
    @Volatile
    var mainActivity: MainActivity? = null
    @Volatile
    var travelMode = TravelMode.WALKING;
}