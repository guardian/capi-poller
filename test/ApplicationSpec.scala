import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test.{FakeRequest, FakeApplication}
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {
    "return a 200 status code on the healthCheck endpoint" in {
      running(FakeApplication()) {
        val root = route(FakeRequest(GET, "/")).get
        status(root) must equalTo(OK)
      }
    }
  }
}

