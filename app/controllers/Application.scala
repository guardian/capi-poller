package controllers

import javax.inject.Inject
import play.api.mvc._

class Application @Inject() extends Controller {

  def healthCheck = Action {
    if (true) Ok else ServiceUnavailable
  }

}
