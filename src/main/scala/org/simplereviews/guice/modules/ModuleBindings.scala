package org.simplereviews.guice.modules

import net.codingwell.scalaguice.ScalaModule

import com.google.inject.AbstractModule

import org.simplereviews.configuration.Configuration
import org.simplereviews.guice.{ Akka, ModulesProvider, OnStart }
import org.simplereviews.logger.impl.{ ApplicationLogger, ErrorLogger, RequestLogger }
import org.simplereviews.persistence.sql.DataAccessLayerProvider

class ModuleBindings extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[Configuration].asEagerSingleton()
    bind[Akka].asEagerSingleton()
    bind[ErrorLogger].asEagerSingleton()
    bind[RequestLogger].asEagerSingleton()
    bind[ApplicationLogger].asEagerSingleton()
    bind[DataAccessLayerProvider].asEagerSingleton()
    bind[ModulesProvider].asEagerSingleton()
    bind[OnStart].asEagerSingleton()
  }
}
