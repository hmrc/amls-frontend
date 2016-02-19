package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.bankdetails.{BankAccount, BankDetails}

import scala.concurrent.Future

trait BankAccountController extends BankAccountUtilController {

  val dataCacheConnector : DataCacheConnector

  def get(index:Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getBankDetails(index) map {
        case Some(BankDetails(_, Some(data))) => Ok(views.html.bank_account_details(Form2[BankAccount](data), edit, index))
        case _ => Ok(views.html.bank_account_details(EmptyForm, edit, index))
      }
  }

  def post(index:Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[BankAccount](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.bank_account_details(f, edit, index)))
        case ValidForm(_, data) => {
          for {
            model <- getBankDetails(index) map {
              case Some(model) => updateBankDetails(index, model.bankAccount(data))
              case _ => updateBankDetails(index, BankDetails(None, Some(data)))
            }
          } yield {Redirect(routes.SummaryController.get())}
        }
      }
    }
  }
}

object BankAccountController extends BankAccountController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
