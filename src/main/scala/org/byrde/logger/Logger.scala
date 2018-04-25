package challenge.logger

import play.api.libs.json.JsObject

import akka.event.LoggingAdapter

abstract class Logger {
  protected def logger: LoggingAdapter

  def info[T](msg: String, elem: T)(implicit loggingInformation: LoggingInformation[T]): Unit =
    logger.info(loggingInformation.log(msg, elem).toString)

  def info[T](msg: JsObject, elem: T)(implicit loggingInformation: LoggingInformation[T]): Unit =
    logger.info(loggingInformation.log(msg, elem).toString)

  def warn[T](msg: String, elem: T)(implicit loggingInformation: LoggingInformation[T]): Unit =
    logger.warning(loggingInformation.log(msg, elem).toString)

  def warn[T](msg: JsObject, elem: T)(implicit loggingInformation: LoggingInformation[T]): Unit =
    logger.warning(loggingInformation.log(msg, elem).toString)

  def error[T](msg: String, elem: T)(implicit loggingInformation: LoggingInformation[T]): Unit =
    logger.error(loggingInformation.log(msg, elem).toString)

  def error[T](msg: JsObject, elem: T)(implicit loggingInformation: LoggingInformation[T]): Unit =
    logger.error(loggingInformation.log(msg, elem).toString)
}