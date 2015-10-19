package models

import play.api.data.validation.ValidationError
import play.api.libs.json.{Json, JsPath}
import play.api.libs.ws.WSResponse

trait PublisherApiError {
  def httpStatus: Int
  def httpMessage: String
}

object MissingWebPublicationDate extends PublisherApiError {
  val httpStatus = 500
  val httpMessage = "Missing web publication date"
}

case class ParseError(errors: Seq[(JsPath, Seq[ValidationError])]) extends PublisherApiError {
  val httpStatus = 500
  val httpMessage = "Parse error"
}

case class UnexpectedServerResponse(response: WSResponse) extends PublisherApiError {
  val httpStatus = 500
  val httpMessage = s"Unexpected server response: ${response.status} ${response.body}"
}

case class ApiException(throwable: Throwable) extends PublisherApiError {
  val httpStatus = 500
  val httpMessage = s"Exception: $throwable"
}

case class ServerErrors(errors: Seq[ServerError]) extends PublisherApiError {
  lazy private val dominantError = errors.sortBy(-_.httpStatus).headOption

  val httpStatus = dominantError map { _.httpStatus } getOrElse 500
  val httpMessage = dominantError map { _.httpMessage } getOrElse s"Unknown error $this"
}

object ServerError { implicit val jf = Json.format[ServerError] }
case class ServerError(code: String) {
  val statusCodes = Map(
    "UNAUTHORIZED" -> 401,
    "FORBIDDEN" -> 403,
    "METHOD_NOT_ALLOWED" -> 405,
    "NOT_ACCEPTABLE" -> 406,
    "UNSUPPORTED_MEDIA_TYPE" -> 415,
    "INVALID_JSON" -> 400,
    "INVALID_TYPE" -> 400,
    "NOT_FOUND" -> 404,
    "NOT_FOUND" -> 404,
    "UNAUTHORIZED" -> 401,
    "MISSING_DATE" -> 401,
    "INVALID_AUTH_FORMAT" -> 401,
    "INVALID_AUTH_SCHEME" -> 401,
    "MISSING_API_KEY_ID" -> 401,
    "MISSING_SIGNATURE" -> 401,
    "INVALID_API_KEY_FORMAT" -> 401,
    "WRONG_SIGNATURE" -> 401,
    "API_KEY_NOT_FOUND" -> 401,
    "DATE_NOT_RECENT" -> 401,
    "INVALID_DATE_FORMAT" -> 401,
    "DUPLICATE_AUTH_PARAMETER" -> 401,
    "DUPLICATE_AUTH_ELEMENT" -> 401,
    "MISSING" -> 400,
    "INVALID_MIME_MULTIPART" -> 400,
    "MIME_PART_TOO_LARGE" -> 400,
    "MIME_PART_INVALID_SIZE" -> 400,
    "MIME_PART_INCORRECT_SIZE" -> 400,
    "MIME_PART_MISSING_FILENAME" -> 400,
    "MIME_PART_MISSING_SIZE" -> 400,
    "INVALID_DOCUMENT" -> 400,
    "DOES_NOT_EXIST" -> 400,
    "NOT_ALLOWED" -> 400,
    "INVALID_URL" -> 400,
    "INVALID_SCHEME" -> 400,
    "INCORRECT_HOST" -> 400,
    "INVALID_RESOURCE_TYPE" -> 400,
    "INVALID_RESOURCE_ID" -> 400,
    "MISSING_RESOURCE_ID" ->	400)

  val httpMessage = code
  val httpStatus = statusCodes.getOrElse(code, 500)
}