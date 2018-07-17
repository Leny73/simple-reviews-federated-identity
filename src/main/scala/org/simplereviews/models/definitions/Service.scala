package org.simplereviews.models.definitions

sealed trait Service {
  def permission: Permission
}

object Service {
  class Google {
    def apply(permission: Permission): Google with Service =
      new Google with Service {
        override def permission: Permission =
          permission
      }
  }

  class Facebook {
    def apply(permission: Permission): Facebook with Service =
      new Facebook with Service {
        override def permission: Permission =
          permission
      }
  }

  val Facebook: Facebook =
    new Facebook

  class S3 {
    def apply(permission: Permission): S3 with Service =
      new S3 with Service {
        override def permission: Permission =
          permission
      }
  }

  val S3: S3 =
    new S3
}