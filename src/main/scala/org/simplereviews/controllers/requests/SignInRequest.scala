package org.simplereviews.controllers.requests

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class SignInRequest(organization: String, email: String, password: String)

object SignInRequest {
  implicit val reads: Reads[SignInRequest] =
    ((JsPath \ "organization").read[String] and
      (JsPath \ "email").read[String] and
      (JsPath \ "password").read[String])(SignInRequest.apply _)
}