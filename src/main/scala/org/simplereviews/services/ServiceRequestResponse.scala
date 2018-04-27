package org.simplereviews.services

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.directives.HttpRequestWithEntity

case class ServiceRequestResponse[T](
  service: NameForLogging,
  epoch: Long,
  originalRequest: HttpRequestWithEntity[_],
  request: HttpRequestWithEntity[T],
  response: HttpResponse
)
