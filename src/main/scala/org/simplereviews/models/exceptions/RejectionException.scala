package org.simplereviews.models.exceptions

import akka.http.scaladsl.server.Rejection

import scala.util.control.NoStackTrace

class RejectionException extends NoStackTrace with Rejection
