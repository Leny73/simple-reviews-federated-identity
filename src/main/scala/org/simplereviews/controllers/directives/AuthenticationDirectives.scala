package org.simplereviews.controllers.directives

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.igl.jwt.{ Exp, Jwt, Sub }

import org.byrde.commons.controllers.actions.auth.definitions.{ Admin, Org }
import org.byrde.commons.models.services.CommonsServiceResponseDictionary._
import org.byrde.commons.utils.auth.JsonWebTokenWrapper
import org.byrde.commons.utils.auth.conf.JwtConfig

import akka.http.scaladsl.model.RemoteAddress
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._

import scala.util.{ Failure, Success, Try }
import scala.util.matching.Regex

trait AuthenticationDirectives extends PlayJsonSupport {
  type JWT = String

  val Bearer: Regex =
    "(^Bearer) (.+)".r

  def isAuthenticatedWithSalt(jwtConfig: JwtConfig): Directive1[Jwt] =
    extractClientIP flatMap { ip =>
      isAuthenticated(jwtConfig.copy(saltOpt = salt(ip)))
    }

  def isAdmin(jwt: Jwt): Directive1[Jwt] = {
    val innerIsAdmin =
      jwt.getClaim[Admin].fold(false) { admin =>
        Try(admin.value.toBoolean).toOption.fold(false)(isAdmin => isAdmin)
      }

    if (innerIsAdmin) {
      provide(jwt)
    } else {
      throw E0403
    }
  }

  def isAuthenticatedAndAdmin(jwtConfig: JwtConfig): Directive1[Jwt] =
    isAuthenticatedWithSalt(jwtConfig) flatMap { jwt =>
      isAdmin(jwt)
    }

  def isAuthenticatedAndSameUser(user: Long, jwtConfig: JwtConfig): Directive1[Jwt] =
    isAuthenticatedWithSalt(jwtConfig) flatMap { jwt =>
      isSameUser(user, jwt)
    }

  def isAuthenticatedAndPartOfOrganization(org: Long, jwtConfig: JwtConfig): Directive1[Jwt] =
    isAuthenticatedWithSalt(jwtConfig) flatMap { jwt =>
      isSameOrganization(org, jwt)
    }

  def isAuthenticatedAndAdminAndPartOfOrganization(org: Long, jwtConfig: JwtConfig): Directive1[Jwt] =
    isAuthenticatedAndAdmin(jwtConfig) flatMap { jwt =>
      isSameOrganization(org, jwt)
    }

  def isAuthenticatedAndPartOfOrganizationAndSameUser(org: Long, user: Long, jwtConfig: JwtConfig): Directive1[Jwt] =
    isAuthenticatedWithSalt(jwtConfig) flatMap { jwt =>
      isSameOrganization(org, jwt) flatMap { jwt =>
        isSameUser(user, jwt)
      }
    }

  def isSameOrganization(org: Long, jwt: Jwt): Directive1[Jwt] = {
    val isSameOrganization =
      jwt.getClaim[Org].fold(false) { x =>
        Try(x.value.toLong).toOption.fold(false)(_ == org)
      }

    if (isSameOrganization) {
      provide(jwt)
    } else {
      throw E0403
    }
  }

  def isSameUser(user: Long, jwt: Jwt): Directive1[Jwt] = {
    val isSameUser =
      jwt.getClaim[Sub].fold(false) { x =>
        Try(x.value.toLong).toOption.fold(false)(_ == user)
      }

    if (isSameUser) {
      provide(jwt)
    } else {
      throw E0403
    }
  }

  protected def salt(remoteAddress: RemoteAddress): Option[String] =
    remoteAddress.toOption.map(_.getHostAddress)

  private def isAuthenticated(jwtConfig: JwtConfig): Directive1[Jwt] =
    extractRequest flatMap { req =>
      optionalHeaderValueByName(jwtConfig.tokenName)
        .flatMap {
          _.flatMap {
            case Bearer(_, raw) =>
              JsonWebTokenWrapper(jwtConfig).decode(raw) match {
                case Success(jwt) if !isTokenExpired(jwt) =>
                  Some(jwt)
                case Failure(ex) =>
                  Option.empty[Jwt]
              }
            case _ =>
              Option.empty[Jwt]
          }.fold(throw E0401)(provide)
        }
    }

  private def isTokenExpired(jwt: Jwt) =
    jwt.getClaim[Exp] match {
      case Some(exp) if exp.value < System.currentTimeMillis() =>
        true
      case _ =>
        false
    }
}
