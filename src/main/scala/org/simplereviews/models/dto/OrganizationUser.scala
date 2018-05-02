package org.simplereviews.models.dto

import org.byrde.commons.persistence.sql.slick.sqlbase.BaseEntity

case class OrganizationUser(id: Long, organizationId: Long, userId: Long) extends BaseEntity

object OrganizationUser {
  def create(organization: Organization, user: User): OrganizationUser =
    create(organization.id, user.id)

  def create(organizationId: Long, userId: Long): OrganizationUser =
    OrganizationUser(0, organizationId, userId)
}