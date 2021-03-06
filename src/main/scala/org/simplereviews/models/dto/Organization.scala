package org.simplereviews.models.dto

import org.simplereviews.utils.KeyGenerator

import org.byrde.commons.persistence.sql.slick.sqlbase.BaseEntity

case class Organization(id: Long, name: String, imageToken: String, google: Option[String] = None, facebook: Option[String] = None) extends BaseEntity

object Organization {
  def create(name: String): Organization =
    Organization(0, name.toLowerCase, KeyGenerator.generateKey)
}