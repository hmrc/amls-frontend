package controllers.renewal

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{BusinessMatching, HighValueDealing}
import models.moneyservicebusiness.{MoneyServiceBusiness, WhichCurrencies}
import models.renewal.MsbWhichCurrencies
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.renewal.msb_which_currencies

import scala.concurrent.Future

class MsbWhichCurrenciesController @Inject()
(
  val authConnector: AuthConnector,
  renewalService: RenewalService
) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      val block = for {
        renewal <- OptionT(renewalService.getRenewal)
        whichCurrencies <- OptionT.fromOption[Future](renewal.msbWhichCurrencies)
      } yield {
        Ok(msb_which_currencies(Form2[MsbWhichCurrencies](whichCurrencies), edit))
      }

      block getOrElse Ok(msb_which_currencies(EmptyForm, edit))

  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[MsbWhichCurrencies](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(msb_which_currencies(f, edit)))
        case ValidForm(_, model) =>
          val maybeResponse = for {
            renewal <- OptionT(renewalService.getRenewal)
            _ <- OptionT.liftF(renewalService.updateRenewal(renewal.msbWhichCurrencies(model)))
          } yield {
            Redirect(routes.SummaryController.get())
          }

          maybeResponse getOrElse Redirect(routes.SummaryController.get())
      }
  }

}
