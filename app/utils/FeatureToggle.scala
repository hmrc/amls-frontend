package utils

import play.api.Play
import play.api.mvc._

case class FeatureToggle(feature: Boolean) {
  def apply(action: Action[AnyContent]): Action[AnyContent] =
    if (feature) action else FeatureToggle.notFound
}

object FeatureToggle {
  val notFound = Action.async {
    request =>
      import play.api.Play.current
      Play.global.onHandlerNotFound(request)
  }
}
