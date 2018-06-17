package org.simplereviews.logger.impl

import com.google.inject.Inject

import org.simplereviews.guice.Akka
import org.simplereviews.logger.{ Logger, LoggingInformation }

import play.api.libs.json.Json

import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.model.HttpRequest

class RequestLogger @Inject() (akka: Akka) extends Logger {
  override protected val logger: LoggingAdapter =
    Logging(akka.system, getClass)

  def request(id: String, epoch: Long, status: String, req: HttpRequest)(implicit loggingInformation: LoggingInformation[HttpRequest]): Unit =
    info(Json.obj("id" -> id, "status" -> status, "epoch" -> s"${epoch}ms"), req)
}
