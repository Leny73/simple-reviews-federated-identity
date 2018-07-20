package org.simplereviews.controllers.support

import org.simplereviews.configuration.CORSConfiguration

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.respondWithHeaders
import akka.http.scaladsl.server.Route

trait CORSSupport {
  val corsConfiguration: CORSConfiguration

  val origins: Seq[RawHeader] =
    corsConfiguration.origins.map {
      RawHeader("Access-Control-Allow-Origin", _)
    }

  val allowedMethods: Seq[RawHeader] =
    corsConfiguration.methods.map {
      RawHeader("Access-Control-Allow-Methods", _)
    }

  val allowHeaders: Seq[RawHeader] =
    corsConfiguration.headers.map {
      RawHeader("Access-Control-Allow-Headers", _)
    }

  val exposeHeaders: Seq[RawHeader] =
    corsConfiguration.headers.map {
      RawHeader("Access-Control-Expose-Headers", _)
    }

  def cors(route: Route): Route =
    respondWithHeaders(
      origins ++
        allowedMethods ++
        allowHeaders ++
        exposeHeaders: _*
    )(route)
}
