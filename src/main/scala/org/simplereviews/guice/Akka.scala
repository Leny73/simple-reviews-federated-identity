package org.simplereviews.guice

import com.google.inject.Inject

import org.simplereviews.configuration.Configuration
import org.simplereviews.persistence.DataAccessLayerProvider

import akka.actor.ActorSystem

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class Akka @Inject() (configuration: Configuration, tables: DataAccessLayerProvider) {
  implicit val system: ActorSystem =
    ActorSystem(configuration.name, configuration.underlyingConfig)

  system.registerOnTermination { () =>
    Await.result(tables.db.shutdown, 10 seconds)
  }
}
