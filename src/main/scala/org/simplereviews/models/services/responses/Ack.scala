package org.simplereviews.models.services.responses

import org.byrde.commons.models.services.DefaultServiceResponse

sealed trait Ack extends DefaultServiceResponse

case object Ack extends Ack {
  override def msg: String =
    "Ok"

  override def status: Int =
    200

  override def code: Int =
    200
}
