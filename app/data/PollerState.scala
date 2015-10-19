package data

trait PollerState
object PollFinished extends PollerState
object StartPolling extends PollerState
object StopPolling extends PollerState
object Poll extends PollerState
