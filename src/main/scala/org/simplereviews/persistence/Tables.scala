package org.simplereviews.persistence

import com.google.inject.Inject

import org.byrde.commons.persistence.sql.slick.table.TablesA
import org.simplereviews.guice.Modules
import org.simplereviews.models.dto.{ Organization, User }

class Tables @Inject() (modules: Modules) extends TablesA(modules.configuration.jdbcConfiguration) {
  import profile.api._

  class Users(_tableTag: Tag) extends BaseTableA[User](_tableTag, "users") {
    def * = (id, organizationId, email, password, firstName, lastName, isVerified, isAdmin) <> ((User.apply _).tupled, User.unapply)

    val organizationId: Rep[Long] = column[Long]("organization_id")
    val email: Rep[String] = column[String]("email", O.Length(255, varying = true), O.Unique)
    val password: Rep[String] = column[String]("password", O.Length(255, varying = true))
    val firstName: Rep[String] = column[String]("first_name", O.Length(255, varying = true))
    val lastName: Rep[String] = column[String]("last_name", O.Length(255, varying = true))
    val isVerified: Rep[Boolean] = column[Boolean]("is_verified")
    val isAdmin: Rep[Boolean] = column[Boolean]("is_admin")

    lazy val organizationFk = foreignKey("fk_organization", organizationId, OrganizationTQ)(_.id)
    lazy val idx = index("idx_email", email, unique = true)
  }

  lazy val UserTQ = new TableQuery(new Users(_))

  class Organizations(_tableTag: Tag) extends BaseTableA[Organization](_tableTag, "organizations") {
    def * = (id, name, google, facebook) <> ((Organization.apply _).tupled, Organization.unapply)

    val name: Rep[String] = column[String]("organization", O.Length(255, varying = true), O.Unique)
    val google: Rep[Option[String]] = column[Option[String]]("google", O.Length(255, varying = true))
    val facebook: Rep[Option[String]] = column[Option[String]]("facebook", O.Length(255, varying = true))

    lazy val idx = index("idx_name", name, unique = true)
  }

  lazy val OrganizationTQ = new TableQuery(new Organizations(_))
}