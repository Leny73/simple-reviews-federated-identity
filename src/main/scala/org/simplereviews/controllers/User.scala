package org.simplereviews.controllers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import org.byrde.commons.models.services.CommonsServiceResponseDictionary._
import org.byrde.commons.utils.auth.conf.JwtConfig
import org.byrde.commons.utils.TryUtils._
import org.byrde.commons.utils.FutureUtils._
import org.simplereviews.controllers.directives.{ ApiSupport, AuthenticationDirectives }
import org.simplereviews.controllers.requests.{ ChangePasswordRequest, UpdateUserRequest }
import org.simplereviews.guice.ModulesProvider

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.{ HttpRequestWithEntity, MarshallingEntityWithRequestDirective }

import scala.concurrent.{ ExecutionContext, Future }

class User(modulesProvider: ModulesProvider)(implicit val ec: ExecutionContext) extends PlayJsonSupport with ApiSupport with AuthenticationDirectives with MarshallingEntityWithRequestDirective {
  val jwtConfig: JwtConfig =
    modulesProvider.configuration.jwtConfiguration

  lazy val routes: Route =
    pathPrefix(LongNumber) { userId =>
      path("change-password") {
        put {
          isAuthenticatedAndSameUser(userId, jwtConfig) { _ =>
            requestEntityUnmarshallerWithEntity(unmarshaller[ChangePasswordRequest]) { implicit request =>
              changePassword(userId, request.body)
            }
          }
        }
      } ~ get {
        requestUnmarshallerWithEntity { implicit request =>
          getUser(userId)
        }
      } ~ put {
        isAuthenticatedAndSameUser(userId, jwtConfig) { _ =>
          requestEntityUnmarshallerWithEntity(unmarshaller[UpdateUserRequest]) { implicit request =>
            updateUser(userId, request.body)
          }
        }
      }
    }

  private def getUser[T](userId: Long)(implicit req: HttpRequestWithEntity[T]): Route =
    asyncJson {
      modulesProvider
        .persistence
        .UsersDAO
        .findById(userId)
        .map {
          case Some(user) =>
            user.!+
          case None =>
            E0404(s"User $userId does not exist").!-
        }.flattenTry
    }

  private def changePassword[T](userId: Long, password: ChangePasswordRequest)(implicit req: HttpRequestWithEntity[T]): Route =
    asyncJson {
      modulesProvider
        .persistence
        .UsersDAO
        .findByIdAndPassword(userId, password.currentPassword).flatMap {
          case Some(_) if password.newPassword != password.verifyNewPassword =>
            Future.failed(E0400("New password doesn't match password verification"))
          case Some(_) =>
            modulesProvider.persistence.UsersDAO.findByIdAndPassword(userId, password.currentPassword).flatMap {
              case None =>
                Future.failed(E0400("Invalid password"))
              case Some(_) =>
                modulesProvider.persistence.UsersDAO.updatePassword(userId, password.newPassword) map {
                  case Some(user) =>
                    user.!+
                  case None =>
                    E0404(s"User $userId does not exist").!-
                }
            }.flattenTry
          case _ =>
            Future.failed(E0400("Wrong password"))
        }
    }

  private def updateUser[T](userId: Long, updateUserRequest: UpdateUserRequest)(implicit req: HttpRequestWithEntity[T]): Route =
    asyncJson {
      modulesProvider
        .persistence
        .UsersDAO
        .updateWithUpdateUserRequest(userId, updateUserRequest)
        .map {
          case Some(user) =>
            user.!+
          case None =>
            E0404(s"User $userId does not exist").!-
        }.flattenTry
    }
}
