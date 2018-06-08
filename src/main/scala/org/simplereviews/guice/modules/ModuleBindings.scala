package org.simplereviews.guice.modules

import net.codingwell.scalaguice.ScalaModule

import com.google.inject.AbstractModule

import org.simplereviews.configuration.Configuration
import org.simplereviews.guice.{ Akka, Modules, OnStart }

class ModuleBindings extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[Configuration]
    bind[Akka]
    bind[Modules].asEagerSingleton()
    bind[OnStart].asEagerSingleton()
  }
}
