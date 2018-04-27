package org.simplereviews.models

import org.simplereviews.utils.WritesUtils

import play.api.libs.json.{ JsValue, Writes }

trait DefaultServiceResponse extends ServiceResponse[String] {
  override implicit val writes: Writes[String] =
    WritesUtils.string

  override val response: String =
    msg
}

object DefaultServiceResponse {
  implicit val writes: Writes[DefaultServiceResponse] =
    new Writes[DefaultServiceResponse] {
      override def writes(o: DefaultServiceResponse): JsValue =
        o.toJson
    }

  def success(_msg: String): DefaultServiceResponse =
    new DefaultServiceResponse {
      override val msg: String =
        _msg

      override val code: Int =
        200

      override val status: Int =
        200

      override val response: String =
        _msg
    }
}