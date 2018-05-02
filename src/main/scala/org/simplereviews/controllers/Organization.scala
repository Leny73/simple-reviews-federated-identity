package org.simplereviews.controllers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import org.byrde.commons.utils.auth.conf.JwtConfig
import org.simplereviews.controllers.directives.AuthenticationDirectives
import org.simplereviews.controllers.requests.UserRequest
import org.simplereviews.guice.Modules
import org.simplereviews.logger.impl.ApplicationLogger
import org.simplereviews.models.dto.User
import org.simplereviews.models.exceptions.ServiceResponseException

import play.api.libs.json.Json

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MarshallingEntityWithRequestDirective

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

class Organization(val modules: Modules)(implicit ec: ExecutionContext) extends PlayJsonSupport with MarshallingEntityWithRequestDirective with AuthenticationDirectives {
  val logger: ApplicationLogger =
    modules.applicationLogger

  private val jwtConfig: JwtConfig =
    modules.configuration.jwtConfiguration

  lazy val routes: Route =
    organization

  private def organization: Route =
    pathPrefix(LongNumber) { org =>
      isAuthenticatedAndAdminAndPartOfOrganization(org, jwtConfig) { _ =>
        path("users") {
          get {
            listUsers(org)
          }
        } ~ pathPrefix("user") {
          post {
            requestEntityUnmarshallerWithEntity(unmarshaller[UserRequest]) { request =>
              createUser(org, request.body)
            }
          }
        }
      }
    }

  private def listUsers(org: Long): Route =
    onComplete(modules.persistence.organizationUsersDAO.findByOrganization(org)) {
      case Success(users) =>
        complete(Json.toJson(users))
      case Failure(ex) =>
        throw ServiceResponseException.E0404.copy(_msg = ex.getMessage)
    }

  private def createUser(org: Long, userRequest: UserRequest): Route =
    onComplete({
      val (user, password) =
        User.create(org, userRequest)

      modules.persistence.usersDAO.upsert(user).map { user =>
        //TODO: Email new password.
        user
      }
    }) {
      case Success(user) =>
        complete(Json.toJson(user))
      case Failure(ex) =>
        throw ServiceResponseException.E0400.copy(_msg = ex.getMessage)
    }
}
