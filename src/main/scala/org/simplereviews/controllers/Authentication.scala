package org.simplereviews.controllers

import io.igl.jwt._

import org.simplereviews.controllers.Authentication.{ InvalidLoginCredentials, UserDoesNotExist }
import org.simplereviews.controllers.requests.{ ForgotPasswordRequest, SignInRequest }
import org.simplereviews.controllers.support.RouteSupport
import org.simplereviews.guice.ModulesProvider
import org.simplereviews.models.Service.Org
import org.simplereviews.models._
import org.simplereviews.models.exceptions.RejectionException
import org.simplereviews.persistence.TokenStore
import org.simplereviews.utils.KeyGenerator

import org.byrde.commons.models.services.CommonsServiceResponseDictionary._
import org.byrde.commons.utils.FutureUtils._
import org.byrde.commons.utils.TryUtils._
import org.byrde.commons.utils.auth.JsonWebTokenWrapper
import org.byrde.commons.utils.auth.conf.JwtConfig

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.RemoteAddress
import akka.http.scaladsl.model.StatusCodes.{ BadRequest, Forbidden, NotFound, Unauthorized }
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{ HttpRequestWithEntity, MarshallingEntityWithRequestDirective }

import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag
import scala.util.matching.Regex
import scala.util.{ Failure, Success, Try }

class Authentication(val modulesProvider: ModulesProvider)(implicit val ec: ExecutionContext) extends RouteSupport with MarshallingEntityWithRequestDirective {
  lazy val routes: Route =
    signedIn ~ signIn ~ forgotPassword

  val tokenStore: TokenStore =
    modulesProvider.tokenStore

  val jwtConfig: JwtConfig =
    modulesProvider.configuration.jwtConfiguration

  def signedIn: Route =
    path("signed-in") {
      get {
        Authentication.isUserAuthenticated(requiresAdmin = false, jwtConfig) { _ =>
          complete(E0200)
        }(modulesProvider)
      }
    }

  def signIn: Route =
    path("sign-in") {
      post {
        Authentication.isClientAuthenticated {
          extractClientIP { ip =>
            requestEntityUnmarshallerWithEntity(unmarshaller[SignInRequest]) { implicit request =>
              async(handleSignIn(ip, request).flattenTry, { token: String =>
                respondWithHeader(RawHeader(jwtConfig.tokenName, s"Bearer $token")) {
                  complete(E0200)
                }
              })
            }
          }
        }(modulesProvider)
      }
    }

  def forgotPassword: Route =
    path("forgot-password") {
      post {
        Authentication.isClientAuthenticated(Service.Org(Permission.Writes)) {
          requestEntityUnmarshallerWithEntity(unmarshaller[ForgotPasswordRequest]) { implicit request =>
            async(handleForgotPassword(request).flattenTry, { user: dto.User =>
              complete(ToResponseMarshallable.apply(user)(marshaller(dto.User.writesWithPasswordFlag(writePassword = true))))
            })
          }
        }(modulesProvider)
      }
    }

  private def handleSignIn(ip: RemoteAddress, request: HttpRequestWithEntity[SignInRequest]): Future[Try[String]] = {
    val query =
      modulesProvider
        .persistence
        .UsersDAO
        .findByEmailAndPasswordAndOrganization(
          request.body.email,
          request.body.password,
          request.body.organization
        )

    query map {
      case Some(user) =>
        val token =
          Authentication.issueJwt(ip, user, jwtConfig)

        modulesProvider
          .tokenStore
          .addTokenForUser(user.id, token)

        token.!+

      case None =>
        InvalidLoginCredentials.!-
    }
  }

  def handleForgotPassword(request: HttpRequestWithEntity[ForgotPasswordRequest]): Future[Try[dto.User]] = {
    val query =
      modulesProvider
        .persistence
        .UsersDAO
        .findByEmailAndAndOrganization(
          request.body.email,
          request.body.organization
        )

    query flatMap {
      case Some(user) =>
        val generatedPassword =
          KeyGenerator.generateKey

        modulesProvider
          .tokenStore
          .deleteTokensForUser(user.id)

        modulesProvider
          .persistence
          .UsersDAO
          .updatePassword(user.id, generatedPassword)
          .map(_.get.copy(password = generatedPassword).!+)

      case None =>
        Future.failed(UserDoesNotExist(request.body.email))
    }
  }
}

object Authentication {
  private case class ClientClaims(clientId: Id, clientToken: Token)

