package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.bankdetails.{BankAccountType, BankDetails}

import scala.concurrent.Future

trait BankAccountTypeController extends BankAccountUtilController {

  val dataCacheConnector : DataCacheConnector

  def get(index:Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getBankDetails(index) map {
        case Some(BankDetails(Some(data), _)) =>
          Ok(views.html.bank_account_types(Form2[Option[BankAccountType]](Some(data)), edit, index))
        case a => {
          Ok(views.html.bank_account_types(EmptyForm, edit, index))
        }
      }
  }

  def post(index:Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[Option[BankAccountType]](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.bank_account_types(f, edit, index)))
        case ValidForm(_, data) => {
          for {
              result <- updateBankDetails[BankDetails](index) {
                case Some(BankDetails(_, Some(x))) => {
                  Some(BankDetails(data, Some(x)))
                }
                case _ => {
                  Some(BankDetails(data, None))
                }
              }
          } yield {
              data match {
                case Some(_) => Redirect(routes.BankAccountController.get(index))
                case _ => Redirect(routes.SummaryController.get())
              }
          }
        }
      }
    }
  }
}

object BankAccountTypeController extends BankAccountTypeController {
    override val authConnector = AMLSAuthConnector
    override val dataCacheConnector = DataCacheConnector
}