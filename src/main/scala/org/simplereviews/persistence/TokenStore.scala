package org.simplereviews.persistence

import org.simplereviews.models.{ Id, JWT }

import scala.concurrent.Future

trait TokenStore {
  def tokenExistsForUser(userId: Id, tokens: JWT): Future[Boolean]

  def addTokenForUser(userId: Id, tokens: JWT): Future[Seq[JWT]]

  def deleteTokensForUser(userId: Id): Future[Seq[JWT]]
}
