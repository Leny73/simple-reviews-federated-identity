package org.simplereviews.configuration

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import com.google.inject.Inject
import com.typesafe.config.{ Config, ConfigFactory }

import org.byrde.commons.utils.auth.conf.JwtConfig

import akka.util.Timeout

import collection.JavaConverters._
import scala.concurrent.duration._

class Configuration @Inject() () {
  lazy val underlyingConfig: Config =
    ConfigFactory.load().resolve()

  lazy val underlyingPlayConfig =
    play.api.Configuration(underlyingConfig)

  lazy val underlyingAkkaConfiguration: Config =
    underlyingConfig.getConfig("akka.server")

  lazy val name: String =
    underlyingAkkaConfiguration.getString("name")

  lazy val interface: String =
    underlyingAkkaConfiguration.getString("interface")

  lazy val port: Int =
    underlyingAkkaConfiguration.getInt("port")

  lazy val timeout: Timeout =
    Timeout(underlyingAkkaConfiguration.getInt("request-timeout") seconds)

  lazy val corsConfiguration: CORSConfiguration =
    CORSConfiguration(
      underlyingAkkaConfiguration.getStringList("cors.origins").asScala,
      underlyingAkkaConfiguration.getStringList("cors.headers").asScala)

  lazy val jwtConfiguration: JwtConfig =
    JwtConfig.apply(underlyingPlayConfig.get[play.api.Configuration]("jwt-config.client"))

  lazy val jdbcConfiguration: DatabaseConfig[JdbcProfile] =
    DatabaseConfig.forConfig("db")
}
