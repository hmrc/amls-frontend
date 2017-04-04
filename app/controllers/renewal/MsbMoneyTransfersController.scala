package controllers.renewal

import javax.inject.Inject

import cats.data.OptionT
import controllers.BaseController
import forms.Form2
import models.renewal.MsbMoneyTransfers
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.msb_money_transfers

import scala.concurrent.Future

class MsbMoneyTransfersController @Inject()(val authConnector: AuthConnector, renewalService: RenewalService) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(msb_money_transfers(edit)))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => ???
  }
}
