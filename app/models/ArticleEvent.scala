package models

import com.gu.contentapi.client.model.v1.Content
import org.joda.time.DateTime
import play.api.libs.json._
import com.gu.contentapi.client.utils.CapiModelEnrichment._

object ArticleEvent {

  import JsonUtils._

  implicit val af = Json.writes[ArticleEvent]

  def fromContent(content: Content, eventTime: DateTime = DateTime.now): Option[ArticleEvent] = for {
    webPublicationDate <- content.webPublicationDate
    section <- content.sectionId
  } yield ArticleEvent(
    entityId = content.id,
    eventTime = eventTime,
    properties = Map(
      "section" -> Right(List(section)),
      "tags" -> Right(content.tags.map(_.id)),
      "type" -> Right(List(content.`type`.toString.toLowerCase)),
      "webPublicationDate" -> Left(webPublicationDate.toJodaDateTime.toString)
    )
  )
}
case class ArticleEvent(
  event: String = "$set",
  entityType: String = "item",
  entityId: String,
  properties: Map[String, Either[String, Seq[String]]],
  eventTime: DateTime
)

object ArticleEventResponse { implicit val af = Json.format[ArticleEventResponse] }
case class ArticleEventResponse(eventId: String)


