package org.simplereviews.persistence

import org.byrde.commons.persistence.sql.slick.dao.BaseDAONoStreamA
import org.mindrot.jbcrypt.BCrypt
import org.simplereviews.guice.Modules
import org.simplereviews.models.dto.User

import scala.concurrent.{ ExecutionContext, Future }

class Persistence(modules: Modules) {
  implicit val tables: Tables =
    modules.tables

  implicit val ec: ExecutionContext =
    modules.akka.system.dispatchers.lookup("db.dispatcher")

  import tables._
  import tables.profile.api._

  lazy val userDAO = new BaseDAONoStreamA[tables.Users, User](UserTQ) {
    def findByUsernameAndPassword(username: String, password: String): Future[Option[User]] =
      findByFilter(u => u.username === username) map { users =>
        users.headOption.filter(u => BCrypt.checkpw(password, u.password))
      }
  }

  def applySchema(): Unit =
    db.run(Seq(UserTQ.schema).reduceLeft(_ ++ _).create)
}