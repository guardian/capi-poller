package data

import akka.agent.Agent
import com.gu.contentapi.client.model._
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.client.{GuardianContentApiError, GuardianContentClient, Parameters}
import data.FutureEither.\?/
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, \/, \/-}

object QueryImplicits {

  private val BaseFields = List(
    "commentable",
    "trailText",
    "standfirst",
    "lastModified",
    "shortUrl",
    "headline",
    "liveBloggingNow",
    "body",
    "starRating",
    "byline",
    "allowUgc",
    "main",
    "internalContentCode",
    "isInappropriateForSponsorship",
    "shouldHideAdverts",
    "displayHint"
  )

  private val TagTypes = List(
    "type",
    "contributor",
    "tone",
    "keyword",
    "series"
  )

  private val ElementTypes = List(
    "image",
    "video",
    "audio",
    "embed"
  )

  private val ReferenceTypes = List(
    "pa-football-team",
    "witness-assignment"
  )

  implicit class RichParametersQuery[T <: ShowParameters[T]](query: T) {
    def showRelevantTags: T = query.showTags(TagTypes mkString ",")

    def showRelevantElements: T = query.showElements(ElementTypes mkString ",")

    def showBaseFields: T = query.showFields(BaseFields mkString ",")
  }

  implicit class RichReferencesQuery[T <: ShowReferencesParameters[T]](query: T) {
    def showRelevantReferences: T = query.showReferences(ReferenceTypes mkString ",")
  }

  implicit class ContentApiParams[Owner <: Parameters[Owner]](query: Owner) {
    private val commaSeparatedCAPIParams = List("tag")

    def mergeParams(params: Seq[(String, String)]): Owner = {
      params.foldLeft(query) { case (updatedQuery, (k, v)) =>
        val updatedValue =
          if (commaSeparatedCAPIParams.contains(k)) {
            query.parameters.get(k).fold(v) { original => s"$v,$original" }
          } else v
        updatedQuery.stringParam(k, updatedValue)
      }
    }

    def mergeTag(tagStr: String): Owner = {
      mergeParams(Seq("tag" -> tagStr))
    }
  }
}

object QueryTools {
  import QueryImplicits._

  private val excludedTags = List(
    "tone/advertisement-features",
    "advertisement-features/advertisement-features",
    "type/audioslideshow",
    "type/competition",
    "type/quiz",
    "type/poll",
    "football/series/world-cup-tools",
    "type/sudoku",
    "lifeandstyle/series/kakuro"
  )

  private val ContentTypeTags = excludedTags.map(t => s"-$t").mkString(",")

  type AdjustSearchQuery = SearchQuery => SearchQuery
  type AdjustItemQuery = ItemQuery => ItemQuery

  def adjustSearchQuery: AdjustSearchQuery = updateSearchQuery
  def adjustItemQuery: AdjustItemQuery = updateItemQuery

  private def updateItemQuery(itemQuery: ItemQuery): ItemQuery =
    itemQuery
      .showRelevantTags
      .showRelevantElements
      .showRelevantReferences
      .showBaseFields
      .mergeTag(ContentTypeTags)

  private def updateSearchQuery(searchQuery: SearchQuery): SearchQuery =
    searchQuery
      .showRelevantTags
      .showRelevantElements
      .showRelevantReferences
      .showBaseFields
      .mergeTag(ContentTypeTags)
}

class ContentSource(capiClient: GuardianContentClient)(implicit val ec: ExecutionContext) {
  implicit val cc = capiClient

  def contentResponseToEither[T](response: Future[T]): GuardianContentApiError \?/ T = FutureEither(response.map(\/.right).recover {
    case err: GuardianContentApiError => \/.left(err)
    case err => \/.left(GuardianContentApiError(500, err.getMessage))
  })

  def content(id: String)(implicit ec: ExecutionContext): GuardianContentApiError \?/ ItemResponse = {
    val itemQuery = {
      val q = capiClient.item(id)
      q.withParameter(
        q.showBlocks.withValue(Some("all")))
        .withParameter(q.showElements.withValue(Some("all")))
        .withParameter(q.showFields.withValue(Some("all")))
        .withParameter(q.showTags.withValue(Some("all")))
    }
    contentResponseToEither(capiClient.getResponse(itemQuery))
  }

  def contentSince(from: DateTime, until: DateTime, page: Int)(implicit ec: ExecutionContext): GuardianContentApiError \?/ SearchResponse = {
    val itemQuery = {
      val q = new SearchQuery().fromDate(from).toDate(until)
      q.withParameter(
        q.showBlocks.withValue(Some("all")))
        .withParameter(q.showElements.withValue(Some("all")))
        .withParameter(q.showFields.withValue(Some("all")))
        .withParameter(q.showTags.withValue(Some("all")))
        .withParameter(q.page.withValue(Some(page)))
        .orderBy("oldest")
    }
    contentResponseToEither(capiClient.getResponse(itemQuery))
  }

  def withContent[T](from: DateTime, until: DateTime)(fn: Content => Future[T])(implicit ec: ExecutionContext): Future[List[T]] = {
    def recur(page: Int, results: List[Future[T]]): Future[List[Future[T]]] = contentSince(from, until, page).future flatMap {
      case \/-(response) =>
        response.results.foreach { content =>
          Agent(Set.empty[String]).send(_ - content.id)
        }
        val updatedResults = results ++ (response.results map fn)
        if (response.pages > response.currentPage)
          recur(page + 1, updatedResults)
        else
          Future.successful(updatedResults)

      case -\/(error) => Future.successful(results)
    }
    recur(1, Nil) flatMap { Future.sequence(_) }
  }

}
