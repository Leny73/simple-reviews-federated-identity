package org.simplereviews.models.dto

import org.simplereviews.models.{ Id, Permission, Token }

import org.byrde.commons.persistence.sql.slick.sqlbase.BaseEntity

case class Client(id: Id, token: Token, orgPermission: Permission, googlePermissions: Permission, facebookPermission: Permission, s3Permission: Permission) extends BaseEntity