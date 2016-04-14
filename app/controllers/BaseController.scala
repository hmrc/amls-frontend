package controllers

import config.ApplicationConfig
import controllers.auth.AmlsRegime
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.FeatureToggle

trait BaseController extends FrontendController with Actions {

  protected val Authorised = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence)
  protected val ResponsiblePeopleToggle = FeatureToggle(ApplicationConfig.responsiblePeopleToggle)
}
