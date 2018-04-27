package org.simplereviews.controllers.directives

import java.util.UUID

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import org.simplereviews.logger.impl.{ ErrorLogger, RequestLogger }
import org.simplereviews.models.exceptions.ServiceResponseException

import play.api.libs.json.Json

import akka.http.scaladsl.model.{ HttpRequest, IdHeader }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directive0, Directive1, ExceptionHandler, Route }

trait RequestResponseHandlingDirective extends PlayJsonSupport {
  def requestLogger: RequestLogger

  def errorLogger: ErrorLogger

  def requestResponseHandler(route: Route): Route =
    requestId {
      case (request, id) =>
        handleExceptions(exceptionHandler(request)) {
          addRequestId(id) {
            addResponseId(id) {
              val start = System.currentTimeMillis
              bagAndTag(request, id, start) {
                route
              }
            }
          }
        }
    }

  private def exceptionHandler(req: HttpRequest): ExceptionHandler =
    ExceptionHandler {
      case exception: Exception =>
        val serviceResponseException =
          exception match {
            case serviceResponseException: ServiceResponseException =>
              serviceResponseException
            case ex: Exception =>
              ServiceResponseException(ex)
          }

        errorLogger.error(serviceResponseException, req)
        complete(serviceResponseException.status, Json.toJson(serviceResponseException))
    }

  private def requestId: Directive1[(HttpRequest, IdHeader)] =
    extractRequestContext.flatMap[Tuple1[(HttpRequest, IdHeader)]] { ctx =>
      provide(ctx.request -> ctx.request.header[IdHeader].getOrElse {
        val header = IdHeader(UUID.randomUUID.toString)
        header
      })
    }

  private def addRequestId(id: IdHeader): Directive0 =
    mapRequest { request =>
      request.copy(
        headers =
        id +:
          request.headers
      )
    }

  private def addResponseId(id: IdHeader): Directive0 =
    mapResponseHeaders { headers =>
      id +:
        headers
    }

  private def bagAndTag(req: HttpRequest, id: IdHeader, start: Long): Directive0 =
    mapResponse { response =>
      requestLogger.request(id.value(), System.currentTimeMillis() - start, response.status.toString(), req)
      response
    }
}
