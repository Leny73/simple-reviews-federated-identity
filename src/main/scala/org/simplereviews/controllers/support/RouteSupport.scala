package org.simplereviews.controllers.support

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import org.simplereviews.models.exceptions.RejectionException

import org.byrde.commons.models.services.ServiceResponse

import play.api.libs.json.{ Json, Writes }

import akka.http.scaladsl.server.Directives.{ complete, _ }
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import scala.util.{ Failure, Success }

trait RouteSupport extends PlayJsonSupport {
  def asyncJson[T](
    fn: Future[T],
    Err: Throwable => Throwable = identity
  )(implicit writes: Writes[T]): Route =
    async(fn, (res: T) => complete(Json.toJson(res)), Err)

  def asyncWithDefaultJsonResponse[T, TT <: ServiceResponse[_]](
    fn: Future[T],
    Ok: TT,
    Err: Throwable => Throwable = identity
  ): Route =
    async(fn, (_: T) => complete(Ok.toJson), Err)

  def async[T](
    fn: Future[T],
    Ok: T => Route,
    Err: Throwable => Throwable = identity
  ): Route =
    onComplete(fn) {
      case Success(res) =>
        Ok(res)
      case Failure(ex) =>
        ex match {
          case ex: RejectionException =>
            reject(ex)
          case _ =>
            throw Err(ex)
        }
    }
}
