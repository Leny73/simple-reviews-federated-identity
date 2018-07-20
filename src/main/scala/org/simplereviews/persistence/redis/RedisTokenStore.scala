package org.simplereviews.persistence.redis

import org.sedis.Pool
import org.simplereviews.models.{ Id, Token }
import org.simplereviews.persistence.TokenStore

import org.byrde.commons.persistence.redis.RedisClient
import org.byrde.commons.utils.redis.conf.RedisConfig

import scala.concurrent.{ ExecutionContext, Future }

class RedisTokenStore(namespace: String, pool: Pool, classLoader: ClassLoader)(implicit val ec: ExecutionContext) extends RedisClient(namespace, pool, classLoader) with TokenStore {
  override def tokenExistsForUser(userId: Id, token: Token): Future[Boolean] =
    get[Seq[Token]](userId.toString)
      .map(_.getOrElse(Nil))
      .map(_.contains(token))

  override def addTokenForUser(userId: Id, token: Token): Future[Seq[Token]] =
    get[Seq[Token]](userId.toString)
      .map(_.getOrElse(Nil))
      .map { jwts =>
        val newJwts =
          jwts :+ token

        set(userId.toString, newJwts)

        newJwts
      }

  override def deleteTokensForUser(userId: Id): Future[Seq[Token]] =
    get[Seq[Token]](userId.toString)
      .map(_.getOrElse(Nil))
      .map { jwts =>
        remove(userId.toString)
        jwts
      }
}

object RedisTokenStore {
  def apply(redisConfig: RedisConfig, classLoader: ClassLoader)(implicit ec: ExecutionContext): RedisClient =
    new RedisClient(redisConfig.namespace, redisConfig.pool, classLoader)
}
