package org.simplereviews.controllers.directives

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.igl.jwt._

import org.simplereviews.controllers.directives.AuthenticationDirectives.{ AuthenticationClaims, ClientClaims }
import org.simplereviews.models.{ Id, JWT, Token }
import org.simplereviews.models.definitions.{ RawJwt, Service, UserInformation }
import org.simplereviews.models.dto.{ Client, User }
import org.simplereviews.persistence.TokenStore

import org.byrde.commons.controllers.actions.auth.definitions.{ Admin, Org }
import org.byrde.commons.utils.auth.JsonWebTokenWrapper
import org.byrde.commons.utils.auth.conf.JwtConfig

import akka.http.scaladsl.model.RemoteAddress
import akka.http.scaladsl.server.{ Directive, Directive0, Directive1, Route }
import akka.http.scaladsl.server.Directives._

import scala.reflect.ClassTag
import scala.util.{ Failure, Success, Try }
import scala.util.matching.Regex

//TODO: Fill out all reject functions
trait AuthenticationDirectives extends PlayJsonSupport {
  val Bearer: Regex =
    "(^Bearer) (.+)".r

  def tokenStore: TokenStore

  def clients: Map[Id, Client]

  def jwtConfig: JwtConfig

  def isClientAuthenticated(services: Service*)(route: Route): Route =
    extractClientAuthenticationClaims { clientClaims =>
      validateClientPermissions(clientClaims.clientId, clientClaims.clientToken, services: _*)(route)
    }

  def isUserAuthenticated(requiresAdmin: Boolean, services: Service*)(route: Jwt with UserInformation => Route): Route =
    extractUserAuthenticationClaims { claims =>
      validateClientPermissions(claims.clientClaims.clientId, claims.clientClaims.clientToken, services: _*) {
        validateTokenPermissions(claims.ip, claims.jwt, requiresAdmin)(route)
      }
    }

  def issueJwt(ip: RemoteAddress, user: User): JWT = {
    val claims =
      Seq(
        Sub(user.id.toString),
        Org(user.organizationId.toString),
        Admin(user.isAdmin.toString)
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

  private def extractUserAuthenticationClaims: Directive1[AuthenticationClaims] =
    for {
      ip <- extractClientIP
      jwt <- headerValueByName(jwtConfig.tokenName)
      clientClaims <- extractClientAuthenticationClaims
    } yield AuthenticationClaims(ip, jwt, clientClaims)

  private def validateClientPermissions(clientId: Id, clientToken: Token, services: Service*): Directive0 =
    Directive { inner =>
      clients
        .get(clientId)
        .filter(_.token == clientToken)
        .map(client => services.filterNot(Service.meetsPermissionCriteria(client, _)))
        .map { unmetPermissionCriteria =>
          if (unmetPermissionCriteria.isEmpty)
            inner()
          else
            reject()
        }
        .getOrElse(reject())
    }

  private def validateTokenPermissions(ip: RemoteAddress, jwt: JWT, requiresAdmin: Boolean): Directive1[Jwt with UserInformation] =
    jwt match {
      case Bearer(_, raw) =>
        JsonWebTokenWrapper(jwtConfig.copy(saltOpt = salt(ip))).decode(raw) match {
          case Success(innerJwt) if !isTokenExpired(innerJwt) =>
            verifyTokenStore(generateJwtWithRawJwt(raw, innerJwt), requiresAdmin)
          case Success(innerJwt) if isTokenExpired(innerJwt) =>
            reject()
          case Failure(ex) =>
            reject()
        }
      case _ =>
        reject()
    }

  private def isTokenExpired(jwt: Jwt) =
    jwt.getClaim[Exp] match {
      case Some(exp) if exp.value < System.currentTimeMillis() =>
        true
      case _ =>
        false
    }

  private def verifyTokenStore(jwt: Jwt with RawJwt, requiresAdmin: Boolean): Directive1[Jwt with UserInformation] =
    extractClaimsFromJwt(jwt) match {
      case (Some(userId), Some(orgId), Some(isAdmin)) if !requiresAdmin || requiresAdmin && isAdmin =>
        onSuccess(tokenStore.tokenExistsForUser(userId, jwt.raw)) flatMap { exists =>
          if (!exists)
            reject()
          else
            provide(generateJwtWithUserInformation(userId, orgId, isAdmin, jwt))
        }
      case (Some(_), Some(_), Some(isAdmin)) if requiresAdmin && !isAdmin =>
        reject()
      case (_, _, _) =>
        reject()
    }

  private def extractClaimsFromJwt(jwt: Jwt): (Option[Id], Option[Id], Option[Boolean]) =
    (jwt.getClaim[Sub].flatMap(x => Try(x.value.toLong).toOption), jwt.getClaim[Org].flatMap(x => Try(x.value.toLong).toOption), jwt.getClaim[Admin].flatMap(x => Try(x.value.toBoolean).toOption))

  private def generateJwtWithRawJwt(_raw: JWT, jwt: Jwt): Jwt with RawJwt =
    new Jwt with RawJwt {
      override def getClaim[T <: ClaimValue](implicit evidence$2: ClassTag[T]): Option[T] =
        jwt.getClaim

      override def encodedAndSigned(secret: String): String =
        jwt.encodedAndSigned(secret)

      override def getHeader[T <: HeaderValue](implicit evidence$1: ClassTag[T]): Option[T] =
        jwt.getHeader

      override def raw: JWT =
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

object AuthenticationDirectives {
  private case class ClientClaims(clientId: Id, clientToken: Token)
  private case class AuthenticationClaims(ip: RemoteAddress, jwt: JWT, clientClaims: ClientClaims)
}
