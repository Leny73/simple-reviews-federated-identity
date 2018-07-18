package org.simplereviews.models.definitions

import org.simplereviews.models.JWT

trait RawJwt {
  def raw: JWT
}
