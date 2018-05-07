package org.simplereviews.services.aws

import org.byrde.commons.utils.OptionUtils._
import org.byrde.commons.utils.TryUtils._
import org.simplereviews.guice.Modules
import org.simplereviews.models.services.responses.Ack
import org.simplereviews.models.services.responses.aws.S3ServiceResponse

import akka.actor.ActorSystem
import akka.http.scaladsl.model.MediaTypes.`application/octet-stream`
import akka.http.scaladsl.model.ContentType
import akka.stream.Materializer
import akka.stream.alpakka.s3.S3Settings
import akka.stream.alpakka.s3.impl.S3Headers
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

class S3ServiceWrapper(modules: Modules)(implicit materializer: Materializer, actorSystem: ActorSystem) {
  implicit protected val ec: ExecutionContext =
    modules.akka.system.dispatchers.lookup("services.aws.s3.dispatcher")

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
              meta
                .contentType
                .flatMap {
                  ContentType
                    .parse(_)
                    .fold(_ => Option.empty[ContentType], _.?)
                }.getOrElse(ContentType.apply(`application/octet-stream`))

            S3ServiceResponse(contentType, meta.contentLength, source).!+
        }
    }

  def upload(bucket: String, key: String, data: Source[ByteString, _], contentType: ContentType): Future[Try[Ack]] = {
    val contentLengthFuture =
      data.runFold(0L) {
        case (acc, byte) =>
          acc + byte.length
      }

    contentLengthFuture.flatMap {
      case contentLength if contentLength <= 0 =>
        Future.failed(new Exception("Cannot upload empty file"))
      case contentLength =>
        s3Client.putObject(bucket, key, data, contentLength, contentType, S3Headers(Nil)).map { _ =>
          Ack.!+
        }
    }
  }
}
