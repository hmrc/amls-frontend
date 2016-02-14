package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.bankdetails.{BankAccountType, BankDetails}

import scala.concurrent.Future

trait BankAccountTypeController extends BaseController {

  val dataCacheConnector : DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BankDetails](BankDetails.key) map {
        case Some(BankDetails(Some(data), _)) => Ok(views.html.bank_account_types(Form2[BankAccountType](data), edit))
        case _ => Ok(views.html.bank_account_types(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[BankAccountType](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.bank_account_types(f, edit)))
        case ValidForm(_, data) =>
          for {
            bankDetails <- dataCacheConnector.fetchDataShortLivedCache[BankDetails](BankDetails.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[BankDetails](BankDetails.key,
              bankDetails.bankAccountType(data)
            )
          } yield edit match {
            case true => Redirect(routes.BankAccountTypeController.get())
            case false => Redirect(routes.BankAccountTypeController.get())
          }
      }
    }
  }

}

object BankAccountTypeController extends BankAccountTypeController {
    override val authConnector = AMLSAuthConnector
    override val dataCacheConnector = DataCacheConnector
}