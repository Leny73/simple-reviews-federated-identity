package challenge.logger

import challenge.utils.OptionUtils._

import play.api.libs.json.{ JsObject, JsString, Json }

import akka.http.scaladsl.model.{ HttpRequest, IdHeader }
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
  implicit val httpRequestInformation: LoggingInformation[HttpRequest] = (req: HttpRequest) =>
    Json.obj(
      "id" -> req.header[IdHeader].fold("None")(header => s"${header.id}"),
      "uri" -> req.uri.toString,
      "method" -> req.method.value.toString,
      "headers" -> req.headers.map(header => s"${header.name}: ${header.value}"),
      "cookies" -> req.cookies.map(cookie => s"${cookie.name}: ${cookie.value}")
    )

  implicit val exceptionWithHttpRequest: LoggingInformation[(Exception, HttpRequest)] =
    (elem: (Exception, HttpRequest)) => {
      val (ex, req) =
        elem._1 -> elem._2

      def serializeException(ex: Exception): JsObject = {
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

  implicit def httpRequestWithEntity[T]: LoggingInformation[HttpRequestWithEntity[T]] =
    (reqWithEntity: HttpRequestWithEntity[T]) => {
      val req =
        reqWithEntity.request
      val bodyOpt =
        if (reqWithEntity.body.toString.isEmpty) None else reqWithEntity.body.toString.?

      bodyOpt.fold(httpRequestInformation(req)) { body =>
        httpRequestInformation(req) ++ Json.obj("body" -> body)
      }
    }
}