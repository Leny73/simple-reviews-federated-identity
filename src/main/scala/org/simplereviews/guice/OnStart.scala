package org.simplereviews.guice

import com.google.inject.Inject

import org.simplereviews.models.dto.{ Organization, User }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

class OnStart @Inject() (modules: Modules) {
  private implicit val _ =
    modules.akka.system.dispatcher

  private val EMAIL =
    "martin@byrde.io"

  private val PASSWORD =
    "admin"

  private val FIRSTNAME =
    "Martin"

  private val LASTNAME =
    "Allaire"

  private val ORGANIZATION =
    "SimpleReviews"

  private def start(): Future[User] = {
    modules.persistence.applySchema()

    modules.persistence.organizationsDAO.findByName(ORGANIZATION).flatMap(_.fold {
      modules.persistence.organizationsDAO.insert(Organization.create(ORGANIZATION))
    }(Future.successful)).flatMap { organization =>
      modules.persistence.usersDAO.findByEmailAndPasswordAndOrganization(EMAIL, PASSWORD, organization.name).flatMap { userOpt =>
        userOpt.fold {
          modules.persistence.usersDAO.insertAndInsertOrganizationUserRow(User.create(organization.id, EMAIL, PASSWORD, FIRSTNAME, LASTNAME, isAdmin = true))
        }(Future.successful)
      }
    }
  }

  Await.result(start(), 10.seconds)
}