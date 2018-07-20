package org.simplereviews.controllers.support

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import org.simplereviews.controllers.{ Authentication, Organization, User }
import org.simplereviews.logger.impl.ErrorLogger

import org.byrde.commons.models.services.CommonsServiceResponseDictionary.E0500
import org.byrde.commons.utils.exception.{ ClientException, ServiceResponseException }

import play.api.libs.json.Json

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpRequest, HttpResponse }
import akka.http.scaladsl.model.headers.Allow
import akka.http.scaladsl.server.Directives.{ complete, options, respondWithHeader }
import akka.http.scaladsl.server.{ ExceptionHandler, MethodRejection, RejectionHandler }

trait ExceptionHandlingSupport extends PlayJsonSupport with CORSSupport {
  def errorLogger: ErrorLogger

  private val ClientError =
    1

  //Use RejectionHandler for all client errors
  implicit val handler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handleAll[MethodRejection] { rejections =>
        lazy val methods =
          rejections
            .map(_.supported)

        lazy val names =
          methods
            .map(_.name)
            .mkString(", ")

        respondWithHeader(Allow(methods)) {
          options {
            cors {
              complete(s"Supported methods : $names")
            }
          } ~ complete(MethodNotAllowed -> s"HTTP method not allowed, supported methods: $names")
        }
      }
      .result()
      .withFallback(Authentication.handler)
      .withFallback(Organization.handler)
      .withFallback(User.handler)
      .withFallback(RejectionHandler.default)
      .mapRejectionResponse {
        case res @ HttpResponse(_status, _, ent: HttpEntity.Strict, _) if ent.contentType != ContentTypes.`application/json` =>
          val status =
            _status.intValue

          val message =
            ent
              .data
              .utf8String

          val clientException =
            ClientException(message, ClientError, status)

          errorLogger.error(clientException)

          res.copy(entity =
            HttpEntity(
              ContentTypes.`application/json`,
              Json.stringify(clientException.toJson)
            ))

        case response =>
          response
      }

  //Use ExceptionHandler for all server errors
  def exceptionHandler(req: HttpRequest): ExceptionHandler =
    ExceptionHandler {
      case exception: Throwable =>
        val serviceException =
          exception match {
            case serviceException: ServiceResponseException[_] =>
              errorLogger.error(serviceException, req)
              serviceException
            case _ =>
              errorLogger.error(exception, req)
              E0500(exception)
          }

        complete(Json.toJson(serviceException))
    }
}
