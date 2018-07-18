package org.simplereviews.guice

import com.google.inject.Inject

import org.simplereviews.configuration.Configuration
import org.simplereviews.logger.impl.{ ApplicationLogger, ErrorLogger, RequestLogger }
import org.simplereviews.persistence.TokenStore
import org.simplereviews.persistence.redis.RedisTokenStore
import org.simplereviews.persistence.sql.{ DataAccessLayer, DataAccessLayerProvider }
import org.simplereviews.utils.ThreadPools

import scala.concurrent.Await
import scala.concurrent.duration._

class ModulesProvider @Inject() (
    val configuration: Configuration,
    val applicationLogger: ApplicationLogger,
    val requestLogger: RequestLogger,
    val errorLogger: ErrorLogger,
    val akka: Akka,
    val classLoader: ClassLoader,
    private val dataAccessLayerProvider: DataAccessLayerProvider
) {
  lazy val persistence: DataAccessLayer =
    dataAccessLayerProvider()(ThreadPools.Postgres)

  lazy val tokenStore: TokenStore =
    new RedisTokenStore(configuration.redisConfiguration, classLoader)(ThreadPools.Redis)

  akka.system.registerOnTermination { () =>
    Await.result(persistence.tables.db.shutdown, 10.seconds)
    Await.result(tokenStore.pool.underlying.destroy, 10.seconds)
  }
}