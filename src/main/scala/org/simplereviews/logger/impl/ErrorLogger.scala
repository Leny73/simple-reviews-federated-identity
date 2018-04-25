package org.simplereviews.logger.impl

import org.simplereviews.guice.Modules
import org.simplereviews.logger.{ Logger, LoggingInformation }

import akka.event.{ Logging, LoggingAdapter }

class ErrorLogger(modules: Modules) extends Logger {
  override protected val logger: LoggingAdapter =
    Logging(modules.akka.actorSystem, getClass)

  def error[T](throwable: Throwable, elem: T)(implicit loggingInformation: LoggingInformation[(Exception, T)]): Unit =
    error(new Exception(throwable), elem)

  def error[T](exception: Exception, elem: T)(implicit loggingInformation: LoggingInformation[(Exception, T)]): Unit =
    logger.error(loggingInformation.log(exception.getMessage, exception -> elem).toString)
}
