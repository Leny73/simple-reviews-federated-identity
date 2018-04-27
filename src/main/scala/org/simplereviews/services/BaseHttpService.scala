package org.simplereviews.services

import org.simplereviews.logger.impl.ServiceLogger

import akka.actor.ActorSystem
import akka.http.scaladsl.{ Http, HttpExt }
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.directives.{ HttpRequestWithEntity, HttpRequestWithNoEntity }
import akka.stream.ActorMaterializer

import scala.concurrent.{ ExecutionContext, Future }

trait BaseHttpService extends NameForLogging {
  protected def serviceLogger: ServiceLogger

  implicit protected def system: ActorSystem

  implicit protected def materializer: ActorMaterializer

  implicit protected def executionContext: ExecutionContext

  protected val http: HttpExt

  def get[T](path: String, fn: HttpResponse => Future[T], secure: Boolean = true)(implicit request: HttpRequestWithEntity[_]): Future[T] =
    executeRequest(HttpRequestWithNoEntity(HttpMethods.GET, s"${protocol(secure)}$host$path")).flatMap(fn)

  private def executeRequest[T](request: HttpRequestWithEntity[T])(implicit originalRequest: HttpRequestWithEntity[_]): Future[HttpResponse] =
    serviceLogger.logService(this, request, originalRequest)(request => http.singleRequest(request.request))

  private def protocol(secure: Boolean): String =
    if (secure)
      "https://"
    else
      "http://"
}