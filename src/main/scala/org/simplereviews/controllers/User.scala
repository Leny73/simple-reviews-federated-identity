package org.simplereviews.controllers

import org.simplereviews.controllers.User.{ InvalidPassword, PasswordDoesntMatch, UserDoesNotExist }
import org.simplereviews.controllers.requests.{ ChangePasswordRequest, UpdateUserRequest }
import org.simplereviews.controllers.support.RouteSupport
import org.simplereviews.guice.ModulesProvider
import org.simplereviews.models.exceptions.RejectionException
import org.simplereviews.models._

import org.byrde.commons.models.services.CommonsServiceResponseDictionary._
import org.byrde.commons.utils.TryUtils._
import org.byrde.commons.utils.FutureUtils._
import org.byrde.commons.utils.auth.conf.JwtConfig

import akka.http.scaladsl.model.RemoteAddress
import akka.http.scaladsl.model.StatusCodes.{ BadRequest, NotFound }
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ RejectionHandler, Route }
import akka.http.scaladsl.server.directives.MarshallingEntityWithRequestDirective

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

class User(val modulesProvider: ModulesProvider)(implicit val ec: ExecutionContext) extends RouteSupport with MarshallingEntityWithRequestDirective {
  lazy val routes: Route =
    getUser() ~ updateUser() ~ changePassword ~ path(LongNumber) { id =>
      getUser(Some(id)) ~ updateUser(Some(id))
    }

  val jwtConfig: JwtConfig =
    modulesProvider.configuration.jwtConfiguration

  def getUser(id: Option[Id] = None): Route =
    get {
      Authentication.isUserAuthenticated(requiresAdmin = id.isDefined, jwtConfig) { token =>
        asyncJson(handleGetUser(id.getOrElse(token.id)).flattenTry)
      }(modulesProvider)
    }

  def updateUser(id: Option[Id] = None): Route =
    put {
      Authentication.isUserAuthenticated(requiresAdmin = id.isDefined, jwtConfig) { token =>
        requestEntityUnmarshallerWithEntity(unmarshaller[UpdateUserRequest]) { implicit request =>
          asyncJson(handleUpdateUser(id.getOrElse(token.id), request.body).flattenTry)
        }
      }(modulesProvider)
    }

  def changePassword: Route =
    path("change-password") {
      put {
        Authentication.isUserAuthenticated(requiresAdmin = false, jwtConfig, Service.Org(Permission.Writes)) { token =>
          extractClientIP { ip =>
            requestEntityUnmarshallerWithEntity(unmarshaller[ChangePasswordRequest]) { implicit request =>
              async(handleChangePassword(ip, token.id, request.body).flattenTry, { newToken: Token =>
                respondWithHeader(RawHeader(jwtConfig.tokenName, s"Bearer $newToken")) {
                  complete(E0200)
                }
              })
            }
          }
        }(modulesProvider)
      }
    }

  private def handleGetUser[T](userId: Id): Future[Try[dto.User]] =
    modulesProvider
      .persistence
      .UsersDAO
      .findById(userId)
      .map {
        case Some(user) =>
          user.!+

        case None =>
          UserDoesNotExist.!-
      }

  private def handleUpdateUser[T](userId: Id, updateUserRequest: UpdateUserRequest): Future[Try[dto.User]] =
    modulesProvider
      .persistence
      .UsersDAO
      .updateWithUpdateUserRequest(userId, updateUserRequest)
      .map {
        case Some(user) =>
          user.!+

        case None =>
          UserDoesNotExist.!-
      }

  private def handleChangePassword[T](ip: RemoteAddress, userId: Long, password: ChangePasswordRequest): Future[Try[Token]] = {
    val query =
      modulesProvider
        .persistence
        .UsersDAO
        .findByIdAndPassword(userId, password.currentPassword)

    if (password.newPassword != password.verifyNewPassword)
      Future.failed(PasswordDoesntMatch)
    else
      query flatMap {
        case Some(_) =>
          modulesProvider
            .tokenStore
            .deleteTokensForUser(userId)

          modulesProvider
            .persistence.UsersDAO
            .updatePassword(userId, password.newPassword)
            .map { userOpt =>
              Authentication.issueJwt(ip, userOpt.get, jwtConfig).!+
            }

        case _ =>
          Future.failed(InvalidPassword)
      }
  }
}

object User {
  final case object PasswordDoesntMatch
    extends RejectionException

  final case object InvalidPassword
    extends RejectionException

  final case object UserDoesNotExist
    extends RejectionException

  val handler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case PasswordDoesntMatch =>
          complete((BadRequest, s"Your new passwords you entered do not match"))
      }
      .handle {
        case InvalidPassword =>
          complete((BadRequest, s"The password you entered does not match your current password"))
      }
      .handle {
        case UserDoesNotExist =>
          complete((NotFound, s"The user you are attempting to update does not exist"))
      }
      .result()
}
