package org.simplereviews.controllers

import org.postgresql.util.PSQLException
import org.simplereviews.controllers.Organization.UserWithEmailAlreadyExists
import org.simplereviews.controllers.requests.CreateUserRequest
import org.simplereviews.controllers.support.RouteSupport
import org.simplereviews.guice.ModulesProvider
import org.simplereviews.models.Service.Org
import org.simplereviews.models.exceptions.RejectionException
import org.simplereviews.models.{ Permission, Service, dto }

import org.byrde.commons.models.services.CommonsServiceResponseDictionary.E0200
import org.byrde.commons.utils.FutureUtils._
import org.byrde.commons.utils.TryUtils._
import org.byrde.commons.utils.auth.conf.JwtConfig

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ RejectionHandler, Route }
import akka.http.scaladsl.server.directives.MarshallingEntityWithRequestDirective

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

class Organization(val modulesProvider: ModulesProvider)(implicit val ec: ExecutionContext) extends RouteSupport with MarshallingEntityWithRequestDirective {
  lazy val routes: Route =
    listUsers ~ createUser

  val jwtConfig: JwtConfig =
    modulesProvider.configuration.jwtConfiguration

  def listUsers: Route =
    path("users") {
      get {
        Authentication.isUserAuthenticated(requiresAdmin = true, jwtConfig) { token =>
          asyncJson(handleListUsers(token.orgId))
        }(modulesProvider)
      }
    }

  def createUser: Route =
    path("user") {
      post {
        Authentication.isUserAuthenticated(requiresAdmin = true, jwtConfig, Service.Org(Permission.Writes)) { token =>
          requestEntityUnmarshallerWithEntity(unmarshaller[CreateUserRequest]) { implicit request =>
            async(handleCreateUser(token.orgId, request.body).flattenTry, { user: dto.User =>
              complete(ToResponseMarshallable.apply(user)(marshaller(dto.User.writesWithPasswordFlag(writePassword = true))))
            })
          }
        }(modulesProvider)
      }
    }

  def deleteUser(): Route =
    path(LongNumber) { userId =>
      delete {
        Authentication.isUserAuthenticated(requiresAdmin = true, jwtConfig, Service.Org(Permission.Writes)) { _ =>
          asyncWithDefaultJsonResponse(handleDeleteUser(userId), E0200)
        }(modulesProvider)
      }
    }

  private def handleListUsers(organizationId: Long): Future[Seq[dto.User]] =
    modulesProvider
      .persistence
      .UsersDAO
      .findByOrganization(organizationId)

  private def handleCreateUser(organizationId: Long, createUserRequest: CreateUserRequest): Future[Try[dto.User]] = {
    val (user, generatedPassword) =
      dto.User.create(
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
      .map { user =>
        user.copy(password = generatedPassword).!+
      }
      .recoverWith {
        case ex: PSQLException if ex.getMessage.contains("duplicate key value") =>
          Future.failed(UserWithEmailAlreadyExists(createUserRequest.email))
        case ex =>
          throw ex
      }
  }

  private def handleDeleteUser(userId: Long): Future[Int] =
    modulesProvider
      .persistence
      .UsersDAO
      .deleteByIds(userId)
}

object Organization {
  final case class UserWithEmailAlreadyExists(email: String)
    extends RejectionException

  val handler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case UserWithEmailAlreadyExists(email) =>
          complete((BadRequest, s"User with email: $email already exists"))
      }
      .result()
}