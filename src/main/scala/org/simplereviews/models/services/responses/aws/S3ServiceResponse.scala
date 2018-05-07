package org.simplereviews.models.services.responses.aws

import akka.http.scaladsl.model.{ ContentType, HttpEntity, UniversalEntity }
import akka.stream.scaladsl.Source
import akka.util.ByteString

case class S3ServiceResponse(contentType: ContentType, contentLength: Long, data: Source[ByteString, Any]) {
  lazy val toUniversalEntity: UniversalEntity =
    HttpEntity(contentType, contentLength, data)
}