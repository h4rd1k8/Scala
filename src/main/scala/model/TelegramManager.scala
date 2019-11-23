package model

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import Serializers.{ElasticSerializer, SprayJsonSerializer, TelegramSerializer}
import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import model.TelegramManager.{SendWhenCreated, SendWhenDeletedd}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}


object TelegramManager{
  case class SendWhenCreated(trip: Trip, requestStatus: Int)
  case class SendWhenDeletedd(trip: Trip, requestStatus:Int)
  def props() = Props(new TelegramManager())
}

case class TelegramManager() extends TelegramSerializer with Actor with ElasticSerializer with SprayJsonSerializer {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val chat_id = -352088280


  val log = LoggerFactory.getLogger("TelegramManager")


  def sendMessageByBot(trip:Trip, requestType:Int, requestStatus:Int): Unit={
    var text = ""

    if(requestType == 1) {
      text = s"Trip ${trip.city} with ID: ${trip.id} has been created. Status of the request is ${requestStatus}"
    }
    else if(requestType == 2) {
      text =  s"Trip ${trip.city} with ID: ${trip.id} has been deleted. Status of the request is ${requestStatus}"
    }
    val msg : TelegramMessage = TelegramMessage(chat_id, text)

    val httpRequest = Marshal(msg).to[RequestEntity].flatMap { entity =>
      val request = HttpRequest(HttpMethods.POST, s"https://api.telegram.org/bot<token>/sendMessage", Nil, entity)
      log.info("Request: {}", request)
      Http().singleRequest(request)
    }


      httpRequest.onComplete {
        case Success(value) =>
          log.debug(s"Response: $value")
          value.discardEntityBytes()

        case Failure(exception) =>
          log.error("error")
      }

    }



  def receive: Receive = {
    case SendWhenCreated(trip:Trip, requestStatus: Int) =>
      sendMessageByBot(trip: Trip, 1, requestStatus)

    case SendWhenDeletedd(trip: Trip, requestStatus: Int) =>
      sendMessageByBot(trip, 2, requestStatus)
  }
}
