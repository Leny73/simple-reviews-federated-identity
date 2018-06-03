package org.simplereviews.controllers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import org.byrde.commons.models.services.CommonsServiceResponseDictionary._
import org.simplereviews.controllers.directives.{ AccountSupport, ApiSupport, AuthenticationDirectives }
import org.simplereviews.controllers.requests.CreateUserRequest
import org.simplereviews.guice.Modules

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MarshallingEntityWithRequestDirective

import scala.concurrent.ExecutionContext

class Organization(val modules: Modules)(implicit val ec: ExecutionContext) extends PlayJsonSupport with ApiSupport with AccountSupport with MarshallingEntityWithRequestDirective with AuthenticationDirectives {
  lazy val routes: Route =
    organization

  private def organization: Route =
    pathPrefix(LongNumber) { organizationId =>
      isAuthenticatedAndAdminAndPartOfOrganization(organizationId, jwtConfig) { _ =>
        path("users") {
          get {
            listUsers(organizationId)
          }
        } ~ pathPrefix("user") {
          post {
            requestEntityUnmarshallerWithEntity(unmarshaller[CreateUserRequest]) { request =>
              createUser(organizationId, request.body)
            }
          } ~ path(LongNumber) { userId =>
            delete {
              deleteUser(userId)
            }
          }
        }
      }
    }

  private def listUsers(organizationId: Long): Route =
    asyncJson({
      modules.persistence.usersDAO.findByOrganization(organizationId)
    }, Err = E0404.apply)

  private def createUser(organizationId: Long, createUserRequest: CreateUserRequest): Route =
    asyncWithDefaultResponse({
      createAccount(organizationId, createUserRequest)
    }, E0200)

  private def deleteUser(userId: Long): Route =
    asyncWithDefaultResponse({
      modules.persistence.usersDAO.deleteById(userId)
    }, E0200)
}
