package cz.dd.routesvalidator

import junit.framework.TestCase.assertEquals
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import kotlin.math.roundToInt

private fun roundTo2DecimalPlaces(number: Double): Double {
    return (number * 100).roundToInt().toDouble() / 100
}

private val a = Coordinates(38.8976, -77.0366)
private val b = Coordinates(39.9496, -75.1503)
private val c1 = Coordinates(40.689361, -74.044705)
private val c2 = Coordinates(40.689426, -74.044542)
private val d = Coordinates(40.703996, -74.064266)

class ExtractRoutesFromWaypointsTest {

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
        val testValues = listOf(
            Pair(emptyList(), emptyList()),
            Pair(listOf(a), emptyList()),
            Pair(listOf(a, a), emptyList()),
            Pair(listOf(a, a, a), emptyList()),
            Pair(listOf(a, a, a, a), emptyList()),
            Pair(listOf(a, a, a, a, a), emptyList()),
            Pair(listOf(a, a, a, a, a, a), emptyList()),
            Pair(listOf(c1, c2), emptyList()),
            Pair(listOf(a, b), listOf(Route(a, b, emptyList()))),
            Pair(listOf(a, b, d), listOf(Route(a, d, listOf(b)))),
            Pair(listOf(a, b, b, b, d), listOf(Route(a, b, emptyList()), Route(b, d, emptyList()))),
            Pair(listOf(a, c1, c2, d, b, b, b, d), listOf(Route(a, b, listOf(c2, d)), Route(b, d, emptyList()))),
            Pair(
                listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, a, b),
                listOf(Route(a, b, listOf(c2, d)), Route(b, d, listOf(c1, a, c2)), Route(d, b, listOf(a)))
            ),
            Pair(
                listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, a, b, b, b),
                listOf(Route(a, b, listOf(c2, d)), Route(b, d, listOf(c1, a, c2)), Route(d, b, listOf(a)))
            ),
            Pair(
                listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, a, b, b, b, b),
                listOf(Route(a, b, listOf(c2, d)), Route(b, d, listOf(c1, a, c2)), Route(d, b, listOf(a)))
            ),
            Pair(
                listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, a, b, b, b, b, b),
                listOf(Route(a, b, listOf(c2, d)), Route(b, d, listOf(c1, a, c2)), Route(d, b, listOf(a)))
            ),
            Pair(
                listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, d, a, b, b, b, b, b),
                listOf(Route(a, b, listOf(c2, d)), Route(b, d, listOf(c1, a, c2)), Route(d, b, listOf(a)))
            ),
            Pair(
                listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, d, d, a, b, b, b, b, b),
                listOf(Route(a, b, listOf(c2, d)), Route(b, d, listOf(c1, a, c2)), Route(d, b, listOf(a)))
            ),
            Pair(
                listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, d, d, a, b, b, b, b, b, d),
                listOf(
                    Route(a, b, listOf(c2, d)),
                    Route(b, d, listOf(c1, a, c2)),
                    Route(d, b, listOf(a)),
                    Route(b, d, emptyList())
                )
            ),
            Pair(
                listOf(a, c1, c2, d, b, b, b, c1, a, c2, d, d, d, d, d, a, b, b, b, b, b, d, d, d, b),
                listOf(
                    Route(a, b, listOf(c2, d)),
                    Route(b, d, listOf(c1, a, c2)),
                    Route(d, b, listOf(a)),
                    Route(b, d, emptyList()),
                    Route(d, b, emptyList())
                )
            ),
            Pair(listOf(a, b, a, b, a, b), listOf(Route(a, b, listOf(b, a, b, a)))),
            Pair(listOf(c1, c2, d), listOf(Route(c1, d, emptyList()))),
            Pair(listOf(d, c1, c2), listOf(Route(d, c1, emptyList()))),
            Pair(listOf(a, c1, c2, c1, d), listOf(Route(a, c1, emptyList()), Route(c1, d, emptyList()))),
            Pair(listOf(a, c1, c1, c2, d), listOf(Route(a, c1, emptyList()), Route(c1, d, emptyList()))),
            Pair(listOf(a, b, c1, c1, c2, a, b, d), listOf(Route(a, c1, listOf(b)), Route(c1, d, listOf(a, b))))
        )

        for ((capturedWaypoints, extractedRoutes) in testValues) {
            assertThat(ExtractRoutesFromWaypoints(capturedWaypoints).extract()).isEqualToComparingFieldByFieldRecursively(
                extractedRoutes
            )
        }
    }
}
