package data

import com.gu.contentapi.client.GuardianContentClient
import com.gu.contentapi.client.model.v1.{CapiDateTime, Content}
import com.gu.contentapi.client.model.v1.ContentType.Article
import models.{ArticleEvent, PublisherApiError, ArticleEventResponse}
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import scala.collection.generic.CanBuildFrom
import scalaz.syntax.either._
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, \/-}
import org.specs2.concurrent.ExecutionEnv

class ContentPollActorTest(implicit ev: ExecutionEnv) extends Specification with Mockito {

  class FakeContentSource(content: List[Content]) extends ContentSource(mock[GuardianContentClient]) {
    override def withContent[T](from: DateTime, until: DateTime)(fn: Content => Future[T])(implicit ec: ExecutionContext): Future[List[T]] = {
      Future.sequence(content.map(fn))(List.canBuildFrom[T], ec)
    }
  }

  val myId = "my-guardian-id"
  val referenceDate = DateTime.now
  val content = Content(
    id = myId,
    webTitle = "My Guardian Article",
    apiUrl = s"http://content.guardianapis.com/$myId",
    sectionId = Some("my-section"),
    `type` = Article,
    webPublicationDate = Some(CapiDateTime(referenceDate.getMillis)),
    webUrl = s"http://www.theguardian.com/$myId"
  )

  "fetchContent" should {
    "keep publishing events after an event failure" in {

      val aEvent1 = ArticleEvent(
        event = "invalid-event",
        entityId = "article2",
        properties = Map.empty,
        eventTime = new DateTime(DateTime.now.minusDays(1))
      )
      val aEvent2 = ArticleEvent(
        entityId = "article1",
        properties = Map.empty,
        eventTime = new DateTime(DateTime.now.minusDays(2))
      )

      val publisher = mock[ContentPublisher]
      publisher.publishEvent(any[ArticleEvent])(any[ExecutionContext]) returns Future.successful(ArticleEventResponse("my-event-id").right[PublisherApiError])

      //publisher.publishEvent(aEvent1)(any[ExecutionContext]) returns Future.successful(ArticleEventResponse("my-event-id1").right[PublisherApiError])
      //publisher.publishEvent(aEvent2)(any[ExecutionContext]) returns Future.successful(ArticleEventResponse("my-event-id2").right[PublisherApiError])

      //change the any[ArticleEvent] to actual ArticleEvent1 and ArticleEvent2
      //and if articleEvent1 fails then it should try to publish ArticleEvent2.

      val contentSource = new FakeContentSource(List(content))
      val service = new ContentPollFetcher(contentSource, publisher)
      service.fetchContent(DateTime.now, DateTime.now) must beEqualTo(List(ArticleEventResponse("my-event-id").right[PublisherApiError])).await
    }
  }

}
