package org.simplereviews.controllers.requests

import play.api.libs.json.{ JsPath, Reads }
import play.api.libs.json.Reads.email
import play.api.libs.functional.syntax._

case class UpdateUserRequest(email: Option[String], firstName: Option[String], lastName: Option[String])

object UpdateUserRequest {
  implicit val reads: Reads[UpdateUserRequest] =
    ((JsPath \ "email").readNullable[String](email) and
      (JsPath \ "firstName").readNullable[String] and
      (JsPath \ "lastName").readNullable[String])(UpdateUserRequest.apply _)
}