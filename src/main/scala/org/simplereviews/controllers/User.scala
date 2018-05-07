package org.simplereviews.controllers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import org.byrde.commons.models.services.CommonsServiceResponseDictionary._
import org.byrde.commons.utils.auth.conf.JwtConfig
import org.byrde.commons.utils.FutureUtils._
import org.byrde.commons.utils.TryUtils._
import org.simplereviews.controllers.directives.{ ApiSupport, AuthenticationDirectives }
import org.simplereviews.controllers.requests.{ ChangePasswordRequest, UpdateUserRequest }
import org.simplereviews.guice.Modules
import org.simplereviews.logger.impl.ApplicationLogger

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MarshallingEntityWithRequestDirective

import scala.concurrent.{ ExecutionContext, Future }

class User(val modules: Modules)(implicit ec: ExecutionContext) extends PlayJsonSupport with ApiSupport with MarshallingEntityWithRequestDirective with AuthenticationDirectives {
  val jwtConfig: JwtConfig =
    modules.configuration.jwtConfiguration

  lazy val routes: Route =
    pathPrefix(LongNumber) { userId =>
      isAuthenticatedAndSameUser(userId, jwtConfig) { _ =>
        path("change-password") {
          put {
            requestEntityUnmarshallerWithEntity(unmarshaller[ChangePasswordRequest]) { request =>
              changePassword(userId, request.body)
            }
          }
        } ~ get {
          getUser(userId)
        } ~ put {
          requestEntityUnmarshallerWithEntity(unmarshaller[UpdateUserRequest]) { request =>
            updateUser(userId, request.body)
          }
        }
      }
    }

  private def getUser(userId: Long): Route =
    asyncJson {
      FutureTry2FutureConversion {
        modules.persistence.usersDAO.findById(userId).map {
          case Some(user) =>
            user.!+
          case None =>
            E0404(s"User $userId does not exist").!-
        }
      }
    }

  private def changePassword(userId: Long, password: ChangePasswordRequest): Route =
    asyncJson {
      modules.persistence.usersDAO.findByIdAndPassword(userId, password.currentPassword).flatMap {
        case Some(_) if password.newPassword != password.verifyNewPassword =>
          Future.failed(E0400("New password doesn't match password verification"))
        case Some(_) =>
          FutureTry2FutureConversion {
            modules.persistence.usersDAO.findByIdAndPassword(userId, password.currentPassword).flatMap {
              case None =>
                Future.failed(E0400("Invalid password"))
              case Some(_) =>
                modules.persistence.usersDAO.updatePassword(userId, password.newPassword) map {
                  case Some(user) =>
                    user.!+
                  case None =>
                    E0404(s"User $userId does not exist").!-
                }
            }
          }
        case _ =>
          Future.failed(E0400("Wrong password"))
      }
    }

  private def updateUser(userId: Long, updateUserRequest: UpdateUserRequest): Route =
    asyncJson {
      FutureTry2FutureConversion {
        modules.persistence.usersDAO.updateWithUpdateUserRequest(userId, updateUserRequest) map {
          case Some(user) =>
            user.!+
          case None =>
            E0404(s"User $userId does not exist").!-
        }
      }
    }
}
