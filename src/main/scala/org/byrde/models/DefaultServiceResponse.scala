package challenge.models

import play.api.libs.json.{ JsString, Writes }

trait DefaultServiceResponse extends ServiceResponse[String] {
  override implicit val writes: Writes[String] =
    (o: String) => JsString(o)
  override val response: String =
    msg
}
