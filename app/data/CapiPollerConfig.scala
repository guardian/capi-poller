package data

import com.gu.conf.ConfigurationFactory

object CapiPollerConfig {

  private val conf = ConfigurationFactory.getConfiguration("capi-poller", "gu")

  val eventsEndpoint = conf.getStringProperty("events.endpoint", "http://events.mobile.guardianapis.com/events.json")
  val accessKey = conf.getStringProperty("accesskey", "")
  val enablePolling = conf.getStringProperty("enablepolling", "false").toBoolean
  val apiKey = conf.getStringProperty("capi.apikey", "")

}

