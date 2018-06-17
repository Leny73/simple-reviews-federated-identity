package org.simplereviews.logger

import org.byrde.commons.utils.OptionUtils._

import play.api.libs.json.{ JsObject, JsString, Json }

import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, IdHeader }
import akka.http.scaladsl.server.directives.HttpRequestWithEntity

trait LoggingInformation[-T] {
  def log(elem: T): JsObject

  def apply(elem: T): JsObject =
    log(elem)

  def log(msg: String, elem: T): JsObject =
    log(elem) + ("message" -> JsString(msg))

  def log(msg: JsObject, elem: T): JsObject =
    log(elem) + ("message" -> msg)
}

object LoggingInformation {
  implicit val httpRequestInformation: LoggingInformation[HttpRequest] =
    new LoggingInformation[HttpRequest] {
      override def log(elem: HttpRequest): JsObject =
        Json.obj(
          "id" -> elem.header[IdHeader].fold("None")(header => s"${header.id}"),
          "uri" -> elem.uri.toString,
          "method" -> elem.method.value.toString,
          "headers" -> elem.headers.map(header => s"${header.name}: ${header.value}"),
          "cookies" -> elem.cookies.map(cookie => s"${cookie.name}: ${cookie.value}")
        )
    }

  implicit def httpRequestWithEntity[T]: LoggingInformation[HttpRequestWithEntity[T]] =
    new LoggingInformation[HttpRequestWithEntity[T]] {
      override def log(elem: HttpRequestWithEntity[T]): JsObject = {
        val req =
          elem.request

        val bodyOpt =
          if (elem.body.toString.isEmpty)
            None
          else
            elem.body.toString.?

        bodyOpt.fold(httpRequestInformation(req)) { body =>
          httpRequestInformation(req) ++ Json.obj("body" -> body)
        }
      }
    }

  implicit val httpResponseInformation: LoggingInformation[HttpResponse] =
    new LoggingInformation[HttpResponse] {
      override def log(elem: HttpResponse): JsObject =
        Json.obj(
          "status" -> elem.status.toString(),
          "headers" -> elem.headers.map(header => s"${header.name}: ${header.value}")
        )
    }

  implicit val exceptionWithHttpRequest: LoggingInformation[(Throwable, HttpRequest)] =
    new LoggingInformation[(Throwable, HttpRequest)] {
      override def log(elem: (Throwable, HttpRequest)): JsObject = {
        val (ex, req) =
          elem._1 -> elem._2

        def serializeException(ex: Throwable): JsObject = {
          def loop(throwable: Throwable): JsObject = {
            val causedBy =
              Option(throwable) match {
                case Some(cause) =>
                  Json.obj("causedBy" -> loop(cause.getCause))
                case None =>
                  Json.obj()
              }
            Json.obj(
              "class" -> ex.getClass.getName(),
              "message" -> ex.getMessage,
              "stackTrace" -> ex.getStackTrace.map(_.toString)
            ) ++ causedBy
          }

          Json.obj(
            "class" -> ex.getClass.getName(),
            "message" -> ex.getMessage,
            "stackTrace" -> ex.getStackTrace.map(_.toString)
          ) ++ loop(ex.getCause)
        }

        httpRequestInformation(req) ++ Json.obj(
          "message" -> ex.getMessage,
          "exception" -> serializeException(ex)
        )
      }
    }
}