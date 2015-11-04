package controllers

import config.AMLSAuthConnector
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

trait AboutYouController extends FrontendController with Actions {



}
object AboutYouController {
  val authConnector = AMLSAuthConnector
}
