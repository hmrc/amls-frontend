package controllers.aboutyou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.ExampleController._
import forms.AreYouEmployedWithinTheBusinessForms._
import play.api.mvc.Action
import services.AreYouEmployedWithinTheBusinessService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.hello_world

trait AreYouEmployedWithinTheBusinessController extends FrontendController with Actions {

  def areYouEmployedWithinTheBusinessService: AreYouEmployedWithinTheBusinessService

  def dataCacheConnector: DataCacheConnector

  def onPageLoad = Action {
    implicit request =>
      Ok(views.html.AreYouEmployedWithinTheBusiness(areYouEmployedWithinTheBusinessForm))
  }

  def onSubmit = Action {
    implicit request =>
      form.bindFromRequest().fold(
        errorForm => BadRequest(hello_world(errorForm)),
        success => Redirect(routes.AreYouEmployedWithinTheBusinessController.onPageLoad)
      )
  }

}

object AreYouEmployedWithinTheBusinessController extends AreYouEmployedWithinTheBusinessController {
  override val authConnector = AMLSAuthConnector
  override lazy val areYouEmployedWithinTheBusinessService = AreYouEmployedWithinTheBusinessService
  override lazy val dataCacheConnector = DataCacheConnector
}