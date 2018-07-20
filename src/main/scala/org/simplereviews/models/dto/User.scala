package org.simplereviews.models.dto

import org.mindrot.jbcrypt.BCrypt

import org.byrde.commons.persistence.sql.slick.sqlbase.BaseEntity
import org.byrde.commons.utils.JsonUtils._
import org.byrde.commons.utils.OptionUtils._
import org.simplereviews.controllers.requests.CreateUserRequest
import org.simplereviews.models.GeneratedKey
import org.simplereviews.utils.KeyGenerator

import play.api.libs.json._

import scala.language.implicitConversions

case class User(
    id: Long,
    organizationId: Long,
    email: String,
    password: String,
    firstName: String,
    lastName: String,
    isAdmin: Boolean,
    imageToken: String
) extends BaseEntity {
  lazy val name =
    s"$firstName $lastName"

  lazy val imageKey: String =
    s"$organizationId/$imageToken"
}

object User {
  implicit val writes: Writes[User] =
    writesWithPasswordFlag(writePassword = false)

  implicit def writesWithPasswordFlag(writePassword: Boolean): Writes[User] =
    new Writes[User] {
      override def writes(o: User): JsValue = {
        val passwordOpt =
          if (writePassword)
            o.password.?
          else
            Option.empty[String]

        Json.obj(
          "id" -> o.id,
          "organizationId" -> o.organizationId,
          "email" -> o.email,
          "firstName" -> o.firstName,
          "lastName" -> o.lastName,
          "isAdmin" -> o.isAdmin,
          "imageKey" -> o.imageKey
        ) +? passwordOpt.map(password => "password" -> JsString(password))
      }
    }

  def create(organizationId: Long, user: CreateUserRequest): (User, GeneratedKey) =
    create(organizationId, user.email, user.firstName, user.lastName, isAdmin = user.isAdmin)

  def create(organizationId: Long, email: String, firstName: String, lastName: String, isAdmin: Boolean): (User, GeneratedKey) = {
    val generatedPassword =
      KeyGenerator.generateKey

    create(organizationId, email, generatedPassword, firstName, lastName, isAdmin) -> generatedPassword
  }

  def create(organizationId: Long, email: String, password: String, firstName: String, lastName: String, isAdmin: Boolean): User =
    User(0, organizationId, email, BCrypt.hashpw(password, BCrypt.gensalt()), standardizeName(firstName), standardizeName(lastName), isAdmin, KeyGenerator.generateKey)

  def standardizeName(name: String): String =
    name.trim.toLowerCase.capitalize
}