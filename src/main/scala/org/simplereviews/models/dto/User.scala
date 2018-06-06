package org.simplereviews.models.dto

import org.byrde.commons.persistence.sql.slick.sqlbase.BaseEntity
import org.mindrot.jbcrypt.BCrypt
import org.simplereviews.controllers.Images
import org.simplereviews.controllers.requests.CreateUserRequest
import org.simplereviews.models.dto.User.GeneratedPassword

import play.api.libs.json.{ JsValue, Json, Writes }

import scala.util.Random

case class User(
    id: Long,
    organizationId: Long,
    email: String,
    password: String,
    firstName: String,
    lastName: String,
    isAdmin: Boolean
) extends BaseEntity {
  lazy val name =
    s"$firstName $lastName"
}

object User {
  type GeneratedPassword = String

  implicit val writes: Writes[User] =
    new Writes[User] {
      override def writes(o: User): JsValue =
        Json.obj(
          "id" -> o.id,
          "organizationId" -> o.organizationId,
          "email" -> o.email,
          "firstName" -> o.firstName,
          "lastName" -> o.lastName,
          "organizationImage" -> Images.buildOrganizationImagePath(o.organizationId),
          "userImage" -> Images.buildUserImagePath(o.organizationId, o.id),
          "isAdmin" -> o.isAdmin
        )
    }

  def create(organizationId: Long, user: CreateUserRequest): (User, GeneratedPassword) =
    create(organizationId, user.email, user.firstName, user.lastName, isAdmin = user.isAdmin)

  def create(organizationId: Long, email: String, firstName: String, lastName: String, isAdmin: Boolean): (User, GeneratedPassword) = {
    val password =
      generatePassword

    create(organizationId, email, password, firstName, lastName, isAdmin) -> password
  }

  def create(organizationId: Long, email: String, password: String, firstName: String, lastName: String, isAdmin: Boolean): User =
    User(0, organizationId, email, BCrypt.hashpw(password, BCrypt.gensalt()), standardizeName(firstName), standardizeName(lastName), isAdmin)

  def standardizeName(name: String): String =
    name.trim.toLowerCase.capitalize

  def generatePassword: String =
    String.valueOf(Random.alphanumeric.take(10).toArray)
}