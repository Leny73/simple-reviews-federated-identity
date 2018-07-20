package org.simplereviews.controllers.support

import java.util.UUID

import org.simplereviews.logger.impl.RequestLogger

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

trait RequestResponseHandlingSupport extends ExceptionHandlingSupport {
  def requestLogger: RequestLogger

  def requestResponseHandler(route: Route): Route =
    cors {
      requestId {
        case (request, id) =>
          handleExceptions(exceptionHandler(request)) {
            addRequestId(id) {
              addResponseId(id) {
                val start =
                  System.currentTimeMillis

                bagAndTag(request, id, start) {
                  route
                }
              }
            }
          }
      }
    }

  private def requestId: Directive1[(HttpRequest, IdHeader)] =
    extractRequestContext.flatMap[Tuple1[(HttpRequest, IdHeader)]] { ctx =>
      provide(
        ctx.request ->
          ctx
          .request
          .header[IdHeader]
          .getOrElse {
            IdHeader(UUID.randomUUID.toString)
          }
      )
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
      requestLogger
        .request(
          id.value(),
          System.currentTimeMillis() - start,
          response.status.toString(),
          req
        )

      response
    }
}
