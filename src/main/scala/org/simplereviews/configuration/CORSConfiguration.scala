package org.simplereviews.configuration

case class CORSConfiguration(origins: Seq[String], methods: Seq[String], headers: Seq[String])