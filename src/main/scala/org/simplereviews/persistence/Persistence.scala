package org.simplereviews.persistence

import slick.jdbc.JdbcBackend

import org.byrde.commons.persistence.sql.slick.dao.BaseDAONoStreamA
import org.byrde.commons.utils.OptionUtils._
import org.mindrot.jbcrypt.BCrypt
import org.simplereviews.controllers.requests.UpdateUserRequest
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
      def insertAndInsertOrganizationUserRow(row: User)(implicit session: Option[JdbcBackend#DatabaseDef => JdbcBackend#SessionDef] = None): Future[User] =
        findById(row.id).flatMap {
          _.fold {
            for {
              user <- insert(row)
              _ <- organizationUsersDAO.insert(OrganizationUser.create(user.organizationId, user.id))
            } yield user
          }(Future.successful)
        }

      def findByOrganization(org: Long): Future[Seq[User]] =
        db.run((for {
          (_, users) <- OrganizationUsersTQ filter (_.organizationId === org) join UsersTQ on (_.userId === _.id)
        } yield {
          users
        }).result)

      def findByIdAndPassword(userId: Long, password: String): Future[Option[User]] =
        findById(userId) map { users =>
          users.filter(user => BCrypt.checkpw(password, user.password))
        }

      def findByEmailAndAndOrganization(email: String, organization: String): Future[Option[User]] =
        db.run((for {
          (user, organization) <- UsersTQ filter (_.email === email) join OrganizationsTQ on (_.organizationId === _.id) filter (_._2.name === organization.toLowerCase)
        } yield {
          user -> organization
        }).result).map { result =>
          result
            .headOption
            .map(_._1)
        }

      def findByEmailAndPasswordAndOrganization(email: String, password: String, organization: String): Future[Option[User]] =
        db.run((for {
          (user, organization) <- UsersTQ filter (_.email === email) join OrganizationsTQ on (_.organizationId === _.id) filter (_._2.name === organization.toLowerCase)
        } yield {
          user -> organization
        }).result).map { result =>
          result
            .headOption
            .map(_._1)
            .filter(user => BCrypt.checkpw(password, user.password))
        }

      def updateOrganization(userId: Long, organizationId: Long): Future[Option[User]] =
        db.run((for { c <- UsersTQ if c.id === userId } yield c.organizationId).update(organizationId)).flatMap(_ => findById(userId))

      def updateEmail(userId: Long, email: String): Future[Option[User]] =
        db.run((for { c <- UsersTQ if c.id === userId } yield c.email).update(email)).flatMap(_ => findById(userId))

      def updatePassword(userId: Long, password: String): Future[Option[User]] =
        db.run((for { c <- UsersTQ if c.id === userId } yield c.password).update(BCrypt.hashpw(password, BCrypt.gensalt()))).flatMap(_ => findById(userId))

      def updateFirstName(userId: Long, firstName: String): Future[Option[User]] =
        db.run((for { c <- UsersTQ if c.id === userId } yield c.firstName).update(firstName)).flatMap(_ => findById(userId))

      def updateLastName(userId: Long, lastName: String): Future[Option[User]] =
        db.run((for { c <- UsersTQ if c.id === userId } yield c.lastName).update(lastName)).flatMap(_ => findById(userId))

      def updateWithUpdateUserRequest(userId: Long, updateUserRequest: UpdateUserRequest): Future[Option[User]] =
        findById(userId).flatMap(_.map { user =>
          for {
            firstNameUpdate <- updateUserRequest.firstName.map(updateFirstName(userId, _)).getOrElse(Future.successful(user.?))
            lastNameUpdate <- updateUserRequest.lastName.map(updateLastName(userId, _)).getOrElse(Future.successful(user.?))
            emailUpdate <- updateUserRequest.email.filter(_ != user.email).map(updateEmail(userId, _)).getOrElse(Future.successful(user.?))
          } yield {
            user.copy(
              email = emailUpdate.map(_.email).getOrElse(user.email),
              firstName = firstNameUpdate.map(_.firstName).getOrElse(user.firstName),
              lastName = lastNameUpdate.map(_.lastName).getOrElse(user.lastName)
            )
          }.?
        }.getOrElse(Future.successful(Option.empty[User])))
    }

  lazy val organizationsDAO =
    new BaseDAONoStreamA[tables.Organizations, Organization](OrganizationsTQ) {
      def findByName(name: String): Future[Option[Organization]] =
        findByFilter(_.name === name.toLowerCase).map(_.headOption)
    }

  lazy val organizationUsersDAO =
    new BaseDAONoStreamA[tables.OrganizationUsers, OrganizationUser](OrganizationUsersTQ) {
      def findByOrganizationAndUser(org: Long, user: Long): Future[Option[OrganizationUser]] =
        findByFilter { row =>
          row.organizationId === org && row.userId === user
        }.map(_.headOption)
    }

  def applySchema(): Unit =
    db.run((UsersTQ.schema ++ OrganizationsTQ.schema ++ OrganizationUsersTQ.schema).create)
}