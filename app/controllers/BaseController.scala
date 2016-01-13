package controllers

import controllers.auth.AmlsRegime
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

trait BaseController extends FrontendController with Actions {

  protected val Authorised = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence)
}
