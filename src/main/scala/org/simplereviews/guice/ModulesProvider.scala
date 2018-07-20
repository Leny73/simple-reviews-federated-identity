package org.simplereviews.guice

import org.simplereviews.configuration.Configuration
import org.simplereviews.logger.impl.{ ApplicationLogger, ErrorLogger, RequestLogger }
import org.simplereviews.models.Id
import org.simplereviews.models.dto.Client
import org.simplereviews.persistence.TokenStore
import org.simplereviews.persistence.sql.DataAccessLayer

trait ModulesProvider {
  def configuration: Configuration

  def applicationLogger: ApplicationLogger

  def requestLogger: RequestLogger

  def errorLogger: ErrorLogger

  def akka: Akka

  def persistence: DataAccessLayer

  def tokenStore: TokenStore

  def clients: Map[Id, Client]
}
