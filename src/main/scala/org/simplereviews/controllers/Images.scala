package org.simplereviews.controllers

import org.byrde.commons.utils.auth.conf.JwtConfig
import org.simplereviews.controllers.directives.AuthenticationDirectives
import org.simplereviews.guice.Modules
import org.simplereviews.logger.impl.ApplicationLogger
import org.simplereviews.models.DefaultServiceResponse
import org.simplereviews.models.exceptions.ServiceResponseException
import org.simplereviews.utils.OptionUtils

import akka.http.scaladsl.server.directives.CachingDirectives._
import akka.http.caching.scaladsl.{ Cache, CachingSettings, LfuCacheSettings }
import akka.http.caching.LfuCache
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MarshallingEntityWithRequestDirective
import akka.http.scaladsl.server.{ RequestContext, Route, RouteResult }
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.impl.S3Headers
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

class Images(val modules: Modules)(implicit ec: ExecutionContext) extends MarshallingEntityWithRequestDirective with AuthenticationDirectives {
  private implicit val materializer: ActorMaterializer =
    modules.akka.materializer

  val logger: ApplicationLogger =
    modules.applicationLogger

  private val jwtConfig: JwtConfig =
    modules.configuration.jwtConfiguration

  private val imageBucket: String =
    modules.configuration.imageBucket

  private val defaultCachingSettings: CachingSettings =
    CachingSettings(modules.akka.system)

  private val lfuCacheSettings: LfuCacheSettings =
    defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(100)
      .withMaxCapacity(1000)
      .withTimeToIdle(60.seconds)

  private val cachingSettings: CachingSettings =
    defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

  private val lfuCache: Cache[Uri, RouteResult] =
    LfuCache(cachingSettings)

  private val keyer: PartialFunction[RequestContext, Uri] = {
    val isGet: RequestContext => Boolean =
      _.request.method == HttpMethods.GET

    PartialFunction {
      case r: RequestContext if isGet(r) =>
        r.request.uri
    }
  }

  private val filename: String =
    "image"

  lazy val routes: Route =
    images

  private def images: Route =
    pathPrefix("organization" / LongNumber) { org =>
      get {
        isAuthenticatedAndPartOfOrganization(org, jwtConfig) { _ =>
          cache(lfuCache, keyer) {
            download(buildOrganizationPath(org, filename))
          }
        }
      } ~ post {
        isAuthenticatedAndAdminAndPartOfOrganization(org, jwtConfig) { _ =>
          fileUpload(filename) {
            case (metadata, byteSource) if metadata.contentType.mediaType == `image/png` || metadata.contentType.mediaType == `image/jpeg` =>
              upload(buildOrganizationPath(org, filename), byteSource, metadata.contentType)
            case _ =>
              throw ServiceResponseException.E0404.copy(_msg = "Unsupported file type")
          }
        }
      } ~ path("account" / LongNumber) { acc =>
        get {
          isAuthenticatedAndPartOfOrganization(org, jwtConfig) { _ =>
            cache(lfuCache, keyer) {
              download(buildAccountPath(org, acc, filename))
            }
          }
        } ~ post {
          isAuthenticatedAndPartOfOrganizationAndSameUser(acc, org, jwtConfig) { _ =>
            fileUpload(filename) {
              case (metadata, byteSource) if metadata.contentType.mediaType == `image/png` || metadata.contentType.mediaType == `image/jpeg` =>
                upload(buildAccountPath(org, acc, filename), byteSource, metadata.contentType)
              case _ =>
                throw ServiceResponseException.E0404.copy(_msg = "Unsupported file type")
            }
          }
        }
      }
    }

  private def buildOrganizationPath(org: Long, file: String): String =
    s"$org/$file"

  private def buildAccountPath(org: Long, acc: Long, file: String): String =
    s"$org/$acc/$file"

  def download(key: String): Route =
    onComplete(modules.s3Client.download(imageBucket, key) match {
      case (source, metaFuture) =>
        metaFuture.map {
          case meta if meta.contentLength > 0 =>
            val contentType =
              meta
                .contentType
                .flatMap {
                  ContentType
                    .parse(_)
                    .fold(_ => Option.empty[ContentType], OptionUtils.Any2Some(_).?)
                }.getOrElse(ContentType.apply(`application/octet-stream`))

            HttpResponse(
              entity = HttpEntity(
                contentType,
                meta.contentLength,
                source
              )
            )
          case _ =>
            throw ServiceResponseException.E0404
        }
    }) {
      case Success(response) =>
        complete(response)
      case Failure(ex) =>
        throw ServiceResponseException.E0404.copy(_msg = ex.getMessage)
    }

  private def upload(key: String, data: Source[ByteString, _], contentType: ContentType): Route = {
    val contentLengthFuture =
      data.runFold(0L) {
        case (acc, byte) =>
          acc + byte.length
      }

    onComplete(contentLengthFuture.flatMap { contentLength =>
      if (contentLength > 0) {
        modules.s3Client.putObject(imageBucket, key, data, contentLength, contentType, S3Headers(Nil)).map { _ =>
          DefaultServiceResponse.success("Success")
        }
      } else {
        throw ServiceResponseException.E0400.copy(_msg = "Empty file")
      }
    }) {
      case Success(response) =>
        complete(response)
      case Failure(ex) =>
        throw ServiceResponseException.E0404.copy(_msg = ex.getMessage)
    }
  }
}

object Images {
  def buildOrganizationImagePath(org: Long): String =
    s"/images/organization/$org"

  def buildAccountImagePath(org: Long, acc: Long): String =
    s"/images/organization/$org/account/$acc"
}
