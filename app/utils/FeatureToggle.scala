package utils

import play.api.mvc._
import play.api.mvc.Results.NotFound

case class FeatureToggle(feature: Boolean) {
  def apply[A](action: Action[A]) =
    if (feature) action else FeatureToggle.notFound
}

object FeatureToggle {
  val notFound = Action(NotFound)
}
