package org.simplereviews.services.facebook

import org.simplereviews.guice.Modules
import org.simplereviews.logger.impl.ServiceLogger
import org.simplereviews.services.BaseHttpService

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

class FacebookService(modules: Modules) extends BaseHttpService {
  override protected def serviceLogger: ServiceLogger = ???

  override implicit protected def system: ActorSystem = ???

  override implicit protected def materializer: ActorMaterializer = ???

  override implicit protected def ec: ExecutionContext = ???

  override protected val http: HttpExt = ???

  override def host: String = ???
}
