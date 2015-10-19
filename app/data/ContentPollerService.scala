package data

import akka.actor.{ActorSystem, Props}
import data.CapiPollerConfig._
import org.joda.time.DateTime

case class UpdateThreshold(threshold: DateTime)

class ContentPollService(contentSource: ContentSource)(implicit val system: ActorSystem) {
  implicit val ec = system.dispatcher
  val stuff = new ContentPollFetcher(contentSource, new EventPublisher(eventsEndpoint, accessKey))
  val actor = system.actorOf(Props(new ContentPollActor(stuff.fetchContentUnit)))

  def start() = actor ! StartPolling

  def stop() = actor ! StopPolling
}

object ContentPollService {

  implicit val system = ActorSystem("content-poll")

  def apply(contentSource: ContentSource): ContentPollService = new ContentPollService(contentSource)

}




