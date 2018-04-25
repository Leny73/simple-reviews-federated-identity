package org.simplereviews

import org.simplereviews.guice.Modules
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import org.simplereviews.models.DefaultServiceResponse

import play.api.libs.json.Json

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{ MarshallingEntityWithRequestDirective, RequestResponseHandlingDirective }
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.util.Timeout

trait Routes extends PlayJsonSupport with RequestResponseHandlingDirective with MarshallingEntityWithRequestDirective {
  def modules: Modules

  implicit lazy val timeout: Timeout =
    modules.configuration.timeout

  implicit def system: ActorSystem =
    modules.akka.actorSystem

  lazy val defaultRoutes: Route =
    requestResponseHandler {
      path("ping") {
        get {
          val response =
            new DefaultServiceResponse {
              override def msg: String =
                "Hello World!"

              override def code: Int =
                200

              override def status: Int =
                200
            }

          complete(OK -> Json.toJson(response))
        }
      }
    }

  lazy val pathBindings =
    Map(
      "api" -> defaultRoutes
    )

  lazy val routes: Route =
    requestResponseHandler {
      pathBindings.map {
        case (k, v) => pathPrefix(k)(v)
      } reduce (_ ~ _)
    }
}
