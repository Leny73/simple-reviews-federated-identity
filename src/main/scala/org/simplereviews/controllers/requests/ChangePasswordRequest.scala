package org.simplereviews.controllers.requests

import play.api.libs.json.{ JsPath, Reads }
import play.api.libs.functional.syntax._

case class ChangePasswordRequest(currentPassword: String, newPassword: String, verifyNewPassword: String)

object ChangePasswordRequest {
  implicit val reads: Reads[ChangePasswordRequest] =
    ((JsPath \ "currentPassword").read[String] and
      (JsPath \ "newPassword").read[String] and
      (JsPath \ "verifyNewPassword").read[String])(ChangePasswordRequest.apply _)
}
