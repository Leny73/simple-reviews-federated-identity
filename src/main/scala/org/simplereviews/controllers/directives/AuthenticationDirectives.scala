package org.simplereviews.controllers.directives

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.igl.jwt.{ Exp, Jwt, Sub }

import org.byrde.commons.controllers.actions.auth.definitions.{ Admin, Org }
import org.byrde.commons.utils.auth.JsonWebTokenWrapper
import org.byrde.commons.utils.auth.conf.JwtConfig
import org.simplereviews.models.exceptions.ServiceResponseException

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{ optionalHeaderValueByName, provide }

import scala.util.Try
import scala.util.matching.Regex

trait AuthenticationDirectives extends PlayJsonSupport {
  type JWT = String

  private val Bearer: Regex =
    "(^Bearer) (.+)".r

  def isAdmin(jwt: Jwt): Directive1[Jwt] = {
    val innerIsAdmin =
      jwt.getClaim[Admin].fold(false) { admin =>
        Try(admin.value.toBoolean).toOption.fold(false)(isAdmin => isAdmin)
      }

    if (innerIsAdmin) {
      provide(jwt)
    } else {
      throw ServiceResponseException.E0403
    }
  }

  def isAuthenticatedAndAdmin(jwtConfig: JwtConfig): Directive1[Jwt] =
    isAuthenticated(jwtConfig) flatMap { jwt =>
      isAdmin(jwt)
    }

  def isAuthenticatedAndPartOfOrganization(org: Long, jwtConfig: JwtConfig): Directive1[Jwt] =
    isAuthenticated(jwtConfig) flatMap { jwt =>
      isSameOrganization(org, jwt)
    }

  def isAuthenticatedAndAdminAndPartOfOrganization(org: Long, jwtConfig: JwtConfig): Directive1[Jwt] =
    isAuthenticatedAndAdmin(jwtConfig) flatMap { jwt =>
      isSameOrganization(org, jwt)
    }

  def isAuthenticatedAndPartOfOrganizationAndSameUser(org: Long, acc: Long, jwtConfig: JwtConfig): Directive1[Jwt] =
    isAuthenticated(jwtConfig) flatMap { jwt =>
      isSameOrganization(org, jwt) flatMap { jwt =>
        isSameUser(acc, jwt)
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
      throw ServiceResponseException.E0403
    }
  }

  def isSameUser(acc: Long, jwt: Jwt): Directive1[Jwt] = {
    val isSameUser =
      jwt.getClaim[Sub].fold(false) { x =>
        Try(x.value.toLong).toOption.fold(false)(_ == acc)
      }

    if (isSameUser) {
      provide(jwt)
    } else {
      throw ServiceResponseException.E0403
    }
  }

  def isAuthenticated(jwtConfig: JwtConfig): Directive1[Jwt] =
    optionalHeaderValueByName(jwtConfig.tokenName)
      .flatMap {
        _.flatMap {
          case Bearer(_, raw) =>
            JsonWebTokenWrapper(jwtConfig).decode(raw).toOption match {
              case Some(jwt) if !isTokenExpired(jwt) =>
                Some(jwt)
              case _ =>
                Option.empty[Jwt]
            }
          case _ =>
            Option.empty[Jwt]
        }.fold(throw ServiceResponseException.E0401)(provide)
      }

  private def isTokenExpired(jwt: Jwt) =
    jwt.getClaim[Exp] match {
      case Some(exp) if exp.value < System.currentTimeMillis() =>
        true
      case _ =>
        false
    }
}
