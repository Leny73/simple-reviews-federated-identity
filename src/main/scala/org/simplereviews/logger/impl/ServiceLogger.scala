package org.simplereviews.logger.impl

import org.simplereviews.guice.Modules
import org.simplereviews.logger.{ Logger, LoggingInformation }
import org.simplereviews.services.{ NameForLogging, ServiceRequestResponse }

import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.directives.HttpRequestWithEntity
import akka.stream.Materializer

import scala.concurrent.{ ExecutionContext, Future }

class ServiceLogger(modules: Modules) extends Logger {
  override protected val logger: LoggingAdapter =
    Logging(modules.akka.system, getClass)

  def logService[T](
    service: NameForLogging,
    serviceRequest: HttpRequestWithEntity[T],
    originalRequest: HttpRequestWithEntity[_]
  )(fn: HttpRequestWithEntity[T] => Future[HttpResponse])(implicit materializer: Materializer, ec: ExecutionContext, loggingInformation: LoggingInformation[ServiceRequestResponse[T]]): Future[HttpResponse] = {
    val start: Long = System.currentTimeMillis()

    fn(serviceRequest).map { serviceResponse =>
      serviceRequest.request.discardEntityBytes()

      info(
        ServiceRequestResponse(
          service,
          System.currentTimeMillis() - start,
          originalRequest,
          serviceRequest,
          serviceResponse
        )
      )

      serviceResponse
    }
  }
}