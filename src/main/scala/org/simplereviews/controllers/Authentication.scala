package org.simplereviews.controllers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.igl.jwt.Sub

import org.byrde.commons.models.services.CommonsServiceResponseDictionary._
import org.byrde.commons.utils.TryUtils._
import org.byrde.commons.utils.FutureUtils._
import org.simplereviews.controllers.directives.{ ApiSupport, AuthenticationDirectives }
import org.simplereviews.controllers.requests.{ ForgotPasswordRequest, SignInRequest }
import org.simplereviews.guice.ModulesProvider

import org.byrde.commons.controllers.actions.auth.definitions.{ Admin, Org }
import org.byrde.commons.utils.auth.JsonWebTokenWrapper
import org.byrde.commons.utils.auth.conf.JwtConfig

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MarshallingEntityWithRequestDirective
import akka.http.scaladsl.server.Route

import scala.concurrent.{ ExecutionContext, Future }

class Authentication(modulesProvider: ModulesProvider)(implicit val ec: ExecutionContext) extends PlayJsonSupport with ApiSupport with AuthenticationDirectives with MarshallingEntityWithRequestDirective {
  lazy val routes: Route =
    signIn ~ signedIn ~ forgotPassword

  val jwtConfig: JwtConfig =
    modulesProvider.configuration.jwtConfiguration

  def signIn: Route =
    path("sign-in") {
      post {
        extractClientIP { ip =>
          requestEntityUnmarshallerWithEntity(unmarshaller[SignInRequest]) { implicit request =>
            val signInRequest =
              request.body

            val fn =
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
                    val claims =
                      Seq(Sub(user.id.toString), Org(user.organizationId.toString), Admin(user.isAdmin.toString))
                    JsonWebTokenWrapper(jwtConfig.copy(saltOpt = salt(ip))).encode(claims).!+
                  case None =>
                    E0400("Invalid organization, username, and password").!-
                }

            async[JWT]({
              fn.flattenTry
            }, jwt => {
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
        isAuthenticatedWithSalt(jwtConfig) { _ =>
          complete(E0200)
        }
      }
    }

  def forgotPassword: Route =
    path("forgot-password") {
      post {
        requestEntityUnmarshallerWithEntity(unmarshaller[ForgotPasswordRequest]) { implicit request =>
          asyncJson {
            val forgotPasswordRequest =
              request.body

            val fn =
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
                          .map(_.map(_.!+).getOrElse(E0400("Could not update password").!-))
                      case None =>
                        Future.failed(E0400("User is not part of an organization"))
                    }
                  case None =>
                    Future.failed(E0404("User does not exist"))
                }

            fn.flattenTry
          }
        }
      }
    }
}
