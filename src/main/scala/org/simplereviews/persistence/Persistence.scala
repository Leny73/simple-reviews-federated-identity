package org.simplereviews.persistence

import org.byrde.commons.persistence.sql.slick.dao.BaseDAONoStreamA
import org.mindrot.jbcrypt.BCrypt
import org.simplereviews.guice.Modules
import org.simplereviews.models.dto.{ Organization, User }

import scala.concurrent.{ ExecutionContext, Future }

class Persistence(modules: Modules) {
  implicit val tables: Tables =
    modules.tables

  implicit val ec: ExecutionContext =
    modules.akka.system.dispatchers.lookup("db.dispatcher")

  import tables._
  import tables.profile.api._

  lazy val userDAO =
    new BaseDAONoStreamA[tables.Users, User](UserTQ) {
      def findByEmailAndPassword(email: String, password: String): Future[Option[User]] =
        findByFilter(_.email === email) map { users =>
          users.headOption.filter(u => BCrypt.checkpw(password, u.password))
        }

      def findByEmailAndPasswordAndOrganization(email: String, password: String, organization: String): Future[Option[User]] =
        db.run((for {
          (user, organization) <- UserTQ filter (_.email === email) join OrganizationTQ on (_.organizationId === _.id) filter (_._2.name === organization.toLowerCase)
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

  lazy val organizationDAO =
    new BaseDAONoStreamA[tables.Organizations, Organization](OrganizationTQ) {
      def findByName(name: String): Future[Option[Organization]] =
        findByFilter(_.name === name.toLowerCase).map(_.headOption)
    }

  def applySchema(): Unit =
    db.run((UserTQ.schema ++ OrganizationTQ.schema).create)
}