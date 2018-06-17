package org.simplereviews.persistence

import com.google.inject.Inject

import org.simplereviews.configuration.Configuration
import org.simplereviews.models.dto.{ Organization, OrganizationUser, User }

import org.byrde.commons.persistence.sql.slick.table.TablesA

import scala.concurrent.ExecutionContext

class DataAccessLayerProvider @Inject() (configuration: Configuration) extends TablesA(configuration.jdbcConfiguration) {
  import profile.api._

  implicit private val _: TablesA =
    this

  class Users(_tableTag: Tag) extends BaseTableA[User](_tableTag, "users") {
    def * = (id, organizationId, email, password, firstName, lastName, isAdmin) <> ((User.apply _).tupled, User.unapply)

    val organizationId: Rep[Long] = column[Long]("organization_id")
    val email: Rep[String] = column[String]("email", O.Length(255, varying = true), O.Unique)
    val password: Rep[String] = column[String]("password", O.Length(255, varying = true))
    val firstName: Rep[String] = column[String]("first_name", O.Length(255, varying = true))
    val lastName: Rep[String] = column[String]("last_name", O.Length(255, varying = true))
    val isAdmin: Rep[Boolean] = column[Boolean]("is_admin")

    lazy val organizationFk = foreignKey("fk_organization", organizationId, OrganizationsTQ)(_.id)
    lazy val idx = index("idx_email", email, unique = true)
  }

  lazy val UsersTQ = new TableQuery(new Users(_))

  class Organizations(_tableTag: Tag) extends BaseTableA[Organization](_tableTag, "organizations") {
    def * = (id, name, google, facebook) <> ((Organization.apply _).tupled, Organization.unapply)

    val name: Rep[String] = column[String]("organization", O.Length(255, varying = true), O.Unique)
    val google: Rep[Option[String]] = column[Option[String]]("google", O.Length(255, varying = true))
    val facebook: Rep[Option[String]] = column[Option[String]]("facebook", O.Length(255, varying = true))

    lazy val idx = index("idx_name", name, unique = true)
  }

  lazy val OrganizationsTQ = new TableQuery(new Organizations(_))

  class OrganizationUsers(_tableTag: Tag) extends BaseTableA[OrganizationUser](_tableTag, "organization_users") {
    def * = (id, organizationId, userId) <> ((OrganizationUser.apply _).tupled, OrganizationUser.unapply)

    val organizationId: Rep[Long] = column[Long]("organization_id")
    val userId: Rep[Long] = column[Long]("user_id")

    lazy val organizationFk = foreignKey("fk_organization_1", organizationId, OrganizationsTQ)(_.id, onDelete = ForeignKeyAction.Restrict)
    lazy val userFk = foreignKey("fk_user_1", userId, UsersTQ)(_.id, onDelete = ForeignKeyAction.Cascade)
    lazy val idx = index("idx_organization_id", organizationId, unique = false)
  }

  lazy val OrganizationUsersTQ = new TableQuery(new OrganizationUsers(_))

  def apply()(implicit ec: ExecutionContext): DataAccessLayer =
    new DataAccessLayer(this)(ec)
}