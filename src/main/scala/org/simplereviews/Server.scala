package org.simplereviews

import com.google.inject.{ Guice, Injector }

import org.simplereviews.configuration.{ CORSConfiguration, Configuration }
import org.simplereviews.controllers.Routes
import org.simplereviews.guice.ModulesProvider
import org.simplereviews.guice.modules.ModuleBindings
import org.simplereviews.logger.impl.{ ErrorLogger, RequestLogger }
import org.simplereviews.utils.ThreadPools

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.ExecutionContext

object Server extends App with Routes {
  import net.codingwell.scalaguice.InjectorExtensions._

  private val injector: Injector =
    Guice.createInjector(new ModuleBindings())

  override lazy val modulesProvider: ModulesProvider =
    Guice.createInjector(new ModuleBindings()).instance[ModulesProvider]

  lazy val configuration: Configuration =
    modulesProvider.configuration

  override lazy val corsConfiguration: CORSConfiguration =
    configuration.corsConfiguration

  override lazy val errorLogger: ErrorLogger =
    modulesProvider.errorLogger

  override lazy val requestLogger: RequestLogger =
    modulesProvider.requestLogger

  override implicit val ec: ExecutionContext =
    ThreadPools.Global

  override implicit val system: ActorSystem =
    modulesProvider.akka.system

  override implicit val timeout: Timeout =
    configuration.timeout

  implicit val materializer: ActorMaterializer =
    ActorMaterializer()

  Http().bindAndHandle(
    routes,
    configuration.interface,
    configuration.port
  )
}