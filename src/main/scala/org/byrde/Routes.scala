package org.byrde

import challenge.guice.Modules

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.{ MarshallingEntityWithRequestDirective, RequestResponseHandlingDirective }
import akka.util.Timeout

trait Routes extends RequestResponseHandlingDirective with MarshallingEntityWithRequestDirective {
  def modules: Modules

  implicit lazy val timeout: Timeout =
    modules.configuration.timeout

  implicit def system: ActorSystem =
    modules.akka.actorSystem

  lazy val defaultRoutes: Route =
    ???

  lazy val pathBindings =
    Map(
      "api" -> defaultRoutes
    )

  lazy val routes: Route =
    requestResponseHandler {
      pathBindings.map {
        case (k, v) => path(k)(v)
      } reduce (_ ~ _)
    }
}
