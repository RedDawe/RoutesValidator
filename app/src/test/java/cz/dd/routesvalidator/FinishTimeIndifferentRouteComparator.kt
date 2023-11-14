package cz.dd.routesvalidator

import cz.dd.routesvalidator.datamodel.Route

class FinishTimeIndifferentRouteComparator : Comparator<Route> {
    override fun compare(o1: Route?, o2: Route?): Int {
        if (o1 == null && o2 == null) return 0
        if (o1 == null || o2 == null) return 1
        return if (o1.origin == o2.origin && o1.destination == o2.destination && o1.waypoints == o2.waypoints) 0 else 1
    }
}