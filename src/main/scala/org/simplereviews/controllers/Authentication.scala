package org.simplereviews.controllers

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import org.byrde.commons.models.services.CommonsServiceResponseDictionary._
import org.byrde.commons.utils.FutureUtils._
import org.simplereviews.controllers.directives.{ AccountSupport, ApiSupport }
import org.simplereviews.controllers.requests.{ ForgotPasswordRequest, SignInRequest }
import org.simplereviews.guice.Modules
import org.simplereviews.logger.impl.ApplicationLogger

import play.api.libs.json.Json

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MarshallingEntityWithRequestDirective
import akka.http.scaladsl.server.Route

import scala.concurrent.ExecutionContext

class Authentication(val modules: Modules)(implicit val ec: ExecutionContext) extends PlayJsonSupport with ApiSupport with AccountSupport with MarshallingEntityWithRequestDirective {
  lazy val routes: Route =
    signIn ~ signedIn ~ forgotPassword

  def signIn: Route =
    path("sign-in") {
      post {
        extractClientIP { ip =>
          requestEntityUnmarshallerWithEntity(unmarshaller[SignInRequest]) { request =>
            async[JWT]({
              FutureTry2FutureConversion {
                authenticate(ip, request.body)
              }
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
        requestEntityUnmarshallerWithEntity(unmarshaller[ForgotPasswordRequest]) { request =>
          asyncJson {
            FutureTry2FutureConversion {
              resetPassword(request.body)
            }
          }
        }
      }
    }
}
