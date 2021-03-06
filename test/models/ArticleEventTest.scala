package models

import com.gu.contentapi.client.model.v1.ContentType.Article
import com.gu.contentapi.client.model.v1.TagType.Keyword
import com.gu.contentapi.client.model.v1.{Tag, CapiDateTime, ContentType, Content}
import org.joda.time.DateTime
import org.specs2.mutable.Specification

class ArticleEventTest extends Specification {

  val referenceDate = DateTime.now()
  val referenceSection = "my-section"
  val myId = "my-guardian-article"

  val eventTime = DateTime.now

  val articleEvent = new ArticleEvent(
    entityId = "my-guardian-article",
    properties = Map(
      "section" -> Right(List(referenceSection)),
      "tags" -> Right(List("test/tag")),
      "type" -> Right(List("article")),
      "webPublicationDate" -> Left(referenceDate.toString)
    ),
    eventTime = eventTime
  )
  val content = Content(
    id = myId,
    webTitle = "My Guardian Article",
    apiUrl = s"http://content.guardianapis.com/$myId",
    sectionId = Some(referenceSection),
    tags = List(Tag(id = "test/tag", `type` = Keyword, webTitle = "test", webUrl = "", apiUrl = "")),
    `type` = Article,
    webPublicationDate = Some(CapiDateTime(referenceDate.getMillis)),
    webUrl = s"http://www.theguardian.com/$myId"
  )

  "fromContent" should {

    "Correctly generate an ArticleEvent from a piece Content" in {
      Some(articleEvent) shouldEqual ArticleEvent.fromContent(content, eventTime)
    }

  }
}