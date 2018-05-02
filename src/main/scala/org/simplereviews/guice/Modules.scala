package org.simplereviews.guice

import com.google.inject.Inject

import org.simplereviews.configuration.Configuration
import org.simplereviews.logger.impl.{ ApplicationLogger, ErrorLogger, RequestLogger }
import org.simplereviews.persistence.{ Persistence, Tables }

import akka.stream.alpakka.s3.S3Settings
import akka.stream.alpakka.s3.scaladsl.S3Client

class Modules @Inject() (
    val configuration: Configuration,
    val akka: Akka
) {
  lazy val persistence: Persistence =
    new Persistence(this)

  lazy val tables: Tables =
    new Tables(this)

  lazy val s3Client: S3Client =
    new S3Client(S3Settings(configuration.underlyingConfig))(akka.system, akka.materializer)

  lazy val applicationLogger: ApplicationLogger =
    new ApplicationLogger(this)

  lazy val requestLogger: RequestLogger =
    new RequestLogger(this)

  lazy val errorLogger: ErrorLogger =
    new ErrorLogger(this)
}
