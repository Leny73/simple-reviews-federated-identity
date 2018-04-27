package org.simplereviews.services

trait NameForLogging {
  def nameForLoggingString: String = {
    val clazz = this.getClass

    if (clazz.isLocalClass) {
      val className =
        clazz.getGenericSuperclass.getTypeName

      val finalNameSpaceIndex =
        className.lastIndexOf(".")

      className.substring(finalNameSpaceIndex)
    } else {
      clazz.getSimpleName
    }
  }

  def host: String
}