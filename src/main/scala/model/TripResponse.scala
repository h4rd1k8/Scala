package model

case class TripResponse(trip: Trip, isSuccessful: Boolean, statusCode: Int, message: String)
