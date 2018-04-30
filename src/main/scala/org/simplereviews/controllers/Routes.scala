package org.simplereviews.controllers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import org.simplereviews.controllers.directives.RequestResponseHandlingDirective
import org.simplereviews.guice.Modules
import org.simplereviews.models.DefaultServiceResponse

import play.api.libs.json.Json

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Allow
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{MethodRejection, RejectionHandler, Route}
import akka.http.scaladsl.server.directives.MarshallingEntityWithRequestDirective
import akka.util.Timeout

import scala.concurrent.ExecutionContext

trait Routes extends PlayJsonSupport with RequestResponseHandlingDirective with MarshallingEntityWithRequestDirective {
  def modules: Modules

  implicit val timeout: Timeout =
    modules.configuration.timeout

  implicit val system: ActorSystem =
    modules.akka.system

  implicit val ec: ExecutionContext =
    system.dispatcher

  implicit val rejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
    .handleAll[MethodRejection] { rejections =>
      val methods = rejections map (_.supported)
      lazy val names = methods map (_.name) mkString ", "

      respondWithHeader(Allow(methods)) {
        options {
          complete(s"Supported methods : $names.")
        } ~
          complete(MethodNotAllowed,
            s"HTTP method not allowed, supported methods: $names!")
      }
    }
    .result()

  lazy val defaultRoutes: Route =
    get {
      complete(OK -> Json.toJson(DefaultServiceResponse.success("Pong!")))
    }

  lazy val pathBindings =
    Map(
      "ping" -> defaultRoutes,
      "account" -> new Account(modules).routes
    )

  lazy val routes: Route =
    requestResponseHandler {
      pathBindings.map {
        case (k, v) => pathPrefix(k)(v)
      } reduce (_ ~ _)
    }
}
