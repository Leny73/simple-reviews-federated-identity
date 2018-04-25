package org.simplereviews.models

import org.simplereviews.utils.WritesUtils

import play.api.libs.json.Writes

trait DefaultServiceResponse extends ServiceResponse[String] {
  override implicit val writes: Writes[String] =
    WritesUtils.string

  override val response: String =
    msg
}
