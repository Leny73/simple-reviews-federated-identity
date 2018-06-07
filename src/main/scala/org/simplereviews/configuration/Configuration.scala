package org.simplereviews.configuration

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import com.google.inject.Inject
import com.typesafe.config.{ Config, ConfigFactory }

import org.byrde.commons.utils.auth.conf.JwtConfig
import org.byrde.commons.utils.email.conf.EmailConfig

import akka.stream.alpakka.s3.S3Settings
import akka.util.Timeout

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

  lazy val env: String =
    underlyingAkkaConfiguration.getString("env")

  lazy val interface: String =
    underlyingAkkaConfiguration.getString("interface")

  lazy val port: Int =
    underlyingAkkaConfiguration.getInt("port")

  lazy val timeout: Timeout =
    Timeout(underlyingAkkaConfiguration.getInt("request-timeout").seconds)

  lazy val uploadConfiguration: UploadConfiguration =
    UploadConfiguration(
      underlyingAkkaConfiguration.getLong("upload.duration").seconds,
      underlyingAkkaConfiguration.getLong("upload.max-content-length")
    )

  lazy val corsConfiguration: CORSConfiguration =
    CORSConfiguration(
      underlyingAkkaConfiguration.getString("cors.origins").split(", "),
      underlyingAkkaConfiguration.getString("cors.methods").split(", "),
      underlyingAkkaConfiguration.getString("cors.headers").split(", ")
    )

  lazy val jwtConfiguration: JwtConfig =
    JwtConfig.apply(underlyingPlayConfig.get[play.api.Configuration]("jwt-config.client"))

  lazy val jdbcConfiguration: DatabaseConfig[JdbcProfile] =
    DatabaseConfig.forConfig("db")

  lazy val s3Configuration: S3Settings =
    S3Settings(underlyingConfig)

  lazy val emailConfiguration: EmailConfig =
    EmailConfig(underlyingPlayConfig.get[play.api.Configuration]("services.email"))

  lazy val imageBucket: String =
    s"${env.toLowerCase}-${name.toLowerCase}-images"
}
