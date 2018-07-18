package org.simplereviews.models.definitions

sealed trait Permission {
  def value: String

  def equals(permission: Permission): Boolean
}

object Permission {
  private val none =
    "-"

  private val reads =
    "r"

  private val writes =
    "w"

  private val all =
    "x"

  object None extends Permission {
    override def value: String =
      none

    override def equals(permission: Permission): Boolean =
      false
  }

  object Reads extends Permission {
    override def value: String =
      reads

    override def equals(permission: Permission): Boolean =
      permission match {
        case Permission.Reads =>
          true
        case _ =>
          false
      }
  }

  object Writes extends Permission {
    override def value: String =
      writes

    override def equals(permission: Permission): Boolean =
      permission match {
        case Permission.Writes =>
          true
        case _ =>
          false
      }
  }

  object All extends Permission {
    override def value: String =
      all

    override def equals(permission: Permission): Boolean =
      permission match {
        case Permission.Reads | Permission.Writes | Permission.All =>
          true
        case _ =>
          false
      }
  }

  def apply: String => Permission = {
    case x if x.equals(none) =>
      None
    case x if x.equals(reads) =>
      Reads
    case x if x.equals(writes) =>
      Writes
    case x if x.equals(all) =>
      All
  }
}