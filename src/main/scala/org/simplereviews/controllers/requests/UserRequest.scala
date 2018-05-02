package org.simplereviews.controllers.requests

import play.api.libs.json.{ JsPath, Reads }
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class UserRequest(
  email: String,
  firstName: String,
  lastName: String,
  isAdmin: Boolean
)

object UserRequest {
  implicit val reads: Reads[UserRequest] =
    ((JsPath \ "email").read[String](email) and
      (JsPath \ "firstName").read[String] and
      (JsPath \ "lastName").read[String] and
      (JsPath \ "isAdmin").read[Boolean])(UserRequest.apply _)
}