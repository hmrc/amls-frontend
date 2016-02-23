package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.bankdetails.{BankAccount, BankDetails}
import play.api.Logger

import scala.concurrent.Future

trait BankAccountController extends BankAccountUtilController {

  val dataCacheConnector : DataCacheConnector

  def get(index:Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getBankDetails(index) map {
        case Some(BankDetails(_, Some(data))) =>
          Logger.info(data.toString)
          Ok(views.html.bank_account_details(Form2[BankAccount](data), edit, index))
        case _ =>
          Ok(views.html.bank_account_details(EmptyForm, edit, index))
      }
  }

  def post(index:Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[BankAccount](request.body)(BankAccount.formRule) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.bank_account_details(f, edit, index)))
        case ValidForm(_, data) => {
          for {
            result <- updateBankDetails(index) {
              a =>
                Logger.info("second controller: " + a.toString)
                a match {
                  case Some(BankDetails(Some(x), _)) => Some(BankDetails(Some(x), Some(data)))
                  case _ => data
                }
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
