package models

import com.gu.contentapi.client.model.v1.Content
import org.joda.time.DateTime
import play.api.libs.json._

object ArticleEvent {

  import JsonUtils._

  implicit val af = Json.format[ArticleEvent]

  def fromContent(content: Content): Option[ArticleEvent] = content.webPublicationDate map { webPublicationDate =>
    ArticleEvent(
      entityId = content.id,
      properties = content.sectionId map { section => Map("section" -> section) } getOrElse Map.empty,
      eventTime = new DateTime(webPublicationDate.dateTime)
    )
  }
}
case class ArticleEvent(
  event: String = "$set",
  entityType: String = "item",
  entityId: String,
  properties: Map[String, String],
  eventTime: DateTime
)

object ArticleEventResponse { implicit val af = Json.format[ArticleEventResponse] }
case class ArticleEventResponse(eventId: String)


