package cz.dd.routesvalidator

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.maps.model.TravelMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

fun indexToTravelMode(index: Int): TravelMode {
    return when(index) {
        0 -> TravelMode.WALKING
        1 -> TravelMode.TRANSIT
        2 -> TravelMode.BICYCLING
        3 -> TravelMode.DRIVING
        else -> throw Exception("Unknown travel mode")
    }
}

fun travelModeToIndex(travelMode: TravelMode): Int {
    return when(travelMode) {
        TravelMode.WALKING -> 0
        TravelMode.TRANSIT -> 1
        TravelMode.BICYCLING -> 2
        TravelMode.DRIVING -> 3
        else -> throw Exception("Unknown travel mode")
    }
}

class LocationCapturingManager {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "location_capturing_manager_file")
        private val keepCapturingDataStoreKey = booleanPreferencesKey("keep_capturing")
        private val travelModeDataStoreKey = intPreferencesKey("travel_mode")

        @Volatile
        private var instance: LocationCapturingManager? = null

        fun getInstance(): LocationCapturingManager {
            return instance ?: synchronized(this) {
                instance ?: LocationCapturingManager().also { instance = it }
            }
        }

        fun restore(context: Context) {
            val instance = getInstance()

            val preferences = runBlocking { context.dataStore.data.first() }

            instance.keepCapturing = preferences[keepCapturingDataStoreKey] ?: false

            val travelModeIndex = preferences[travelModeDataStoreKey]
            if (travelModeIndex != null) {
                instance.travelMode = indexToTravelMode(travelModeIndex)
            }
        }

        fun flushChanges(context: Context) {
            val instance = getInstance()
            val dataStore = context.dataStore
            runBlocking {
                dataStore.edit {
                    it[keepCapturingDataStoreKey] = instance.keepCapturing
                    it[travelModeDataStoreKey] = travelModeToIndex(instance.travelMode)
                }
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