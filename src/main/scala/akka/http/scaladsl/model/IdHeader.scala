package akka.http.scaladsl.model

import akka.http.scaladsl.model.headers.{ ModeledCustomHeader, ModeledCustomHeaderCompanion }

import scala.util.{ Success, Try }

final case class IdHeader(id: String) extends ModeledCustomHeader[IdHeader] {
  override val renderInRequests =
    false

  override val renderInResponses =
    true

  override val companion: IdHeader.type =
    IdHeader

  override def value(): String =
    id
}

object IdHeader extends ModeledCustomHeaderCompanion[IdHeader] {
  override def name: String =
    "X-Request-Id"

  override def parse(value: String): Try[IdHeader] =
    Success(new IdHeader(value))
}
