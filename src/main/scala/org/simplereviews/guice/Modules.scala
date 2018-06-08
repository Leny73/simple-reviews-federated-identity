package org.simplereviews.guice

import com.google.inject.Inject

import org.byrde.commons.services.email.EmailServiceWrapper
import org.simplereviews.configuration.Configuration
import org.simplereviews.logger.impl.{ ApplicationLogger, ErrorLogger, RequestLogger }
import org.simplereviews.persistence.{ Persistence, Tables }
import org.simplereviews.services.aws.S3ServiceWrapper

@Singleton
class Modules @Inject() (
    val configuration: Configuration,
    val akka: Akka
) {
  lazy val persistence: Persistence =
    new Persistence(this)

  lazy val tables: Tables =
    new Tables(this)

  lazy val s3ServiceWrapper: S3ServiceWrapper =
    new S3ServiceWrapper(this)(akka.materializer, akka.system)

  lazy val emailService: EmailServiceWrapper =
    EmailServiceWrapper(configuration.emailConfiguration)

  lazy val applicationLogger: ApplicationLogger =
    new ApplicationLogger(this)

  lazy val requestLogger: RequestLogger =
    new RequestLogger(this)

  lazy val errorLogger: ErrorLogger =
    new ErrorLogger(this)
}
