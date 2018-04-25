package org.simplereviews

import com.google.inject.Guice

import org.simplereviews.guice.Modules
import org.simplereviews.guice.modules.ModuleBindings
import org.simplereviews.logger.impl.{ ErrorLogger, RequestLogger }

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