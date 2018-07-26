package org.simplereviews.persistence.sql

import org.mindrot.jbcrypt.BCrypt
import org.simplereviews.controllers.requests.UpdateUserRequest
import org.simplereviews.models.dto.{ Client, Organization, OrganizationUser, User }

import org.byrde.commons.persistence.sql.slick.dao.BaseDAONoStreamA
import org.byrde.commons.utils.OptionUtils._

import scala.concurrent.{ ExecutionContext, Future }

class DataAccessLayer(dataAccessLayerProvider: DataAccessLayerProvider)(implicit ec: ExecutionContext) {
  implicit val tables: DataAccessLayerProvider =
    dataAccessLayerProvider

  import tables._
  import tables.profile.api._

  lazy val UsersDAO =
    new BaseDAONoStreamA[Users, User](UsersTQ) {
      def insertAndInsertOrganizationUserRow(row: User): Future[User] =
        findById(row.id).flatMap {
          _.fold {
            for {
              user <- inserts(row).map(_.head)
              _ <- OrganizationUsersDAO.inserts(OrganizationUser.create(user.organizationId, user.id))
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
        findById(userId).flatMap {
          _.fold(Future.successful(Option.empty[User])) { user =>
            val futureFirstName =
              updateUserRequest
                .firstName
                .map(updateFirstName(userId, _))
                .getOrElse(Future.successful(user.?))

            val futureLastName =
              updateUserRequest
                .lastName
                .map(updateLastName(userId, _))
                .getOrElse(Future.successful(user.?))

            val futureEmail =
              updateUserRequest
                .email
                .filter(_ != user.email)
                .map(updateEmail(userId, _))
                .getOrElse(Future.successful(user.?))

            for {
              firstNameUpdate <- futureFirstName
              lastNameUpdate <- futureLastName
              emailUpdate <- futureEmail
            } yield {
              user.copy(
                email = emailUpdate.map(_.email).getOrElse(user.email),
                firstName = firstNameUpdate.map(_.firstName).getOrElse(user.firstName),
                lastName = lastNameUpdate.map(_.lastName).getOrElse(user.lastName)
              ).?
            }
          }
        }
    }

  lazy val OrganizationsDAO =
    new BaseDAONoStreamA[Organizations, Organization](OrganizationsTQ) {
      def findByName(name: String): Future[Option[Organization]] =
        findByFilter(_.name === name).map(_.headOption)
    }

  lazy val OrganizationUsersDAO =
    new BaseDAONoStreamA[OrganizationUsers, OrganizationUser](OrganizationUsersTQ) {}

  lazy val ClientsDAO =
    new BaseDAONoStreamA[Clients, Client](ClientsTQ) {
      def fetchAll: Future[Seq[Client]] =
        db.run((for {
          clients <- ClientsTQ
        } yield {
          clients
        }).result)
    }

  def applySchema(): Unit =
    db.run(
      (UsersTQ.schema ++
      OrganizationsTQ.schema ++
      OrganizationUsersTQ.schema ++
      ClientsTQ.schema).create
    )
}
