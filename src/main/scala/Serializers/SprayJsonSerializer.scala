package Serializers


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import model.{SuccessfulResponse, ErrorResponse, Traveler, Trip}

trait SprayJsonSerializer extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val successfulResponse: RootJsonFormat[SuccessfulResponse] = jsonFormat2(SuccessfulResponse)
  implicit val errorResponse: RootJsonFormat[ErrorResponse] = jsonFormat2(ErrorResponse)

  implicit val travelerFormat: RootJsonFormat[Traveler] = jsonFormat3(Traveler)
  implicit val tripFormat: RootJsonFormat[Trip] = jsonFormat5(Trip)

}
