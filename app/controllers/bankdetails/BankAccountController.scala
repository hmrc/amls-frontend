package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.bankdetails.{BankAccount, BankDetails}
import utils.{RepeatingSection, RepeatingSectionFlow}

import scala.concurrent.Future

trait BankAccountController extends RepeatingSection with BaseController {

  val dataCacheConnector : DataCacheConnector

  def get(index:Int = 0, flow: RepeatingSectionFlow) = Authorised.async {
    implicit authContext => implicit request =>
      getData[BankDetails](index) map { oBankDetails =>
        (flow, oBankDetails) match {
          case (RepeatingSectionFlow.Add, _) =>
            Ok(views.html.bankdetails.bank_account_details(EmptyForm, flow, index))
          case (_, Some(BankDetails(_, Some(data)))) =>
            Ok(views.html.bankdetails.bank_account_details(Form2[BankAccount](data), flow, index))
          case _ =>
            BadRequest
        }
      }
  }

  def post(index:Int = 0, flow: RepeatingSectionFlow) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[BankAccount](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.bankdetails.bank_account_details(f, flow, index)))
        case ValidForm(_, data) => {
          for {
            _ <- updateData[BankDetails](index) {
              case Some(BankDetails(Some(x), _)) => Some(BankDetails(Some(x), Some(data)))
              case _ => data
            }
          } yield {Redirect(routes.SummaryController.get())}
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
