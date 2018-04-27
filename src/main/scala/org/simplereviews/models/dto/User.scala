package org.simplereviews.models.dto

import org.byrde.commons.persistence.sql.slick.sqlbase.BaseEntity
import org.mindrot.jbcrypt.BCrypt

case class User(id: Long, username: String, password: String) extends BaseEntity

object User {
  def create(username: String, password: String): User =
    User(0, username, BCrypt.hashpw(password, BCrypt.gensalt()))
}