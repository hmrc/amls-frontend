package controllers.renewal

import javax.inject.Inject

import cats.data.OptionT
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.renewal.MsbMoneyTransfers
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.msb_money_transfers
import cats.implicits._

import scala.concurrent.Future

class MsbMoneyTransfersController @Inject()(val authConnector: AuthConnector, renewalService: RenewalService) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        val block = for {
          renewal <- OptionT(renewalService.getRenewal)
          transfers <- OptionT.fromOption[Future](renewal.msbTransfers)
        } yield {
          Ok(msb_money_transfers(Form2[MsbMoneyTransfers](transfers), edit))
        }

        block getOrElse Ok(msb_money_transfers(EmptyForm, edit))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
        Form2[MsbMoneyTransfers](request.body) match {
          case f: InvalidForm => Future.successful(BadRequest(msb_money_transfers(f, edit)))
          case ValidForm(_, model) =>
            val maybeResponse = for {
              renewal <- OptionT(renewalService.getRenewal)
              _ <- OptionT.liftF(renewalService.updateRenewal(renewal.msbTransfers(model)))
            } yield {
              Redirect(nextPageUrl(edit))
            }

            maybeResponse getOrElse Redirect(routes.SummaryController.get())
        }
  }

  private def nextPageUrl(edit: Boolean) = {
    if (edit) {
      routes.SummaryController.get()
    } else {
      routes.MsbSendTheLargestAmountsOfMoneyController.get()
    }
  }
}
