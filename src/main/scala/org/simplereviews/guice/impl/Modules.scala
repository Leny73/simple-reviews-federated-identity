package org.simplereviews.guice.impl

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted

import org.sedis.Pool
import org.simplereviews.configuration.Configuration
import org.simplereviews.guice.{ Akka, ModulesProvider }
import org.simplereviews.logger.impl.{ ApplicationLogger, ErrorLogger, RequestLogger }
import org.simplereviews.models.Id
import org.simplereviews.models.dto.{ Client, Organization, User }
import org.simplereviews.persistence.redis.RedisTokenStore
import org.simplereviews.persistence.sql.{ DataAccessLayer, DataAccessLayerProvider }
import org.simplereviews.utils.ThreadPools

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._

class Modules @Inject() (
    @Assisted() classLoader: ClassLoader,
    val configuration: Configuration,
    val applicationLogger: ApplicationLogger,
    val requestLogger: RequestLogger,
    val errorLogger: ErrorLogger,
    val akka: Akka,
    private val dataAccessLayerProvider: DataAccessLayerProvider
) extends ModulesProvider {
  private lazy val redisPool: Pool =
    configuration.redisConfiguration.pool

  lazy val persistence: DataAccessLayer =
    dataAccessLayerProvider()(ThreadPools.Postgres)

  lazy val tokenStore: RedisTokenStore =
    new RedisTokenStore(configuration.redisConfiguration.namespace, redisPool, classLoader)(ThreadPools.Redis)

  lazy val clients: Map[Id, Client] = {
    val fn =
      persistence
        .ClientsDAO
        .fetchAll

    val clients =
      Await.result(fn, Duration.Inf)

    clients
      .map { client =>
        client.id -> client
      }
      .toMap
  }

  private def start(): Future[Unit] = {
    implicit val ec: ExecutionContext =
      ExecutionContext.Implicits.global

    val EMAIL =
      "martin@byrde.io"

    val PASSWORD =
      "admin"

    val FIRSTNAME =
      "Martin"

    val LASTNAME =
      "Allaire"

    val ORGANIZATION =
      "simplereviews"

    persistence.applySchema()

    persistence.OrganizationsDAO.findByName(ORGANIZATION).flatMap(_.fold {
      persistence.OrganizationsDAO.inserts(Organization.create(ORGANIZATION)).map(_.head)
    }(Future.successful)).flatMap { organization =>
      persistence.UsersDAO.findByEmailAndAndOrganization(EMAIL, organization.name).flatMap { userOpt =>
        userOpt.fold {
          persistence.UsersDAO.insertAndInsertOrganizationUserRow(User.create(organization.id, EMAIL, PASSWORD, FIRSTNAME, LASTNAME, isAdmin = true))
        }(Future.successful)
      }
    }.map(_ => ())
  }

  akka.system.registerOnTermination { () =>
    Await.result(persistence.tables.db.shutdown, Duration.Inf)
    redisPool.underlying.destroy()
  }

  Await.result(start(), Duration.Inf)
}

object Modules {
  trait Factory {
    def withClassLoader(classLoader: ClassLoader): Modules
  }
}