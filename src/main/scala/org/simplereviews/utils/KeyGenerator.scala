package org.simplereviews.utils

import org.simplereviews.models.GeneratedKey

import scala.util.Random

object KeyGenerator {
  def generateKey: GeneratedKey =
    String.valueOf(Random.alphanumeric.take(10).toArray)
}
