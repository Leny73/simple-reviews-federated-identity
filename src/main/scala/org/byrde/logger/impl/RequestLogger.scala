package challenge.logger.impl

import javax.inject.Inject

import challenge.guice.Modules
import challenge.logger.{ Logger, LoggingInformation }

import play.api.libs.json.Json

import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.model.HttpRequest

class RequestLogger @Inject() (modules: Modules) extends Logger {
  override protected val logger: LoggingAdapter =
    Logging(modules.akka.actorSystem, getClass)

  def request(id: String, epoch: Long, status: String, req: HttpRequest)(implicit loggingInformation: LoggingInformation[HttpRequest]): Unit =
    info(Json.obj("id" -> id, "status" -> status, "epoch" -> s"${epoch}ms"), req)
}
