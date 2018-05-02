package org.simplereviews.controllers.requests

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class LoginRequest(organization: String, email: String, password: String)

object LoginRequest {
  implicit val reads: Reads[LoginRequest] =
    ((JsPath \ "organization").read[String] and
      (JsPath \ "email").read[String] and
      (JsPath \ "password").read[String])(LoginRequest.apply _)
}