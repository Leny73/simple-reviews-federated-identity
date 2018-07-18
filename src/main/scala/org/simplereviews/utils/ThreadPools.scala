package org.simplereviews.utils

import java.util.concurrent.Executors

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

object ThreadPools {
  lazy val Global: ExecutionContextExecutor =
    ExecutionContext.global

  lazy val Postgres: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  lazy val Redis: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
}
