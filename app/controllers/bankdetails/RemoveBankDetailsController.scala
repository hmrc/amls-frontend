package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.bankdetails.BankDetails
import utils.RepeatingSection

import scala.concurrent.Future

trait RemoveBankDetailsController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, accountName: String, complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.bankdetails.remove_bank_details(EmptyForm, index, accountName, complete)))
  }

  def remove(index: Int, complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      removeDataStrict[BankDetails](index) map { _ =>
        Redirect(routes.SummaryController.get(complete))
      }
  }
}

object RemoveBankDetailsController extends RemoveBankDetailsController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
