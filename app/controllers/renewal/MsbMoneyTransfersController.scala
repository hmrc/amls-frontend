package controllers.renewal

import javax.inject.Inject

import controllers.BaseController
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.msb_money_transfers

import scala.concurrent.Future

class MsbMoneyTransfersController @Inject()(val authConnector: AuthConnector) extends BaseController {
  def get = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(msb_money_transfers()))
  }
}
