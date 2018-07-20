package org.simplereviews.models.dto

import org.mindrot.jbcrypt.BCrypt

import org.byrde.commons.persistence.sql.slick.sqlbase.BaseEntity
import org.byrde.commons.utils.JsonUtils._
import org.simplereviews.controllers.requests.CreateUserRequest
import org.simplereviews.models.GeneratedPassword

import play.api.libs.json._

import scala.language.implicitConversions
import scala.util.Random

case class User(
    id: Long,
    organizationId: Long,
    email: String,
    password: String,
    firstName: String,
    lastName: String,
    isAdmin: Boolean,
    imageTokenOpt: Option[String] = None
) extends BaseEntity {
  lazy val name =
    s"$firstName $lastName"

  lazy val imageKeyOpt: Option[String] =
    imageTokenOpt.map(imageToken => s"$organizationId/$imageToken")
}

object User {
  implicit val writes: Writes[User] =
    writesWithPasswordFlag(writePassword = false)

  implicit def writesWithPasswordFlag(writePassword: Boolean): Writes[User] =
    new Writes[User] {
      override def writes(o: User): JsValue = {
        val passwordOpt =
          if (writePassword)
            Some(o.password)
          else
            Option.empty[String]

        Json.obj(
          "id" -> o.id,
          "organizationId" -> o.organizationId,
          "email" -> o.email,
          "firstName" -> o.firstName,
          "lastName" -> o.lastName,
          "isAdmin" -> o.isAdmin
        ) +?
          o.imageKeyOpt.map(imageKey => "image" -> JsString(imageKey)) +?
          passwordOpt.map(password => "password" -> JsString(password))
      }
    }

  def create(organizationId: Long, user: CreateUserRequest): (User, GeneratedPassword) =
    create(organizationId, user.email, user.firstName, user.lastName, isAdmin = user.isAdmin)

  def create(organizationId: Long, email: String, firstName: String, lastName: String, isAdmin: Boolean): (User, GeneratedPassword) = {
    val generatedPassword =
      generatePassword

    create(organizationId, email, generatedPassword, firstName, lastName, isAdmin) -> generatedPassword
  }

  def create(organizationId: Long, email: String, password: String, firstName: String, lastName: String, isAdmin: Boolean): User =
    User(0, organizationId, email, BCrypt.hashpw(password, BCrypt.gensalt()), standardizeName(firstName), standardizeName(lastName), isAdmin)

  def standardizeName(name: String): String =
    name.trim.toLowerCase.capitalize

  def generatePassword: GeneratedPassword =
    String.valueOf(Random.alphanumeric.take(10).toArray)
}