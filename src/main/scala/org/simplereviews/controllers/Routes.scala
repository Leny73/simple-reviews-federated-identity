package org.simplereviews.controllers

import org.byrde.commons.models.services.CommonsServiceResponseDictionary._
import org.simplereviews.controllers.directives.RequestResponseHandlingDirective
import org.simplereviews.guice.ModulesProvider
import org.simplereviews.models.Id
import org.simplereviews.models.dto.Client

import play.api.libs.json.Json

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MarshallingEntityWithRequestDirective
import akka.util.Timeout

import scala.concurrent.ExecutionContext

//TODO: Update routes to no longer accept path parameters
trait Routes extends RequestResponseHandlingDirective with MarshallingEntityWithRequestDirective {
  implicit def modulesProvider: ModulesProvider

  implicit def ec: ExecutionContext

  implicit def system: ActorSystem

  implicit def timeout: Timeout

  implicit def clients: Map[Id, Client]

  lazy val defaultRoutes: Route =
    get {
      complete(E0200("Pong!"))
    }

  lazy val pathBindings =
    Map(
      "ping" -> defaultRoutes,
      "auth" -> new Authentication().routes,
      "org" -> new Organization().routes,
      "user" -> new User().routes
    )

  lazy val routes: Route =
    requestResponseHandler {
      pathBindings.map {
        case (k, v) => pathPrefix(k)(v)
      } reduce (_ ~ _)
    }
}
