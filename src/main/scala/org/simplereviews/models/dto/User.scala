package org.simplereviews.models.dto

import org.byrde.commons.persistence.sql.slick.sqlbase.BaseEntity
import org.mindrot.jbcrypt.BCrypt

case class User(
  id: Long,
  organization: Long,
  email: String,
  password: String,
  firstName: String,
  lastName: String,
  isVerified: Boolean,
  isAdmin: Boolean
) extends BaseEntity

object User {
  def create(organization: Organization, email: String, password: String, firstName: String, lastName: String, isVerified: Boolean, isAdmin: Boolean): User =
    User(0, organization.id, email, BCrypt.hashpw(password, BCrypt.gensalt()), firstName, lastName, isVerified, isAdmin)
}