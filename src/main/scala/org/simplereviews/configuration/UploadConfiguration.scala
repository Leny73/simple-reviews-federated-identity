package org.simplereviews.configuration

import scala.concurrent.duration._

case class UploadConfiguration(duration: FiniteDuration, maxContentLength: Long)