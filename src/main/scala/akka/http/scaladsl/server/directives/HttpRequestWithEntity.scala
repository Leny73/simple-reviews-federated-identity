package akka.http.scaladsl.server.directives

import akka.http.scaladsl.model.HttpRequest

case class HttpRequestWithEntity[T](body: T, request: HttpRequest)

object HttpRequestWithEntity {
  def info[T](emptyValue: T): HttpRequestWithEntity[T] =
    HttpRequestWithEntity(emptyValue, HttpRequest())
}
