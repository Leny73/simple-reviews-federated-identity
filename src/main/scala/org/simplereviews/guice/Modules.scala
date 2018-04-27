package org.simplereviews.guice

import com.google.inject.Inject

import org.simplereviews.configuration.Configuration
import org.simplereviews.logger.impl.{ ApplicationLogger, ErrorLogger, RequestLogger }
import org.simplereviews.persistence.{ Persistence, Tables }

class Modules @Inject() (
    val configuration: Configuration,
    val akka: Akka
) {
  lazy val persistence: Persistence =
    new Persistence(this)

  lazy val tables: Tables =
    new Tables(this)

  lazy val applicationLogger: ApplicationLogger =
    new ApplicationLogger(this)

  lazy val requestLogger: RequestLogger =
    new RequestLogger(this)

  lazy val errorLogger: ErrorLogger =
    new ErrorLogger(this)
}
