package org.simplereviews.controllers

import org.byrde.commons.models.services.CommonsServiceResponseDictionary._
import org.byrde.commons.utils.TryUtils._
import org.byrde.commons.utils.FutureUtils._
import org.simplereviews.controllers.directives.{ ApiSupport, AuthenticationDirectives }
import org.simplereviews.controllers.requests.{ ForgotPasswordRequest, SignInRequest }
import org.simplereviews.guice.ModulesProvider
import org.simplereviews.models.dto.Client
import org.simplereviews.models.{ Id, JWT }
import org.simplereviews.persistence.TokenStore

import org.byrde.commons.utils.auth.conf.JwtConfig

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MarshallingEntityWithRequestDirective
import akka.http.scaladsl.server.Route

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

class Authentication()(implicit val modulesProvider: ModulesProvider, val clients: Map[Id, Client], val ec: ExecutionContext) extends ApiSupport with AuthenticationDirectives with MarshallingEntityWithRequestDirective {
  lazy val routes: Route =
    signIn ~ signedIn ~ forgotPassword

  val tokenStore: TokenStore =
    modulesProvider.tokenStore

  val jwtConfig: JwtConfig =
    modulesProvider.configuration.jwtConfiguration

  def signIn: Route =
    path("sign-in") {
      post {
        extractClientIP { ip =>
          requestEntityUnmarshallerWithEntity(unmarshaller[SignInRequest]) { implicit request =>
            val signInRequest =
              request.body

            val fn: Future[Try[String]] =
              modulesProvider
                .persistence
                .UsersDAO
                .findByEmailAndPasswordAndOrganization(
                  signInRequest.email,
                  signInRequest.password,
                  signInRequest.organization
                )
                .map {
                  case Some(user) =>
                    issueJwt(ip, user).!+
                  case None =>
                    E0400("Invalid organization, username, and password").!-
                }

            async[JWT](fn.flattenTry, jwt => {
              respondWithHeader(RawHeader(jwtConfig.tokenName, s"Bearer $jwt")) {
                complete(E0200)
              }
            }, E0401.apply)
          }
        }
      }
    }

  def signedIn: Route =
    path("signed-in") {
      get {
        isUserAuthenticated(requiresAdmin = false) { _ =>
          complete(E0200)
        }
      }
    }

  def forgotPassword: Route =
    path("forgot-password") {
      post {
        isClientAuthenticated(Nil: _*) {
          requestEntityUnmarshallerWithEntity(unmarshaller[ForgotPasswordRequest]) { implicit request =>
            asyncJson {
              val forgotPasswordRequest =
                request.body

              modulesProvider
                .persistence
                .UsersDAO
                .findByEmailAndAndOrganization(
                  forgotPasswordRequest.email,
                  forgotPasswordRequest.organization
                )
                .flatMap {
                  case Some(user) =>
                    val password =
                      org.simplereviews.models.dto.User.generatePassword

                    modulesProvider.persistence.OrganizationsDAO.findById(user.organizationId).flatMap {
                      case Some(_) =>
                        modulesProvider
                          .persistence
                          .UsersDAO
                          .updatePassword(user.id, password)
                          .map { userOpt =>
                            userOpt
                              .map(_.!+)
                              .getOrElse(E0400("Could not update password").!-)
                          }
                      case None =>
                        Future.failed(E0400("User is not part of an organization"))
                    }
                  case None =>
                    Future.failed(E0404("User does not exist"))
                }
                .flattenTry
            }
          }
        }
      }
    }
}
