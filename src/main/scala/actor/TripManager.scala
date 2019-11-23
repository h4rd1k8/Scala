package actor

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.stream.{ActorMaterializer, Materializer}
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import model.{ErrorResponse, SuccessfulResponse, TelegramManager, TelegramMessage, Trip, TripResponse}
import Serializers.{ElasticSerializer, SprayJsonSerializer}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object TripManager {

  case class CreateTrip(recipe: Trip)

  case class ReadTrip(id: String)

  case class UpdateTrip(recipe: Trip)

  case class DeleteTrip(id: String)

  def props() = Props(new TripManager)

}

class TripManager extends Actor with ActorLogging with ElasticSerializer {
  import TripManager._

  implicit val system: ActorSystem = ActorSystem("telegram-service")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val client = HttpClient(ElasticsearchClientUri("localhost", 9200))

  val telegramManager = system.actorOf(TelegramManager.props(), "telegram-manager")


  val chat_id = -352088280

  override def receive: Receive = {
    case CreateTrip(trip:Trip)  =>
      val cmd = client.execute(indexInto("trips" / "_doc").id(trip.id).doc(trip))
      val replyTo = sender()

      cmd.onComplete {
        case Success(_) =>
          val tripResponse = TripResponse(null, isSuccessful = true, 201, s"Trip with ID = ${trip.id} successfully created. Trip = [$trip]")
          handleResponse(replyTo, tripResponse)
          var msg = TelegramMessage(chat_id, s"Trip with ID = ${trip.id} successfully created. Trip = [$trip]")
          telegramManager ! TelegramManager.SendWhenCreated(trip, 201)

        case Failure(_) =>
          val tripResponse = TripResponse(null, isSuccessful = false, 409, s"Bad response")
          handleResponse(replyTo, tripResponse)
      }

    case ReadTrip(id) =>
      val cmd = client.execute(get(id).from("trips" / "_doc"))
      val replyTo = sender()

      cmd.onComplete {
        case Success(either) => either match {
          case Right(right) =>
            if (right.result.found) {
              either.map(e => e.result.to[Trip]).foreach { recipe =>
                val tripResponse = TripResponse(recipe, isSuccessful = true, 201, null)
                handleResponse(replyTo, tripResponse)
                var msg = TelegramMessage(chat_id, s"Trip with ${id} found")

              }
            } else {
              val tripResponse = TripResponse(null, isSuccessful = false, 404, s"Trip with id = $id not found")
              handleResponse(replyTo, tripResponse)
            }

          case Left(_) =>
            val tripResponse = TripResponse(null, isSuccessful = false, 500, s"Elastic Search internal error")
            handleResponse(replyTo, tripResponse)
        }

        case Failure(exception) =>
          val tripResponse = TripResponse(null, isSuccessful = false, 401, exception.getMessage)
          handleResponse(replyTo, tripResponse)
      }

    case UpdateTrip(trip) =>
      val cmd = client.execute(update(trip.id).in("trips" / "_doc").docAsUpsert(trip))
      val replyTo = sender()

      cmd.onComplete {
        case Success(_) =>
          val tripResponse = TripResponse(null, isSuccessful = true, 201, s"Trip with ID = ${trip.id} successfully updated. Recipe = [$trip]")
          handleResponse(replyTo, tripResponse)
          var msg = TelegramMessage(chat_id, s"Trip with ID = ${trip.id} successfully updated. Trip = [$trip]")


        case Failure(_) =>
          val tripResponse = TripResponse(null, isSuccessful = false, 409, s"Bad response")
          handleResponse(replyTo, tripResponse)
      }

    case DeleteTrip(id) =>
      val cmd = client.execute(delete(id).from("trips" / "_doc"))
      val replyTo = sender()

      cmd.onComplete {
        case Success(either) => either match {
          case Right(right) =>
            if (right.result.found) {
              val tripResponse = TripResponse(null, isSuccessful = true, 201, s"Trip with ID = $id successfully deleted.")
              handleResponse(replyTo, tripResponse)
              var msg = TelegramMessage(chat_id, s"Trip with ID = ${id} successfully deleted.")




            } else {
              val tripResponse = TripResponse(null, isSuccessful = false, 409, s"Trip  with id = $id not found.")
              handleResponse(replyTo, tripResponse)
            }
          case Left(_) =>
            val tripResponse = TripResponse(null, isSuccessful = false, 500, s"Elastic Search internal error")
            handleResponse(replyTo, tripResponse)
        }
        case Failure(exception) =>
          val tripResponse = TripResponse(null, isSuccessful = false, 401, exception.getMessage)
          handleResponse(replyTo, tripResponse)
      }
  }

  private def handleResponse(replyTo: ActorRef, tripResponse: TripResponse): Unit = {
    if (tripResponse.isSuccessful) {
      if (tripResponse.trip != null) {
        replyTo ! Right(tripResponse.trip)
      } else {
        replyTo ! Right(SuccessfulResponse(tripResponse.statusCode, tripResponse.message))
      }
    } else {
      replyTo ! Left(ErrorResponse(tripResponse.statusCode, tripResponse.message))
    }
  }
}