package Serializers

import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import spray.json._
import scala.util.Try
import model.Trip

trait ElasticSerializer extends SprayJsonSerializer  {

  implicit object TripIndexable extends Indexable[Trip] {
    override def json(trip: Trip): String = trip.toJson.compactPrint
  }

  implicit object TripHitReader extends HitReader[Trip] {
    override def read(hit: Hit): Either[Throwable, Trip] = {
      Try {
        val json = hit.sourceAsString.parseJson
        json.convertTo[Trip]
      }.toEither
    }
  }
}
