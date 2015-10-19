package data

import models.{MissingWebPublicationDate, ArticleEvent, ArticleEventResponse, PublisherApiError}
import org.joda.time.DateTime
import play.api.Logger
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}
import scalaz.\/
import scalaz.syntax.either._

class ContentPollFetcher(contentSource: ContentSource, publisher: EventPublisher)(implicit ec: ExecutionContext) {

  private def logResults(ls: List[PublisherApiError \/ ArticleEventResponse]) = {
    val (failures, successes) = (ls.count(_.isLeft), ls.count(_.isRight))

    ls.flatMap( _.swap.toOption) foreach( error => Logger.error(s"${error.httpMessage}"))
    Logger.info(s"Published $successes events and $failures failures")
  }

  def fetchContentUnit(from: DateTime, until: DateTime): Future[Unit] =
    fetchContent(from, until).map(_ => ())

  def fetchContent(from: DateTime, until: DateTime) = {
    contentSource.withContent(from, until) { content =>
      Logger.info(s"Got article ${content.id}, attempting to publish")
      val response = ArticleEvent.fromContent(content) map publisher.publishEvent
      response getOrElse Future.successful(MissingWebPublicationDate.left)
    } andThen {
      case Success(ls) =>
        logResults(ls)
      case Failure(err) =>
        Logger.error("Failed to fetch articles", err)
    }
  }

}
