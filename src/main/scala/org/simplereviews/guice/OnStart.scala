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

    val organizationFuture =
      modules.persistence.organizationDAO.findByName(ORGANIZATION).flatMap(_.fold {
        modules.persistence.organizationDAO.upsert(Organization.create(ORGANIZATION))
      }(Future.successful))

    organizationFuture.flatMap { organization =>
      modules.persistence.userDAO.findByEmailAndPasswordAndOrganization(EMAIL, PASSWORD, organization.name).flatMap(_.fold {
        modules.persistence.userDAO.upsert(User.create(organization, EMAIL, PASSWORD, FIRSTNAME, LASTNAME, isVerified = true, isAdmin = true))
      } { user =>
        modules.persistence.userDAO.upsert(user.copy(organization = organization.id))
      })
    }
  }

  Await.result(start(), 10.seconds)
}