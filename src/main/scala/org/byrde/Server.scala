package org.byrde

import challenge.guice.Modules
import challenge.guice.modules.ModuleBindings
import challenge.logger.impl.{ ErrorLogger, RequestLogger }

import com.google.inject.Guice

import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

object Server extends App with Routes {
  import net.codingwell.scalaguice.InjectorExtensions._

  override lazy val modules: Modules =
    Guice.createInjector(new ModuleBindings()).instance[Modules]

  override lazy val errorLogger: ErrorLogger =
    modules.errorLogger

  override lazy val requestLogger: RequestLogger =
    modules.requestLogger

  implicit val materializer: ActorMaterializer =
    modules.akka.actorMaterializer

  Http().bindAndHandle(routes, modules.configuration.interface, modules.configuration.port)
}