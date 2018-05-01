package org.simplereviews.configuration

case class CORSConfiguration(origins: Seq[String], headers: Seq[String])