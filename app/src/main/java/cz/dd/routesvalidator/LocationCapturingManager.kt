package cz.dd.routesvalidator

class LocationCapturingManager {
    companion object {

        @Volatile
        private var instance: LocationCapturingManager? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: LocationCapturingManager().also { instance = it }
            }
    }

    @Volatile
    var keepCapturing = false
}