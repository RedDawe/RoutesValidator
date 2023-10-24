package cz.dd.routesvalidator

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
    var mainActivity: MainActivity? = null
}