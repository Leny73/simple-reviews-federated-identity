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
      path("users") {
        get {
          isAuthenticatedAndAdminAndPartOfOrganization(organizationId, jwtConfig) { _ =>
            listUsers(organizationId)
          }
        }
      } ~ pathPrefix("user") {
        post {
          isAuthenticatedAndAdminAndPartOfOrganization(organizationId, jwtConfig) { _ =>
            requestEntityUnmarshallerWithEntity(unmarshaller[CreateUserRequest]) { request =>
              asyncWithDefaultResponse({
                createUser(organizationId, request.body)
              }, E0200)
            }
          }
        } ~ path(LongNumber) { userId =>
          delete {
            isAuthenticatedAndAdminAndPartOfOrganization(organizationId, jwtConfig) { _ =>
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

  private def deleteUser(userId: Long): Route =
    asyncWithDefaultResponse({
      modules.persistence.usersDAO.deleteById(userId)
    }, E0200)
}
