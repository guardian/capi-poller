package data

import models._
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.{WSResponse, WS}
import scala.concurrent.{Future, ExecutionContext}
import scalaz.\/
import scalaz.syntax.either._
import play.api.Play.current

class EventPublisher(endpoint: String, accessKey: String) {

  def publishEvent(event: ArticleEvent)(implicit ec: ExecutionContext): Future[PublisherApiError \/ ArticleEventResponse] = {
    WS.url(endpoint)
      .withQueryString("accessKey" -> accessKey)
      .withHeaders("Content-Type" -> "application/json")
      .post(Json.toJson(event))
      .map(parseResponse)
  }

  private def parseResponse(response: WSResponse) = {
    if (response.status == 201) {
      response.json.validate[ArticleEventResponse] match {
        case JsSuccess(eventResponse, _) => eventResponse.right
        case JsError(error) => ParseError(error).left
      }
    } else {
      UnexpectedServerResponse(response).left
    }
  }

}
