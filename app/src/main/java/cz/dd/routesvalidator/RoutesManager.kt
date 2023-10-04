package cz.dd.routesvalidator

import cz.dd.routesvalidator.datamodel.Route

class RoutesManager(private val mapsAPIConnector: MapsAPIConnector) {

    fun capturedNewRoute(route: Route) {
        if (isRouteShortest(route, mapsAPIConnector.fetchOptimalWaypointsForRoute(route))) {
            appendSuspectedRoute(route)
        }
    }
}