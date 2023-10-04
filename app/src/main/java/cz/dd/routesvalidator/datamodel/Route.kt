package cz.dd.routesvalidator.datamodel

import cz.dd.routesvalidator.datamodel.Coordinate

class Route (val origin: Coordinate, val destination: Coordinate, val waypoints: List<Coordinate>)