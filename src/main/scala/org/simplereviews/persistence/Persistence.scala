package org.simplereviews.persistence

import slick.jdbc.JdbcBackend

import org.byrde.commons.persistence.sql.slick.dao.BaseDAONoStreamA
import org.mindrot.jbcrypt.BCrypt
import org.simplereviews.guice.Modules
import org.simplereviews.models.dto.{ Organization, OrganizationUser, User }

import scala.concurrent.{ ExecutionContext, Future }

class Persistence(modules: Modules) {
  implicit val tables: Tables =
    modules.tables

  implicit val ec: ExecutionContext =
    modules.akka.system.dispatchers.lookup("db.dispatcher")

  import tables._
  import tables.profile.api._

  lazy val usersDAO =
    new BaseDAONoStreamA[tables.Users, User](UsersTQ) {
      override def upsert(row: User)(implicit session: Option[JdbcBackend#DatabaseDef => JdbcBackend#SessionDef] = None): Future[User] =
        findById(row.id).flatMap {
          _.fold {
            for {
              user <- super.upsert(row)
              _ <- organizationUsersDAO.upsert(OrganizationUser.create(user.organizationId, user.id))
            } yield user
          } { _ =>
            val func = UsersTQ filter (_.id === row.id) update row
            session.fold(db.run(func))(_.apply(db).database.run(func.withPinnedSession)).flatMap { success =>
              findById(row.id).map(_.get)
            }
          }
        }

      def findByEmailAndPassword(email: String, password: String): Future[Option[User]] =
        findByFilter(_.email === email) map { users =>
          users.headOption.filter(user => BCrypt.checkpw(password, user.password))
        }

      def findByEmailAndPasswordAndOrganization(email: String, password: String, organization: String): Future[Option[User]] =
        db.run((for {
          (user, organization) <- UsersTQ filter (_.email === email) join OrganizationsTQ on (_.organizationId === _.id) filter (_._2.name === organization.toLowerCase)
        } yield {
          user -> organization
        }).result).map { result =>
          result
            .headOption
            .collect {
              case (user, _) if BCrypt.checkpw(password, user.password) =>
                user
            }
        }
    }

  lazy val organizationsDAO =
    new BaseDAONoStreamA[tables.Organizations, Organization](OrganizationsTQ) {
      def findByName(name: String): Future[Option[Organization]] =
        findByFilter(_.name === name.toLowerCase).map(_.headOption)
    }

  lazy val organizationUsersDAO =
    new BaseDAONoStreamA[tables.OrganizationUsers, OrganizationUser](OrganizationUsersTQ) {
      def findByOrganization(org: Long): Future[Seq[User]] =
        db.run((for {
          (_, users) <- OrganizationUsersTQ filter (_.organizationId === org) join UsersTQ on (_.userId === _.id)
        } yield {
          users
        }).result)

      def findByOrganizationAndUser(org: Long, user: Long): Future[Option[OrganizationUser]] =
        findByFilter { row =>
          row.organizationId === org && row.userId === user
        }.map(_.headOption)
    }

  def applySchema(): Unit =
    db.run((UsersTQ.schema ++ OrganizationsTQ.schema ++ OrganizationUsersTQ.schema).create)
}