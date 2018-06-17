package org.simplereviews.controllers

import org.byrde.commons.models.services.CommonsServiceResponseDictionary._
import org.simplereviews.controllers.directives.RequestResponseHandlingDirective
import org.simplereviews.guice.ModulesProvider

import play.api.libs.json.Json

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MarshallingEntityWithRequestDirective
import akka.util.Timeout

import scala.concurrent.ExecutionContext

trait Routes extends RequestResponseHandlingDirective with MarshallingEntityWithRequestDirective {
  def modulesProvider: ModulesProvider

  implicit def ec: ExecutionContext

  implicit def system: ActorSystem

  implicit def timeout: Timeout

  lazy val defaultRoutes: Route =
    get {
      complete(OK -> Json.toJson(E0200("Pong!")))
    }

  lazy val pathBindings =
    Map(
      "ping" -> defaultRoutes,
      "auth" -> new Authentication(modulesProvider).routes,
      "org" -> new Organization(modulesProvider).routes,
      "user" -> new User(modulesProvider).routes
    )

  lazy val routes: Route =
    requestResponseHandler {
      pathBindings.map {
        case (k, v) => pathPrefix(k)(v)
      } reduce (_ ~ _)
    }
}
