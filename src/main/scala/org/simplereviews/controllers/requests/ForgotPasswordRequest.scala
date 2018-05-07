package org.simplereviews.controllers.requests

import play.api.libs.json.{ JsPath, Reads }
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._

case class ForgotPasswordRequest(organization: String, email: String)

object ForgotPasswordRequest {
  implicit val reads: Reads[ForgotPasswordRequest] =
    ((JsPath \ "organization").read[String] and
      (JsPath \ "email").read[String](email))(ForgotPasswordRequest.apply _)
}