package org.simplereviews.guice

import com.google.inject.Inject

import org.simplereviews.models.dto.{ Organization, User }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

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

    val organization =
      Await.result(modules.persistence.organizationsDAO.findByName(ORGANIZATION).flatMap(_.fold {
        modules.persistence.organizationsDAO.upsert(Organization.create(ORGANIZATION))
      }(Future.successful)), 10.seconds)

    modules.persistence.usersDAO.findByEmailAndPasswordAndOrganization(EMAIL, PASSWORD, organization.name).flatMap(_.fold {
      modules.persistence.usersDAO.upsert(User.create(organization.id, EMAIL, PASSWORD, FIRSTNAME, LASTNAME, isVerified = true, isAdmin = true))
    } { user =>
      modules.persistence.usersDAO.upsert(user.copy(organizationId = organization.id))
    })
  }

  Await.result(start(), 10.seconds)
}