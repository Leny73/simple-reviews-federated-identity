package org.simplereviews.models

trait UserInformation {
  def id: Long

  def orgId: Long

  def isAdmin: Boolean
}
