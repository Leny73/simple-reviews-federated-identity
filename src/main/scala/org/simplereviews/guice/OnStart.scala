package org.simplereviews.guice

import com.google.inject.Inject

import org.simplereviews.models.dto.User

class OnStart @Inject() (modules: Modules) {
  private implicit val _ =
    modules.akka.system.dispatcher

  private val USERNAME =
    "admin"

  private val PASSWORD =
    "admin"

  private def start(): Unit = {
    modules.persistence.applySchema()

    modules.persistence.userDAO.findByUsernameAndPassword(USERNAME, PASSWORD).map(_.getOrElse {
      modules.persistence.userDAO.upsert(User.create(USERNAME, PASSWORD))
    })
  }

  start()
}