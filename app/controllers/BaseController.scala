package controllers

import config.ApplicationConfig
import controllers.auth.AmlsRegime
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{ControllerHelper, FeatureToggle}

trait BaseController extends FrontendController with Actions  with I18nSupport {

  protected def Authorised = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence)
  protected def AmendmentsToggle = FeatureToggle(ApplicationConfig.amendmentsToggle)

  def notFoundView(implicit request: Request[_]) = ControllerHelper.notFoundView(request)

   import play.api.Play.current
   implicit def messagesApi = Messages.Implicits.applicationMessages.messages

}
