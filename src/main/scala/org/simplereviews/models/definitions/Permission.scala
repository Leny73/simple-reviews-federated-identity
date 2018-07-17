package org.simplereviews.models.definitions

sealed trait Permission {
  def value: String
}

object Permission {
  private val reads =
    "r"

  private val writes =
    "w"

  private val all =
    "x"

  object Reads extends Permission {
    override def value: String =
      reads
  }

  object Writes extends Permission {
    override def value: String =
      writes
  }

  object All extends Permission {
    override def value: String =
      all
  }

  def apply: String => Permission = {
    case x if x.equals(reads) =>
      Reads
    case x if x.equals(writes) =>
      Writes
    case x if x.equals(all) =>
      All
  }
}