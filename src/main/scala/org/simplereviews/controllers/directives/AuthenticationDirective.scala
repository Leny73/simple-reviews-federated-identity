package org.simplereviews.controllers.directives

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import io.igl.jwt.{ Exp, Jwt }

import org.byrde.commons.utils.auth.JsonWebTokenWrapper
import org.byrde.commons.utils.auth.conf.JwtConfig
import org.simplereviews.models.exceptions.ServiceResponseException

import akka.http.scaladsl.model.RemoteAddress
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{ optionalHeaderValueByName, provide }

import scala.util.matching.Regex

trait AuthenticationDirective extends PlayJsonSupport {
  private val Bearer: Regex =
    "(^Bearer) (.+)".r

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

  def salt(remoteAddress: RemoteAddress): Option[String] =
    remoteAddress.toOption.map(_.getHostAddress)

  private def isTokenExpired(jwt: Jwt) =
    jwt.getClaim[Exp] match {
      case Some(exp) if exp.value < System.currentTimeMillis() =>
        true
      case _ =>
        false
    }
}
