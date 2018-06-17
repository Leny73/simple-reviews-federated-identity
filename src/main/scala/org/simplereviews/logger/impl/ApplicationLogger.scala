package org.simplereviews.logger.impl

import com.google.inject.Inject

import org.simplereviews.guice.Akka
import org.simplereviews.logger.Logger

import akka.event.{ Logging, LoggingAdapter }

class ApplicationLogger @Inject() (akka: Akka) extends Logger {
  override protected def logger: LoggingAdapter =
    Logging(akka.system, getClass)
}
