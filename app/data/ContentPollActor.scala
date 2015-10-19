package data

import akka.actor.{Cancellable, Actor}
import org.joda.time.DateTime
import play.api.Logger
import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}

class ContentPollActor(taskFn: (DateTime, DateTime) => Future[Unit]) extends Actor {

  implicit val ec = context.dispatcher
  var threshold = DateTime.now.minusMinutes(20)
  var cancellable: Option[Cancellable] = None
  var pollInProgress = false

  def schedule(initialDelay: FiniteDuration = 2.seconds, interval: FiniteDuration = 60.seconds) = {
    context.system.scheduler.schedule(initialDelay, interval, self, Poll)
  }

  def receive = {
    case StartPolling =>
      if (cancellable.isEmpty)
        cancellable = Option(schedule())

    case StopPolling =>
      cancellable foreach { _.cancel() }
      cancellable = None

    case Poll if pollInProgress =>
      Logger.error("New poll requested but current poll still is still in progress. Ignoring.")

    case Poll if !pollInProgress =>
      pollInProgress = true
      val until = DateTime.now
      taskFn(threshold, until).map { _ =>
        self ! UpdateThreshold(until.minusMinutes(5))
      } andThen {
        case _ =>
          self ! PollFinished
      }

    case PollFinished =>
      pollInProgress = false

    case UpdateThreshold(newThreshold) =>
      if (newThreshold isAfter threshold)
        threshold = newThreshold

    case _ =>
      Logger.error(s"ContentPollActor actor received an unknown message")
  }
}

