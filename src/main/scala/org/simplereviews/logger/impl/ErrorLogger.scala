package org.simplereviews.logger.impl

import com.google.inject.Inject

import org.simplereviews.guice.Akka
import org.simplereviews.logger.{ Logger, LoggingInformation }

import akka.event.{ Logging, LoggingAdapter }

class ErrorLogger @Inject() (akka: Akka) extends Logger {
  override protected val logger: LoggingAdapter =
    Logging(akka.system, getClass)

  def error[T](throwable: Throwable)(implicit loggingInformation: LoggingInformation[Throwable]): Unit =
    logger.error(loggingInformation.log("Client Error", throwable).toString)

  def error[T](throwable: Throwable, elem: T)(implicit loggingInformation: LoggingInformation[(Throwable, T)]): Unit =
    logger.error(loggingInformation.log(throwable.getMessage, throwable -> elem).toString)
}
