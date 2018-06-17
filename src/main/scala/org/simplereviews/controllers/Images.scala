package org.simplereviews.controllers

class Images {
  //TODO: Migrate to SimpleReviews API
  //  def uploadImage(filename: String, bucket: String, key: String): Route =
  //    fileUpload(filename) {
  //      case (metadata, _) if !(metadata.contentType.mediaType == `image/png`) && !(metadata.contentType.mediaType == `image/jpeg`) =>
  //        complete(E0415)
  //      case (metadata, byteSource) =>
  //        async[UniversalEntity]({
  //          modulesProvider.s3ServiceWrapper.upload(imageBucket, key, byteSource, metadata.contentType).flattenTry.map(_.toUniversalEntity)
  //        }, entity => complete(HttpResponse(entity = entity)))
  //      case _ =>
  //        complete(E0400)
  //    }
}
