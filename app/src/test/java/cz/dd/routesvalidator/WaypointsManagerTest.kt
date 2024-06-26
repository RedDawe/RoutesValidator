package cz.dd.routesvalidator

import cz.dd.routesvalidator.datamodel.Coordinate
import cz.dd.routesvalidator.datamodel.Route
import junit.framework.TestCase.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.LocalDateTime
import kotlin.math.roundToInt

private fun roundTo2DecimalPlaces(number: Double): Double {
    return (number * 100).roundToInt().toDouble() / 100
}

private const val IS_A_PLACE_OF_STAY_CONSECUTIVE_WAYPOINTS_THRESHOLD_TESTS = 3.0

private val a = Coordinate(38.8976, -77.0366, 0)
private val b = Coordinate(39.9496, -75.1503, 0)
private val c1 = Coordinate(40.689361, -74.044705, 0)
private val c2 = Coordinate(40.689426, -74.044542, 0)
private val d = Coordinate(40.703996, -74.064266, 0)

val waypointsRoutesTestValues = listOf(
    Pair(emptyList(), emptyList()),
    Pair(listOf(a), emptyList()),
    Pair(listOf(a, a), emptyList()),
    Pair(listOf(a, a, a), emptyList()),
    Pair(listOf(a, a, a, a), emptyList()),
    Pair(listOf(a, a, a, a, a), emptyList()),
    Pair(listOf(a, a, a, a, a, a), emptyList()),
    Pair(listOf(c1, c2), emptyList()),
    Pair(listOf(a, b), listOf(Route(a, b, emptyList(), LocalDateTime.now()))),
    Pair(listOf(a, b, d), listOf(Route(a, d, listOf(b), LocalDateTime.now()))),
    Pair(listOf(a, b, b, b, d), listOf(Route(a, b, emptyList(), LocalDateTime.now()), Route(b, d, emptyList(), LocalDateTime.now()))),
    Pair(listOf(a, c1, c2, d, b, b, b, d), listOf(Route(a, b, listOf(c1, d), LocalDateTime.now()), Route(b, d, emptyList(), LocalDateTime.now()))),
    Pair(
        listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, a, b),
        listOf(Route(a, b, listOf(c1, d), LocalDateTime.now()), Route(b, d, listOf(c1, a, c2), LocalDateTime.now()), Route(d, b, listOf(a), LocalDateTime.now()))
    ),
    Pair(
        listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, a, b, b, b),
        listOf(Route(a, b, listOf(c1, d), LocalDateTime.now()), Route(b, d, listOf(c1, a, c2), LocalDateTime.now()), Route(d, b, listOf(a), LocalDateTime.now()))
    ),
    Pair(
        listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, a, b, b, b, b),
        listOf(Route(a, b, listOf(c1, d), LocalDateTime.now()), Route(b, d, listOf(c1, a, c2), LocalDateTime.now()), Route(d, b, listOf(a), LocalDateTime.now()))
    ),
    Pair(
        listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, a, b, b, b, b, b),
        listOf(Route(a, b, listOf(c1, d), LocalDateTime.now()), Route(b, d, listOf(c1, a, c2), LocalDateTime.now()), Route(d, b, listOf(a), LocalDateTime.now()))
    ),
    Pair(
        listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, d, a, b, b, b, b, b),
        listOf(Route(a, b, listOf(c1, d), LocalDateTime.now()), Route(b, d, listOf(c1, a, c2), LocalDateTime.now()), Route(d, b, listOf(a), LocalDateTime.now()))
    ),
    Pair(
        listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, d, d, a, b, b, b, b, b),
        listOf(Route(a, b, listOf(c1, d), LocalDateTime.now()), Route(b, d, listOf(c1, a, c2), LocalDateTime.now()), Route(d, b, listOf(a), LocalDateTime.now()))
    ),
    Pair(
        listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, d, d, a, b, b, b, b, b, d),
        listOf(
            Route(a, b, listOf(c1, d), LocalDateTime.now()),
            Route(b, d, listOf(c1, a, c2), LocalDateTime.now()),
            Route(d, b, listOf(a), LocalDateTime.now()),
            Route(b, d, emptyList(), LocalDateTime.now())
        )
    ),
    Pair(
        listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, d, d, a, b, b, b, b, b, d, d, d, b),
        listOf(
            Route(a, b, listOf(c1, d), LocalDateTime.now()),
            Route(b, d, listOf(c1, a, c2), LocalDateTime.now()),
            Route(d, b, listOf(a), LocalDateTime.now()),
            Route(b, d, emptyList(), LocalDateTime.now()),
            Route(d, b, emptyList(), LocalDateTime.now())
        )
    ),
    Pair(listOf(a, b, a, b, a, b), listOf(Route(a, b, listOf(b, a, b, a), LocalDateTime.now()))),
    Pair(listOf(c1, c2, d), listOf(Route(c1, d, emptyList(), LocalDateTime.now()))),
    Pair(listOf(d, c1, c2), listOf(Route(d, c1, emptyList(), LocalDateTime.now()))),
    Pair(listOf(a, c1, c2, c1, d), listOf(Route(a, c1, emptyList(), LocalDateTime.now()), Route(c1, d, emptyList(), LocalDateTime.now()))),
    Pair(listOf(a, c1, c1, c2, d), listOf(Route(a, c1, emptyList(), LocalDateTime.now()), Route(c1, d, emptyList(), LocalDateTime.now()))),
    Pair(listOf(a, c2, c2, c1, d), listOf(Route(a, c2, emptyList(), LocalDateTime.now()), Route(c2, d, emptyList(), LocalDateTime.now()))),
    Pair(listOf(a, b, c1, c1, c2, a, b, d), listOf(Route(a, c1, listOf(b), LocalDateTime.now()), Route(c1, d, listOf(a, b), LocalDateTime.now())))
)

class WaypointsManagerTest {

    @Test
    fun calculateDistanceKilometersTest() {
        assertEquals(199.83, roundTo2DecimalPlaces(calculateDistanceKilometers(a, b)))
    }

    @Test
    fun calculateDistanceKilometersTestReversed() {
        assertEquals(199.83, roundTo2DecimalPlaces(calculateDistanceKilometers(b, a)))
    }

    @Test
    fun extract() {
        val waypointsManager = WaypointsManager.getNewInstanceForTests(
            IS_A_PLACE_OF_STAY_CONSECUTIVE_WAYPOINTS_THRESHOLD_TESTS
        )
        for ((capturedWaypoints, expectedRoutes) in waypointsRoutesTestValues) {
            waypointsManager.reset()

            val actualRoutes = mutableListOf<Route>()

            for (capturedWaypoint in capturedWaypoints) {
                val potentialRoute = waypointsManager.processWaypoint(capturedWaypoint)
                if (potentialRoute != null) actualRoutes.add(potentialRoute)
            }
            val potentialRoute = waypointsManager.finishAddingWaypoints()
            if (potentialRoute != null) actualRoutes.add(potentialRoute)

            assertThat(actualRoutes).hasSameSizeAs(expectedRoutes)
            actualRoutes.zip(expectedRoutes).forEach { (actualRoute, expectedRoute) ->
                assertThat(actualRoute).usingComparator(FinishTimeIndifferentRouteComparator()).isEqualTo(expectedRoute)
            }
        }
    }
}