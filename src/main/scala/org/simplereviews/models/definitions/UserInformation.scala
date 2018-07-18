package org.simplereviews.models.definitions

trait UserInformation {
  def id: Long

  def orgId: Long

  def isAdmin: Boolean
}
