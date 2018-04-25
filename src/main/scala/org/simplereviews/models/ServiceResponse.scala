package org.simplereviews.models

import play.api.libs.json.{ JsObject, JsValue, Json, Writes }

object ServiceResponse {
  implicit val writes: Writes[ServiceResponse[_]] =
    new Writes[ServiceResponse[_]] {
      override def writes(o: ServiceResponse[_]): JsValue =
        o.toJson
    }

  def apply[T](_status: Int, _code: Int, _response: T, _msg: String = "response")(implicit _writes: Writes[T]): ServiceResponse[T] =
    new ServiceResponse[T] {
      override implicit val writes: Writes[T] =
        _writes

      override val msg: String =
        _msg

      override val code: Int =
        _code

      override val status: Int =
        _status

      override val response: T =
        _response
    }
}

trait ServiceResponse[T] {
  implicit def writes: Writes[T]

  def msg: String

  def status: Int

  def code: Int

  def response: T

  def toJson: JsObject =
    Json.obj(
      "message" -> msg,
      "status" -> status,
      "code" -> code,
      "response" -> Json.toJson(response)
    )
}

