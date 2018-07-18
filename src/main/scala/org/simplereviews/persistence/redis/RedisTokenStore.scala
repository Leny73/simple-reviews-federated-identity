package org.simplereviews.persistence.redis

import org.simplereviews.models.{ Id, JWT }
import org.simplereviews.persistence.TokenStore

import org.byrde.commons.persistence.redis.RedisClient
import org.byrde.commons.utils.redis.conf.RedisConfig

import scala.concurrent.{ ExecutionContext, Future }

//TODO: Update configuration file
class RedisTokenStore(redisConfig: RedisConfig, classLoader: ClassLoader)(implicit val ec: ExecutionContext) extends RedisClient(redisConfig.namespace, redisConfig.pool, classLoader) with TokenStore {
  override def tokenExistsForUser(userId: Id, token: JWT): Future[Boolean] =
    get[Seq[JWT]](userId.toString)
      .map(_.getOrElse(Nil))
      .map(_.contains(token))

  override def addTokenForUser(userId: Id, token: JWT): Future[Seq[JWT]] =
    get[Seq[JWT]](userId.toString)
      .map(_.getOrElse(Nil))
      .map { jwts =>
        val newJwts =
          jwts :+ token

        set(userId.toString, newJwts)

        newJwts
      }

  override def deleteTokensForUser(userId: Id): Future[Seq[JWT]] =
    get[Seq[JWT]](userId.toString)
      .map(_.getOrElse(Nil))
      .map { jwts =>
        remove(userId.toString)
        jwts
      }
}
