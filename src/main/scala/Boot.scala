
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{delete, put, _}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout

import Serializers.SprayJsonSerializer
import actor.TripManager
import model.{SuccessfulResponse, ErrorResponse, Trip, TelegramMessage}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor


object Boot extends App with SprayJsonSerializer {

  implicit val system: ActorSystem = ActorSystem("trip-service")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = Timeout(10.seconds)

  val tripManager = system.actorOf(TripManager.props(), "trip-manager")

  //val message: TelegramMessage = TelegramMessage(-352088280, "Hello world")


  val route =
    pathPrefix("my-trip-manager") {
      path("trip" / Segment) { tripId =>
        concat(
          get {
            complete {
              (tripManager ? TripManager.ReadTrip(tripId)).mapTo[Either[ErrorResponse, Trip]]
            }
          },
          delete {
            complete {
              (tripManager ? TripManager.DeleteTrip(tripId)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
            }
          }
        )
      } ~
        path("trip") {
          concat(
            post {
              entity(as[Trip]) { trip =>
                complete {
                  (tripManager ? TripManager.CreateTrip(trip)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
                }
              }
            },
            put {
              entity(as[Trip]) { trip =>
                complete {
                  (tripManager ? TripManager.UpdateTrip(trip)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
                }
              }
            }
          )
        }
    }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)
}