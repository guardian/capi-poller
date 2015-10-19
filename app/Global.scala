import com.gu.contentapi.client.GuardianContentClient
import data.{ContentPollService, ContentSource}
import play.Play
import play.api.GlobalSettings
import scala.concurrent.ExecutionContext.Implicits.global
import data.CapiPollerConfig

object Global extends GlobalSettings {

  lazy val capiClient = new GuardianContentClient(CapiPollerConfig.apiKey)
  lazy val contentSource = new ContentSource(capiClient)
  lazy val contentPollService = ContentPollService(contentSource)

  override def onStart(app: play.api.Application) {
    if (!Play.isTest && CapiPollerConfig.enablePolling) {
      contentPollService.start()
    }
  }

  override def onStop(app: play.api.Application) {
    contentPollService.stop()
  }
}