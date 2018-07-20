package org.simplereviews.guice.modules

import net.codingwell.scalaguice.ScalaModule

import com.google.inject.{ AbstractModule, Binder }

import org.simplereviews.configuration.Configuration
import org.simplereviews.guice.impl.Modules
import org.simplereviews.guice.{ Akka, AssistedInjectFactoryScalaModule, ModulesProvider }
import org.simplereviews.logger.impl.{ ApplicationLogger, ErrorLogger, RequestLogger }
import org.simplereviews.persistence.sql.DataAccessLayerProvider

class ModuleBindings extends AbstractModule with ScalaModule with AssistedInjectFactoryScalaModule[Binder] {
  override def configure(): Unit = {
    bind[Configuration].asEagerSingleton()
    bind[Akka].asEagerSingleton()
    bind[ErrorLogger].asEagerSingleton()
    bind[RequestLogger].asEagerSingleton()
    bind[ApplicationLogger].asEagerSingleton()
    bind[DataAccessLayerProvider].asEagerSingleton()
    bindFactory[ModulesProvider, Modules.Factory]()
  }
}
