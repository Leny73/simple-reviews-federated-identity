package org.simplereviews.models.exceptions

import org.simplereviews.models.{ DefaultServiceResponse, ServiceResponse }

import play.api.libs.json.Writes

case class ServiceResponseException(private val _msg: String, private val _code: Int, private val _status: Int) extends Exception(_msg) with DefaultServiceResponse {
  override val msg: String =
    _msg

  override val status: Int =
    _status

  override val code: Int =
    _code

  override val response: String =
    _msg
}

object ServiceResponseException {
  def apply[T](serviceResponse: ServiceResponse[T])(implicit writes: Writes[T]): ServiceResponseException =
    ServiceResponseException(serviceResponse.msg, serviceResponse.code, serviceResponse.status)

  def apply(throwable: Throwable): ServiceResponseException =
    apply(new Exception(throwable))

  def apply(ex: Exception): ServiceResponseException =
    E0500.copy(_msg = ex.getMessage)

  object E0400 extends ServiceResponseException("Bad Request", 400, 400)
  object E0401 extends ServiceResponseException("Unauthorized", 401, 401)
  object E0403 extends ServiceResponseException("Forbidden", 403, 403)
  object E0404 extends ServiceResponseException("Not Found", 404, 404)
  object E0500 extends ServiceResponseException("Internal Server Error", 500, 500)
}
