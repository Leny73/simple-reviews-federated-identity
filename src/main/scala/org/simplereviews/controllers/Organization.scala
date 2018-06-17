package org.simplereviews.controllers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import org.byrde.commons.models.services.CommonsServiceResponseDictionary._
import org.byrde.commons.utils.TryUtils._
import org.simplereviews.controllers.directives.{ ApiSupport, AuthenticationDirectives }
import org.simplereviews.controllers.requests.CreateUserRequest
import org.simplereviews.guice.ModulesProvider

import org.byrde.commons.utils.auth.conf.JwtConfig

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.{ HttpRequestWithEntity, MarshallingEntityWithRequestDirective }

import scala.concurrent.{ ExecutionContext, Future }

class Organization(modulesProvider: ModulesProvider)(implicit val ec: ExecutionContext) extends PlayJsonSupport with ApiSupport with AuthenticationDirectives with MarshallingEntityWithRequestDirective {
  lazy val routes: Route =
    organization

  val jwtConfig: JwtConfig =
    modulesProvider.configuration.jwtConfiguration

  private def organization: Route =
    pathPrefix(LongNumber) { organizationId =>
      path("users") {
        get {
          isAuthenticatedAndAdminAndPartOfOrganization(organizationId, jwtConfig) { _ =>
            requestUnmarshallerWithEntity { implicit request =>
              listUsers(organizationId)
            }
          }
        }
      } ~ pathPrefix("user") {
        post {
          isAuthenticatedAndAdminAndPartOfOrganization(organizationId, jwtConfig) { _ =>
            requestEntityUnmarshallerWithEntity(unmarshaller[CreateUserRequest]) { implicit request =>
              createUser(organizationId, request.body)
            }
          }
        } ~ path(LongNumber) { userId =>
          delete {
            isAuthenticatedAndAdminAndPartOfOrganization(organizationId, jwtConfig) { _ =>
              requestUnmarshallerWithEntity { implicit request =>
                deleteUser(userId)
              }
            }
          }
        }
      }
    }

  private def listUsers[T](organizationId: Long)(implicit req: HttpRequestWithEntity[T]): Route =
    asyncJson({
      modulesProvider
        .persistence
        .UsersDAO
        .findByOrganization(organizationId)
    }, Err = E0404.apply)

  private def createUser[T](organizationId: Long, createUserRequest: CreateUserRequest)(implicit req: HttpRequestWithEntity[T]) = {
    val fn =
      modulesProvider
        .persistence
        .OrganizationsDAO
        .findById(organizationId)
        .flatMap {
          case Some(organization) =>
            val (user, password) =
              org.simplereviews.models.dto.User.create(
                organizationId,
                createUserRequest.email,
                createUserRequest.firstName,
                createUserRequest.lastName,
                createUserRequest.isAdmin
              )

            modulesProvider
              .persistence
              .UsersDAO
              .insertAndInsertOrganizationUserRow(user)
              .map(_.!+)
          case None =>
            Future.failed(E0400("Invalid organization"))
        }

    asyncWithDefaultResponse(fn, E0200)
  }

  private def deleteUser[T](userId: Long)(implicit req: HttpRequestWithEntity[T]): Route =
    asyncWithDefaultResponse({
      modulesProvider
        .persistence
        .UsersDAO
        .deleteById(userId)
    }, E0200)
}
