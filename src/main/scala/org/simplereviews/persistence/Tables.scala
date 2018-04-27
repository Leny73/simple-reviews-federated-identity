package org.simplereviews.persistence

import com.google.inject.Inject

import org.byrde.commons.persistence.sql.slick.table.TablesA
import org.simplereviews.guice.Modules
import org.simplereviews.models.dto.User

class Tables @Inject() (modules: Modules) extends TablesA(modules.configuration.jdbcConfiguration) {
  import profile.api._

  class Users(_tableTag: Tag) extends BaseTableA[User](_tableTag, "users") {
    def * = (id, username, password) <> ((User.apply _).tupled, User.unapply)

    val username: Rep[String] = column[String]("username", O.Length(255, varying = true))
    val password: Rep[String] = column[String]("password", O.Length(255, varying = true))

    val idx = index("idx_username", username, unique = true)
  }

  lazy val UserTQ = new TableQuery(tag => new Users(tag))
}