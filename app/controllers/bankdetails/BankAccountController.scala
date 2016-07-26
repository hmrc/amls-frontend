package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.bankdetails.{BankAccount, BankDetails}
import utils.{RepeatingSection}

import scala.concurrent.Future

trait BankAccountController extends RepeatingSection with BaseController {

  val dataCacheConnector : DataCacheConnector

  def get(index:Int, edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[BankDetails](index) map {
        case Some(BankDetails(_, Some(data))) =>
          Ok(views.html.bankdetails.bank_account_details(Form2[BankAccount](data), edit, index))
        case Some(_) =>
          Ok(views.html.bankdetails.bank_account_details(EmptyForm, edit, index))
        case _ => {
          NotFound
        }
      }
  }

  def post(index:Int, edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[BankAccount](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.bankdetails.bank_account_details(f, edit, index)))
        case ValidForm(_, data) => {
          for {
            result <- updateDataStrict[BankDetails](index) {
              case Some(BankDetails(Some(x), _)) => Some(BankDetails(Some(x), Some(data)))
              case _ => Some(BankDetails(None, Some(data)))
            }
          } yield Redirect(routes.SummaryController.get())
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(BadRequest)
        }
      }
    }
  }
}

object BankAccountController extends BankAccountController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
