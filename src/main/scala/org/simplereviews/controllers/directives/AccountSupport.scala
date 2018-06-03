package org.simplereviews.controllers.directives

import io.igl.jwt.Sub

import org.byrde.commons.controllers.actions.auth.definitions.{ Admin, Org }
import org.byrde.commons.models.services.CommonsServiceResponseDictionary.E0400
import org.byrde.commons.utils.auth.JsonWebTokenWrapper
import org.byrde.commons.utils.auth.conf.JwtConfig
import org.byrde.commons.utils.TryUtils._
import org.simplereviews.controllers.requests.{ CreateUserRequest, ForgotPasswordRequest, SignInRequest }
import org.simplereviews.guice.Modules
import org.simplereviews.models.dto.User
import org.simplereviews.models.services.responses.Ack

import akka.http.scaladsl.model.RemoteAddress

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

trait AccountSupport extends AuthenticationDirectives {
  implicit def ec: ExecutionContext

  def modules: Modules

  val jwtConfig: JwtConfig =
    modules.configuration.jwtConfiguration

  def authenticate(remoteAddress: RemoteAddress, signInRequest: SignInRequest): Future[Try[JWT]] =
    modules
      .persistence
      .usersDAO
      .findByEmailAndPasswordAndOrganization(
        signInRequest.email,
        signInRequest.password,
        signInRequest.organization
      )
      .map {
        case Some(user) =>
          val claims =
            Seq(Sub(user.id.toString), Org(user.organizationId.toString), Admin(user.isAdmin.toString))
          JsonWebTokenWrapper(jwtConfig.copy(saltOpt = salt(remoteAddress))).encode(claims).!+
        case None =>
          E0400("Invalid username and password").!-
      }

  def resetPassword(forgotPasswordRequest: ForgotPasswordRequest): Future[Try[Ack]] =
    modules
      .persistence
      .usersDAO
      .findByEmailAndAndOrganization(
        forgotPasswordRequest.email,
        forgotPasswordRequest.organization
      )
      .flatMap {
        case Some(user) =>
          val password =
            User.generatePassword

          modules.persistence.organizationsDAO.findById(user.organizationId).flatMap {
            case Some(organization) =>
              for {
                _ <- modules.persistence.usersDAO.updatePassword(user.id, password)
                _ <- sendPasswordEmail(user.email, organization.name, user.name, password)
              } yield {
                Ack.!+
              }
            case None =>
              Future.failed(E0400("User is not part of an organization"))
          }
        case None =>
          Future.failed(E0400("User does not exist"))
      }

  def createAccount(organizationId: Long, createUserRequest: CreateUserRequest): Future[Try[User]] =
    modules
      .persistence
      .organizationsDAO
      .findById(organizationId)
      .flatMap {
        case Some(organization) =>
          val (user, password) =
            User.create(
              organizationId,
              createUserRequest.email,
              createUserRequest.firstName,
              createUserRequest.lastName,
              createUserRequest.isAdmin
            )

          for {
            newUser <- modules.persistence.usersDAO.insertAndInsertOrganizationUserRow(user)
            _ <- sendPasswordEmail(user.email, organization.name, user.name, password)
          } yield newUser.!+
        case None =>
          Future.failed(E0400("Invalid organization"))
      }

  private def sendPasswordEmail(email: String, organization: String, name: String, password: String): Future[Try[Ack]] =
    Future(Try(modules.emailService.sendMessage(email, s"Welcome to $organization", org.simplereviews.templates.html.PasswordEmail(name, organization, email, password).render))).map {
      case Success(_) =>
        Ack.!+
      case Failure(ex) =>
        ex.!-
    }
}
