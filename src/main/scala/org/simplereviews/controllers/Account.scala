package org.simplereviews.controllers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.igl.jwt.Sub

import org.byrde.commons.utils.auth.JsonWebTokenWrapper
import org.byrde.commons.utils.auth.conf.JwtConfig
import org.simplereviews.controllers.requests.LoginRequest
import org.simplereviews.guice.Modules
import org.simplereviews.models.DefaultServiceResponse
import org.simplereviews.models.exceptions.ServiceResponseException

import play.api.libs.json.Json

import akka.http.scaladsl.model.{ RemoteAddress, StatusCodes }
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{ HttpRequestWithEntity, MarshallingEntityWithRequestDirective }
import akka.http.scaladsl.server.Route

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Success

class Account(modules: Modules)(implicit ec: ExecutionContext) extends PlayJsonSupport with MarshallingEntityWithRequestDirective {
  type JWT = String

  val jwtConfig: JwtConfig =
    modules.configuration.jwtConfiguration

  lazy val routes: Route =
    path("login") {
      post {
        extractClientIP { ip =>
          requestWithEntity(unmarshaller[LoginRequest]) { request =>
            onComplete(login(ip)(request)) {
              case Success(jwt) if jwt.nonEmpty =>
                respondWithHeader(RawHeader("Set-Cookie", s"${jwtConfig.tokenName}=${jwt.get}")) {
                  complete(StatusCodes.OK, Json.toJson(DefaultServiceResponse.success("Success")))
                }
              case _ =>
                throw ServiceResponseException.E0401
            }
          }
        }
      }
    }

  protected def login(remoteAddress: RemoteAddress)(implicit request: HttpRequestWithEntity[LoginRequest]): Future[Option[JWT]] =
    modules.persistence.userDAO.findByUsernameAndPassword(request.body.username, request.body.password).map {
      _.map { user =>
        val claims =
          Seq(Sub(user.id.toString))

        val salt =
          remoteAddress.toOption.map(_.getHostAddress)

        JsonWebTokenWrapper(
          jwtConfig.copy(saltOpt = salt)
        ).encode(claims)
      }
    }
}
