package org.simplereviews.services.aws

import org.byrde.commons.utils.OptionUtils._
import org.byrde.commons.utils.TryUtils._
import org.byrde.commons.utils.AkkaStreamsUtils._
import org.simplereviews.guice.Modules
import org.simplereviews.models.services.responses.aws.S3ServiceResponse

import akka.actor.ActorSystem
import akka.http.scaladsl.model.MediaTypes.`application/octet-stream`
import akka.http.scaladsl.model.ContentType
import akka.stream.Materializer
import akka.stream.alpakka.s3.impl.S3Headers
import akka.stream.alpakka.s3.scaladsl.{ ObjectMetadata, S3Client }
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.ByteString

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

class S3ServiceWrapper(modules: Modules)(implicit materializer: Materializer, actorSystem: ActorSystem) {
  implicit protected val ec: ExecutionContext =
    modules.akka.system.dispatchers.lookup("akka.stream.alpakka.s3.dispatcher")

  lazy val s3Client: S3Client =
    new S3Client(modules.configuration.s3Configuration)

  def download(bucket: String, key: String): Future[Try[S3ServiceResponse]] =
    s3Client.download(bucket, key) match {
      case (source, metaFuture) =>
        metaFuture.map {
          case meta if meta.contentLength <= 0 =>
            new Exception(s"Could not find S3 object for key: $key, in bucket: $bucket").!-
          case meta =>
            val contentType =
              getContentType(meta)

            S3ServiceResponse(contentType, meta.contentLength, source).!+
        }
    }

  def upload(bucket: String, key: String, data: Source[ByteString, _], contentType: ContentType): Future[Try[S3ServiceResponse]] = {
    data.runWith(Sink.seq).flatMap { materializedData =>
      val contentLength =
        materializedData.foldLeft(0L) {
          case (acc, byte) =>
            acc + byte.length
        }

      contentLength match {
        case x if x <= 0 =>
          Future.failed(new Exception("Cannot upload empty file"))
        case x =>
          s3Client
            //If both consumers used the same stream it would get flushed before the second consumer could get to it.
            //Have to materialize the data once and then re-source the data for the upload & response because
            //there is too much time between the first and second consumer.
            .putObject(bucket, key, materializedData.toSource, x, contentType, S3Headers(Nil))
            .map { _ =>
              S3ServiceResponse(contentType, contentLength, materializedData.toSource).!+
            }
      }
    }
  }

  private def getContentType: ObjectMetadata => ContentType = meta =>
    meta
      .contentType
      .flatMap {
        ContentType
          .parse(_)
          .fold(_ => Option.empty[ContentType], _.?)
      }
      .getOrElse(ContentType.apply(`application/octet-stream`))
}
