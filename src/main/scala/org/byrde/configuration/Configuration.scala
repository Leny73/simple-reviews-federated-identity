package challenge.configuration

import com.google.inject.Inject
import com.typesafe.config.{ Config, ConfigFactory }

import akka.util.Timeout

import scala.concurrent.duration._

class Configuration @Inject() () {
  lazy val underlyingConfig: Config =
    ConfigFactory.load().resolve()
  lazy val name: String =
    underlyingConfig.getString("akka.server.name")
  lazy val interface: String =
    underlyingConfig.getString("akka.server.interface")
  lazy val port: Int =
    underlyingConfig.getInt("akka.server.port")
  lazy val timeout: Timeout =
    Timeout(underlyingConfig.getInt("akka.server.timeout") seconds)
}