  private case class AuthenticationClaims(ip: RemoteAddress, jwt: Token, clientClaims: ClientClaims)

  private val Bearer: Regex =
    "(^Bearer) (.+)".r

  final case object InvalidLoginCredentials
    extends RejectionException

  final case class UserDoesNotExist(email: String)
    extends RejectionException

  final case class MissingClientPermissions(unmetServicesPermissions: Seq[Service])
    extends Rejection

  final case class InvalidClient(clientId: Id, clientToken: Token)
    extends Rejection

  final case object TokenExpired
    extends Rejection

  final case class TokenFailedValidation(cause: Throwable)
    extends Rejection

  final case object NoTokenReference
    extends Rejection

  final case object MissingTokenClaims
    extends Rejection

  val handler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case InvalidLoginCredentials =>
          complete((Unauthorized, s"Invalid email, organization, and password"))
      }
      .handle {
        case UserDoesNotExist(email) =>
          complete((NotFound, s"User with email: $email not found"))
      }
      .handle {
        case MissingClientPermissions(unmetServicesPermissions) =>
          complete((Forbidden, s"The supplied client is not authorized to access these resources: (${unmetServicesPermissions.map(service => s"${service.name} 🡒 ${service.permission.value}").mkString(", ")})"))
      }
      .handle {
        case InvalidClient(clientId, clientToken) =>
          complete((Unauthorized, s"The supplied client id and client token do not match our records: ($clientId, $clientToken)"))
      }
      .handle {
        case TokenExpired =>
          complete((Unauthorized, "Token is expired."))
      }
      .handle {
        case TokenFailedValidation(ex) =>
          complete((Unauthorized, s"Token failed validation: ${ex.getMessage}"))
      }
      .handle {
        case NoTokenReference =>
          complete((Unauthorized, "The supplied token does not match our records"))
      }
      .handle {
        case NoTokenReference =>
          complete((Unauthorized, "The supplied token does not match our records"))
      }
      .handle {
        case AuthorizationFailedRejection =>
          complete((Forbidden, "The supplied token is not authorized to access these resources"))
      }
      .handle {
        case MissingTokenClaims =>
          complete((Unauthorized, "The supplied token is not valid"))
      }
      .result()

  def isClientAuthenticated(route: Route)(implicit modulesProvider: ModulesProvider): Route =
    isClientAuthenticated()(route)(modulesProvider)

  def isClientAuthenticated(services: Service*)(route: Route)(implicit modulesProvider: ModulesProvider): Route =
    extractClientAuthenticationClaims { clientClaims =>
      validateClientPermissions(clientClaims, services: _*)(modulesProvider)(route)
    }

  def isUserAuthenticated(requiresAdmin: Boolean, jwtConfig: JwtConfig, services: Service*)(route: Jwt with UserInformation => Route)(implicit modulesProvider: ModulesProvider): Route =
    extractUserAuthenticationClaims(jwtConfig) { claims =>
      validateClientPermissions(claims.clientClaims, services: _*)(modulesProvider) {
        validateTokenPermissions(claims.ip, claims.jwt, requiresAdmin, jwtConfig)(modulesProvider)(route)
      }
    }

  def issueJwt(ip: RemoteAddress, user: dto.User, jwtConfig: JwtConfig): Token = {
    val claims =
      Seq(
        Sub(user.id.toString),
        org.byrde.commons.controllers.actions.auth.definitions.Org(user.organizationId.toString),
        org.byrde.commons.controllers.actions.auth.definitions.Admin(user.isAdmin.toString)
      )

    JsonWebTokenWrapper(jwtConfig.copy(saltOpt = salt(ip))).encode(claims)
  }

  private def salt(remoteAddress: RemoteAddress): Option[String] =
    remoteAddress
      .toOption
      .map(_.getHostAddress)

  private def extractClientAuthenticationClaims: Directive1[ClientClaims] =
    for {
      clientId <- headerValueByName("X-Client-Id").map(_.toLong)
      clientToken <- headerValueByName("X-Client-Token")
    } yield ClientClaims(clientId, clientToken)

  private def extractUserAuthenticationClaims(jwtConfig: JwtConfig): Directive1[AuthenticationClaims] =
    for {
      ip <- extractClientIP
      jwt <- headerValueByName(jwtConfig.tokenName)
      clientClaims <- extractClientAuthenticationClaims
    } yield AuthenticationClaims(ip, jwt, clientClaims)

  private def validateClientPermissions(clientClaims: ClientClaims, services: Service*)(implicit modulesProvider: ModulesProvider): Directive0 =
    Directive { inner =>
      modulesProvider
        .clients
        .get(clientClaims.clientId)
        .filter(_.token == clientClaims.clientToken)
        .map(client => services.filterNot(Service.meetsPermissionCriteria(client, _)))
        .map { unmetPermissionCriteria =>
          if (unmetPermissionCriteria.isEmpty)
            inner(())
          else
            reject(MissingClientPermissions(unmetPermissionCriteria))
        }
        .getOrElse(reject(InvalidClient(clientClaims.clientId, clientClaims.clientToken)))
    }

  private def validateTokenPermissions(ip: RemoteAddress, jwt: Token, requiresAdmin: Boolean, jwtConfig: JwtConfig)(implicit modulesProvider: ModulesProvider): Directive1[Jwt with UserInformation] =
    jwt match {
      case Bearer(_, raw) =>
        JsonWebTokenWrapper(jwtConfig.copy(saltOpt = salt(ip))).decode(raw) match {
          case Success(innerJwt) if !isTokenExpired(innerJwt) =>
            verifyTokenStore(generateJwtWithRawJwt(raw, innerJwt), requiresAdmin)
          case Success(innerJwt) if isTokenExpired(innerJwt) =>
            reject(TokenExpired)
          case Failure(ex) =>
            reject(TokenFailedValidation(ex))
        }
      case _ =>
        reject(MalformedHeaderRejection(jwtConfig.tokenName, jwt))
    }

  private def isTokenExpired(jwt: Jwt) =
    jwt.getClaim[Exp] match {
      case Some(exp) if exp.value < System.currentTimeMillis() =>
        true
      case _ =>
        false
    }

  private def verifyTokenStore(jwt: Jwt with RawJwt, requiresAdmin: Boolean)(implicit modulesProvider: ModulesProvider): Directive1[Jwt with UserInformation] =
    extractClaimsFromJwt(jwt) match {
      case (Some(userId), Some(orgId), Some(isAdmin)) if !requiresAdmin || requiresAdmin && isAdmin =>
        onSuccess(modulesProvider.tokenStore.tokenExistsForUser(userId, jwt.raw)) flatMap { exists =>
          if (!exists)
            reject(NoTokenReference)
          else
            provide(generateJwtWithUserInformation(userId, orgId, isAdmin, jwt))
        }
      case (Some(_), Some(_), Some(isAdmin)) if requiresAdmin && !isAdmin =>
        reject(AuthorizationFailedRejection)
      case (_, _, _) =>
        reject(MissingTokenClaims)
    }

  private def extractClaimsFromJwt(jwt: Jwt): (Option[Id], Option[Id], Option[Boolean]) =
    (
      jwt.getClaim[Sub].flatMap(x => Try(x.value.toLong).toOption),
      jwt.getClaim[org.byrde.commons.controllers.actions.auth.definitions.Org].flatMap(x => Try(x.value.toLong).toOption),
      jwt.getClaim[org.byrde.commons.controllers.actions.auth.definitions.Admin].flatMap(x => Try(x.value.toBoolean).toOption)
    )

  private def generateJwtWithRawJwt(_raw: Token, jwt: Jwt): Jwt with RawJwt =
    new Jwt with RawJwt {
      override def getClaim[T <: ClaimValue](implicit evidence$2: ClassTag[T]): Option[T] =
        jwt.getClaim

      override def encodedAndSigned(secret: String): String =
        jwt.encodedAndSigned(secret)

      override def getHeader[T <: HeaderValue](implicit evidence$1: ClassTag[T]): Option[T] =
        jwt.getHeader

      override def raw: Token =
        _raw
    }

  private def generateJwtWithUserInformation(_id: Id, _orgId: Id, _isAdmin: Boolean, jwt: Jwt): Jwt with UserInformation =
    new Jwt with UserInformation {
      override def getClaim[T <: ClaimValue](implicit evidence$2: ClassTag[T]): Option[T] =
        jwt.getClaim

      override def encodedAndSigned(secret: String): String =
        jwt.encodedAndSigned(secret)

      override def getHeader[T <: HeaderValue](implicit evidence$1: ClassTag[T]): Option[T] =
        jwt.getHeader

      override def id: Id =
        _id

      override def orgId: Id =
        _orgId

      override def isAdmin: Boolean =
        _isAdmin
    }
}
