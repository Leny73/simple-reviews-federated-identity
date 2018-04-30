package org.simplereviews.controllers

import akka.http.scaladsl.model.StatusCodes.MethodNotAllowed
import akka.http.scaladsl.model.headers.Allow
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ MethodRejection, RejectionHandler }

trait WithRejectionHandler {
  implicit val handler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handleAll[MethodRejection] { rejections =>
        lazy val methods = rejections.map(_.supported)
        lazy val names = methods.map(_.name).mkString(", ")

        respondWithHeader(Allow(methods)) {
          options {
            complete(s"Supported methods : $names")
          } ~
            complete(
              MethodNotAllowed,
              s"HTTP method not allowed, supported methods: $names"
            )
        }
      }.result()
}
