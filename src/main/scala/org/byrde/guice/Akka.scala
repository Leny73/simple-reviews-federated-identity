package challenge.guice

import challenge.configuration.Configuration

import com.google.inject.Inject

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

class Akka @Inject() (configuration: Configuration) {
  val actorSystem: ActorSystem =
    ActorSystem(configuration.name, configuration.underlyingConfig)
  val actorMaterializer: ActorMaterializer =
    ActorMaterializer()(actorSystem)
}
