package challenge.models.exceptions

import challenge.models.{ DefaultServiceResponse, ServiceResponse }

import play.api.libs.json.Writes

case class ServiceResponseException(private val _msg: String, private val _code: Int, private val _status: Int) extends Exception(_msg) with DefaultServiceResponse {
  override val msg: String =
    _msg
  override val status: Int =
    _status
  override val code: Int =
    _code
}

object ServiceResponseException {
  def apply[T](serviceResponse: ServiceResponse[T])(implicit writes: Writes[T]): ServiceResponseException =
    ServiceResponseException(serviceResponse.msg, serviceResponse.code, serviceResponse.status)

  def apply(throwable: Throwable): ServiceResponseException =
    apply(new Exception(throwable))

  def apply(ex: Exception): ServiceResponseException =
    E0500.copy(_msg = ex.getMessage)

  object E0400 extends ServiceResponseException("Bad Request", 400, 400)
  object E0500 extends ServiceResponseException("Internal Server Error", 500, 500)
}
