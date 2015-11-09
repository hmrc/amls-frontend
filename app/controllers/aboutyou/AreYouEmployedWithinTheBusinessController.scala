package controllers.aboutyou

import config.AMLSAuthConnector
import controllers.auth.AmlsRegime
import forms.AreYouEmployedWithinTheBusinessForms._
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait AreYouEmployedWithinTheBusinessController extends FrontendController with Actions {

  def onPageLoad = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        Future.successful(Ok(views.html.AreYouEmployedWithinTheBusiness(areYouEmployedWithinTheBusinessForm)))
  }

  def onSubmit = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        Future.successful(Ok(views.html.AreYouEmployedWithinTheBusiness(areYouEmployedWithinTheBusinessForm)))
  }
}

object AreYouEmployedWithinTheBusinessController extends AreYouEmployedWithinTheBusinessController {
  val authConnector = AMLSAuthConnector
}