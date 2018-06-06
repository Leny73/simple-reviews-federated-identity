package org.simplereviews.controllers

import org.byrde.commons.models.services.CommonsServiceResponseDictionary._
import org.byrde.commons.utils.auth.conf.JwtConfig
import org.byrde.commons.utils.FutureUtils._
import org.simplereviews.controllers.directives.{ ApiSupport, AuthenticationDirectives }
import org.simplereviews.guice.Modules

import akka.http.scaladsl.server.directives.CachingDirectives._
import akka.http.caching.scaladsl.{ Cache, CachingSettings, LfuCacheSettings }
import akka.http.caching.LfuCache
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MarshallingEntityWithRequestDirective
import akka.http.scaladsl.server.{ RequestContext, Route, RouteResult }
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Images(val modules: Modules)(implicit ec: ExecutionContext) extends ApiSupport with MarshallingEntityWithRequestDirective with AuthenticationDirectives {
  implicit val materializer: ActorMaterializer =
    modules.akka.materializer

  val jwtConfig: JwtConfig =
    modules.configuration.jwtConfiguration

  val imageBucket: String =
    modules.configuration.imageBucket

  val defaultCachingSettings: CachingSettings =
    CachingSettings(modules.akka.system)

  val lfuCacheSettings: LfuCacheSettings =
    defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(100)
      .withMaxCapacity(1000)
      .withTimeToIdle(60.seconds)

  val cachingSettings: CachingSettings =
    defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

  val lfuCache: Cache[Uri, RouteResult] =
    LfuCache(cachingSettings)

  val keyer: PartialFunction[RequestContext, Uri] = {
    val isGet: RequestContext => Boolean =
      _.request.method == HttpMethods.GET

    PartialFunction {
      case r: RequestContext if isGet(r) =>
        r.request.uri
    }
  }

  val filename: String =
    "image"

  lazy val routes: Route =
    pathPrefix("org" / LongNumber) { organizationId =>
      get {
        isAuthenticatedAndPartOfOrganization(organizationId, jwtConfig) { _ =>
          cache(lfuCache, keyer) {
            downloadImage(imageBucket, Images.buildOrganizationS3Key(organizationId, filename))
          }
        }
      } ~ post {
        isAuthenticatedAndAdminAndPartOfOrganization(organizationId, jwtConfig) { _ =>
          uploadImage(filename, imageBucket, Images.buildOrganizationS3Key(organizationId, filename))
        }
      } ~ path("user" / LongNumber) { userId =>
        get {
          isAuthenticatedAndPartOfOrganization(organizationId, jwtConfig) { _ =>
            cache(lfuCache, keyer) {
              downloadImage(imageBucket, Images.buildUserS3Key(organizationId, userId, filename))
            }
          }
        } ~ post {
          isAuthenticatedAndPartOfOrganizationAndSameUser(organizationId, userId, jwtConfig) { _ =>
            uploadImage(filename, imageBucket, Images.buildUserS3Key(organizationId, userId, filename))
          }
        }
      }
    }

  def downloadImage(bucket: String, key: String): Route =
    async[UniversalEntity]({
      modules.s3ServiceWrapper.download(bucket, key).flattenTry.map(_.toUniversalEntity)
    }, entity => complete(HttpResponse(entity = entity)), E0404.apply)

  def uploadImage(filename: String, bucket: String, key: String): Route =
    fileUpload(filename) {
      case (metadata, _) if !(metadata.contentType.mediaType == `image/png`) && !(metadata.contentType.mediaType == `image/jpeg`) =>
        complete(E0400("Unsupported file type"))
      case (metadata, byteSource) =>
        async[UniversalEntity]({
          modules.s3ServiceWrapper.upload(imageBucket, key, byteSource, metadata.contentType).flattenTry.map(_.toUniversalEntity)
        }, entity => complete(HttpResponse(entity = entity)))
      case _ =>
        complete(E0404("Invalid request"))
    }
}

object Images {
  def buildOrganizationImagePath(organizationId: Long): String =
    s"/images/org/$organizationId"

  def buildUserImagePath(organizationId: Long, userId: Long): String =
    s"/images/org/$organizationId/user/$userId"

  def buildOrganizationS3Key(org: Long, file: String): String =
    s"$org/$file"

  def buildUserS3Key(org: Long, acc: Long, file: String): String =
    s"$org/$acc/$file"
}
