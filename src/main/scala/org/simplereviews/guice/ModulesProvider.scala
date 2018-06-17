package org.simplereviews.guice

import com.google.inject.Inject

import org.simplereviews.configuration.Configuration
import org.simplereviews.logger.impl.{ ApplicationLogger, ErrorLogger, RequestLogger }
import org.simplereviews.persistence.{ DataAccessLayer, DataAccessLayerProvider }
import org.simplereviews.utils.ThreadPools

class ModulesProvider @Inject() (
    val configuration: Configuration,
    val applicationLogger: ApplicationLogger,
    val requestLogger: RequestLogger,
    val errorLogger: ErrorLogger,
    val akka: Akka,
    private val dataAccessLayerProvider: DataAccessLayerProvider
) {
  lazy val persistence: DataAccessLayer =
    dataAccessLayerProvider()(ThreadPools.Database)
}