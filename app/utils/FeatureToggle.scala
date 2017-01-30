package utils

import play.api.http.{Status, DefaultHttpErrorHandler}
import play.api.mvc._

case class FeatureToggle(feature: Boolean) {
  def apply(action: Action[AnyContent]): Action[AnyContent] =
    if (feature) action else FeatureToggle.notFound
}

object FeatureToggle {
  val notFound = Action.async {
    request =>
      DefaultHttpErrorHandler.onClientError(request, Status.NOT_FOUND, "")
  }
}
