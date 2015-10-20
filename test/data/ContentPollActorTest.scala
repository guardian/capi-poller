package data

import com.gu.contentapi.client.GuardianContentClient
import com.gu.contentapi.client.model.v1.{CapiDateTime, Content}
import com.gu.contentapi.client.model.v1.ContentType.Article
import models.{ArticleEvent, PublisherApiError, ArticleEventResponse}
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import scalaz.syntax.either._
import scala.concurrent.{ExecutionContext, Future}
import org.specs2.concurrent.ExecutionEnv
import play.api.test.Helpers._
import play.api.test.FakeApplication

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

  val aEvent1 = mock[ArticleEvent]
  aEvent1.event returns "invalid-event"
  aEvent1.entityId returns "article1"
  aEvent1.properties returns Map.empty
  aEvent1.eventTime returns new DateTime(DateTime.now.minusDays(1))

  val aEvent2 = mock[ArticleEvent]
  aEvent2.entityId returns "article2"
  aEvent2.properties returns Map.empty
  aEvent2.eventTime returns new DateTime(DateTime.now.minusDays(2))

  val publisher = mock[EventPublisher]
  val contentSource = new FakeContentSource(List(content))
  val service = new ContentPollFetcher(contentSource, publisher)

  //change the any[ArticleEvent] to actual ArticleEvent1 and ArticleEvent2
  //and if articleEvent1 fails then it should try to publish ArticleEvent2.

  "Integration test" should {
    "receive a ArticleEventResponse after posting an article" in {
      running(FakeApplication()) {
        publisher.publishEvent(any[ArticleEvent])(any[ExecutionContext]) returns Future.successful(ArticleEventResponse("my-event-id").right[PublisherApiError])
        service.fetchContent(DateTime.now, DateTime.now) must beEqualTo(List(ArticleEventResponse("my-event-id").right[PublisherApiError])).await
      }
    }

//    "should keep posting after a failure" in {
//      running(FakeApplication()) {
//        publisher.publishEvent(Matchers.eq(aEvent1))(any[ExecutionContext]) returns Future.successful(UnexpectedServerResponse.left[ArticleEventResponse])
//        service.fetchContent(DateTime.now, DateTime.now) must beEqualTo(List(ArticleEventResponse("my-event-id").right[PublisherApiError])).await
//      }
//    }
  }
}
