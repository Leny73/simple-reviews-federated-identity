package org.simplereviews.persistence

import org.simplereviews.models.{ Id, Token }

import scala.concurrent.Future

trait TokenStore {
  def tokenExistsForUser(userId: Id, tokens: Token): Future[Boolean]

  def addTokenForUser(userId: Id, tokens: Token): Future[Seq[Token]]

  def deleteTokensForUser(userId: Id): Future[Seq[Token]]
}
