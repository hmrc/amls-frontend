package controllers.msb

import config.AMLSAuthConnector
import controllers.BaseController
import forms.Form2
import models.moneyservicebusiness.{WholesalerMoneySource, BankMoneySource, CustomerMoneySource, WhichCurrencies}
import play.api.Logger
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait WhichCurrenciesController extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      val model = WhichCurrencies(Seq("USD", "CHF", "EUR"), Some(BankMoneySource("Bank names")), Some(WholesalerMoneySource("wholesaler names")), true)
      Future.successful(Ok(views.html.msb.which_currencies(Form2[WhichCurrencies](model), edit)))
    }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {

      Logger.debug(s"${request.body.asFormUrlEncoded}")
      val model = WhichCurrencies(Seq("USD", "CHF", "EUR"), Some(BankMoneySource("Bank names")), Some(WholesalerMoneySource("wholesaler names")), true)
      Future.successful(Ok(views.html.msb.which_currencies(Form2[WhichCurrencies](model),edit)))
    }
  }
}

object WhichCurrenciesController extends WhichCurrenciesController {
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
