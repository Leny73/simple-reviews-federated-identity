package org.simplereviews.logger.impl

import org.simplereviews.guice.Modules
import org.simplereviews.logger.Logger

import akka.event.{ Logging, LoggingAdapter }

class ApplicationLogger(modules: Modules) extends Logger {
  override protected def logger: LoggingAdapter =
    Logging(modules.akka.actorSystem, getClass)
}
