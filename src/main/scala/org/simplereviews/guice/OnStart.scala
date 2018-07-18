package org.simplereviews.guice

import com.google.inject.Inject

import org.simplereviews.persistence.sql.DataAccessLayerProvider

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

class OnStart @Inject() (modulesProvider: ModulesProvider) {
  private implicit val _ =
    modulesProvider.akka.system.dispatcher

  private def start(): Future[Unit] =
    Future(modulesProvider.persistence.applySchema())

  Await.result(start(), 10.seconds)
}