package models

import org.joda.time.{DateTimeZone, DateTime}
import play.api.libs.json._

object JsonUtils {
  lazy implicit val jodaFormat = new Format[DateTime] {
    override def reads(json: JsValue): JsResult[DateTime] =
      Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ").reads(json).map(_.withZone(DateTimeZone.UTC))

    override def writes(o: DateTime): JsValue =
      Writes.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").writes(o.withZone(DateTimeZone.UTC))
  }
}
