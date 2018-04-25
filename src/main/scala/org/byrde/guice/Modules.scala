package challenge.guice

import challenge.configuration.Configuration
import challenge.logger.impl.{ ApplicationLogger, ErrorLogger, RequestLogger }

import com.google.inject.Inject

class Modules @Inject() (
  val configuration: Configuration,
  val applicationLogger: ApplicationLogger,
  val requestLogger: RequestLogger,
  val errorLogger: ErrorLogger,
  val akka: Akka
)
