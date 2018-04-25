package org.simplereviews.guice.modules

import net.codingwell.scalaguice.ScalaModule

import com.google.inject.AbstractModule

import org.simplereviews.configuration.Configuration
import org.simplereviews.guice.{ Akka, Modules }
import org.simplereviews.logger.impl.{ ApplicationLogger, ErrorLogger, RequestLogger }

class ModuleBindings extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[Configuration]
    bind[Akka]
    bind[Modules]
  }
}
