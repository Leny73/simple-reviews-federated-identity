package org.simplereviews.controllers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import org.simplereviews.controllers.directives.{ RequestResponseHandlingDirective, RejectionHandlerDirective }
import org.simplereviews.guice.Modules
import org.simplereviews.models.DefaultServiceResponse

import play.api.libs.json.Json

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MarshallingEntityWithRequestDirective
import akka.util.Timeout

import scala.concurrent.ExecutionContext

trait Routes extends PlayJsonSupport with RequestResponseHandlingDirective with MarshallingEntityWithRequestDirective with RejectionHandlerDirective {
  def modules: Modules

  implicit val timeout: Timeout =
    modules.configuration.timeout

  implicit val system: ActorSystem =
    modules.akka.system

  implicit val ec: ExecutionContext =
    system.dispatcher

  lazy val defaultRoutes: Route =
    get {
      complete(OK -> Json.toJson(DefaultServiceResponse.success("Pong!")))
    }

  lazy val pathBindings =
    Map(
      "ping" -> defaultRoutes,
      "account" -> new Account(modules).routes,
      "images" -> new Images(modules).routes
    )

  lazy val routes: Route =
    requestResponseHandler {
      pathBindings.map {
        case (k, v) => pathPrefix(k)(v)
      } reduce (_ ~ _)
    }
}
