package org.simplereviews.models

import org.simplereviews.models.dto.Client

sealed trait Service {
  def name: String

  def permission: Permission
}

object Service {
  case class Org(permission: Permission) extends Service {
    override def name: String =
      "Org"
  }

  case class Google(permission: Permission) extends Service {
    override def name: String =
      "Google"
  }

  case class Facebook(permission: Permission) extends Service {
    override def name: String =
      "Facebook"
  }

  case class S3(permission: Permission) extends Service {
    override def name: String =
      "S3"
  }

  def meetsPermissionCriteria(client: Client, service: Service): Boolean =
    service match {
      case x: Org if client.orgPermission.equals(x.permission) =>
        true
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