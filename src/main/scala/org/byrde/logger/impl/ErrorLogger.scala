package challenge.logger.impl

import challenge.guice.Modules
import challenge.logger.{ Logger, LoggingInformation }

import com.google.inject.Inject

import akka.event.{ Logging, LoggingAdapter }

class ErrorLogger @Inject() (modules: Modules) extends Logger {
  override protected val logger: LoggingAdapter =
    Logging(modules.akka.actorSystem, getClass)

  def error[T](throwable: Throwable, elem: T)(implicit loggingInformation: LoggingInformation[(Exception, T)]): Unit =
    error(new Exception(throwable), elem)

  def error[T](exception: Exception, elem: T)(implicit loggingInformation: LoggingInformation[(Exception, T)]): Unit =
    logger.error(loggingInformation.log(exception.getMessage, exception -> elem).toString)
}
