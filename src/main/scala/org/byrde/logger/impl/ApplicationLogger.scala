package challenge.logger.impl

import challenge.guice.Modules
import challenge.logger.Logger

import com.google.inject.Inject

import akka.event.{ Logging, LoggingAdapter }

class ApplicationLogger @Inject() (modules: Modules) extends Logger {
  override protected def logger: LoggingAdapter =
    Logging(modules.akka.actorSystem, getClass)
}
