package org.simplereviews.controllers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.igl.jwt.Sub

import org.byrde.commons.controllers.actions.auth.definitions.Admin
import org.byrde.commons.controllers.actions.auth.definitions.Org
import org.byrde.commons.utils.auth.JsonWebTokenWrapper
import org.byrde.commons.utils.auth.conf.JwtConfig
import org.simplereviews.controllers.directives.AuthenticationDirectives
import org.simplereviews.controllers.requests.LoginRequest
import org.simplereviews.guice.Modules
import org.simplereviews.logger.impl.ApplicationLogger
import org.simplereviews.models.DefaultServiceResponse
import org.simplereviews.models.exceptions.ServiceResponseException

import play.api.libs.json.Json

import akka.http.scaladsl.model.{ RemoteAddress, StatusCodes }
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{ HttpRequestWithEntity, MarshallingEntityWithRequestDirective }
import akka.http.scaladsl.server.Route

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class Account(val modules: Modules)(implicit ec: ExecutionContext) extends PlayJsonSupport with MarshallingEntityWithRequestDirective with AuthenticationDirectives {
  val logger: ApplicationLogger =
    modules.applicationLogger

  private val jwtConfig: JwtConfig =
    modules.configuration.jwtConfiguration

  lazy val routes: Route =
    authenticate ~ authenticated

  private def authenticate: Route =
    path("authenticate") {
      post {
        extractClientIP { ip =>
          requestEntityUnmarshallerWithEntity(unmarshaller[LoginRequest]) { request =>
            onComplete(login(ip)(request)) {
              case Success(jwt) if jwt.nonEmpty =>
                respondWithHeader(RawHeader(jwtConfig.tokenName, s"Bearer ${jwt.get}")) {
                  complete(StatusCodes.OK, Json.toJson(DefaultServiceResponse.success("Success")))
                }
              case Failure(ex) =>
                throw ServiceResponseException.E0401.copy(_msg = ex.getMessage)
            }
          }
        }
      }
    }

  private def authenticated: Route =
    path("authenticated") {
      get {
        isAuthenticatedWithSalt(jwtConfig) { _ =>
          complete(StatusCodes.OK, Json.toJson(DefaultServiceResponse.success("Success")))
        }
      }
    }

  private def login(remoteAddress: RemoteAddress)(implicit request: HttpRequestWithEntity[LoginRequest]): Future[Option[JWT]] =
    modules.persistence.usersDAO.findByEmailAndPasswordAndOrganization(request.body.email, request.body.password, request.body.organization).map {
      _.map { user =>
        val claims =
          Seq(Sub(user.id.toString), Org(user.organizationId.toString), Admin(user.isAdmin.toString))

        JsonWebTokenWrapper(jwtConfig.copy(saltOpt = salt(remoteAddress))).encode(claims)
      }
    }
}
