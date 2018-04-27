package org.simplereviews.controllers.requests

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class LoginRequest(username: String, password: String)

object LoginRequest {
  implicit val reads: Reads[LoginRequest] =
    ((JsPath \ "username").read[String] and
      (JsPath \ "password").read[String])(LoginRequest.apply _)
}