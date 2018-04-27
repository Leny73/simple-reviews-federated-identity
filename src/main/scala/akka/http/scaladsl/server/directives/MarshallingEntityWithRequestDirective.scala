package akka.http.scaladsl.server.directives

import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives.{ cancelRejections, extractRequestContext, provide }
import akka.http.scaladsl.server.directives.FutureDirectives.onComplete
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, FromRequestUnmarshaller, Unmarshaller }

import scala.util.{ Failure, Success }

trait MarshallingEntityWithRequestDirective {
  def requestWithEntity[T](um: FromEntityUnmarshaller[T]): Directive1[HttpRequestWithEntity[T]] =
    extractRequestContext.flatMap[Tuple1[HttpRequestWithEntity[T]]] { ctx ⇒
      import ctx.{ executionContext, materializer }

      onComplete(um(ctx.request.entity).map(_ -> ctx.request)) flatMap {
        case Success((value, req)) =>
          provide(HttpRequestWithEntity[T](value, req))
        case Failure(RejectionError(r)) =>
          reject(r)
        case Failure(Unmarshaller.NoContentException) =>
          reject(RequestEntityExpectedRejection)
        case Failure(Unmarshaller.UnsupportedContentTypeException(x)) =>
          reject(UnsupportedRequestContentTypeRejection(x))
        case Failure(x: IllegalArgumentException) =>
          reject(ValidationRejection(x.getMessage, Some(x)))
        case Failure(x) =>
          reject(MalformedRequestContentRejection(x.getMessage, x))
      }
    } & cancelRejections(RequestEntityExpectedRejection.getClass, classOf[UnsupportedRequestContentTypeRejection])
}
