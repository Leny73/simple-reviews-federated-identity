package org.simplereviews.models.dto

import org.simplereviews.models.definitions.Permission

case class Client(id: Client.Id, token: Client.Token, googlePermissions: Permission, facebookPermission: Permission, s3Permission: Permission)

object Client {
  type Id = Long

  type Token = String
}
