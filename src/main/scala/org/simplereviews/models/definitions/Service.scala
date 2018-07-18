package org.simplereviews.models.definitions

import org.simplereviews.models.dto.Client

sealed trait Service {
  def permission: Permission
}

object Service {
  case class Google(permission: Permission) extends Service
  case class Facebook(permission: Permission) extends Service
  case class S3(permission: Permission) extends Service

  def meetsPermissionCriteria(client: Client, service: Service): Boolean =
    service match {
      case x: Google if client.googlePermissions.equals(x.permission) =>
        true
      case x: Facebook if client.facebookPermission.equals(x.permission) =>
        true
      case x: S3 if client.s3Permission.equals(x.permission) =>
        true
      case _ =>
        false
    }
}