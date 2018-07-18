package org.simplereviews.models.dto

import org.simplereviews.models.{ Id, Token }
import org.simplereviews.models.definitions.Permission

case class Client(id: Id, token: Token, googlePermissions: Permission, facebookPermission: Permission, s3Permission: Permission)