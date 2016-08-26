package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.bankdetails.{BankAccountType, BankDetails}
import utils.RepeatingSection

import scala.concurrent.Future

trait BankAccountTypeController extends RepeatingSection with BaseController {

  val dataCacheConnector : DataCacheConnector

  def get(index:Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[BankDetails](index) map {
        case Some(BankDetails(Some(data), _)) =>
          Ok(views.html.bankdetails.bank_account_types(Form2[Option[BankAccountType]](Some(data)), edit, index))
        case Some(_) =>
          Ok(views.html.bankdetails.bank_account_types(EmptyForm, edit, index))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index:Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[Option[BankAccountType]](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.bankdetails.bank_account_types(f, edit, index)))
        case ValidForm(_, data) => {
          for {
              result <- updateDataStrict[BankDetails](index) {
                case Some(BankDetails(_, Some(x))) => Some(BankDetails(data, Some(x)))
                case _ => BankAccountType.convert(data)
              }

          } yield {
            data match {
              case Some(_) => Redirect(routes.BankAccountController.get(index, edit))
              case _ => Redirect(routes.SummaryController.get(false))
            }
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
    }
  }
}

object BankAccountTypeController extends BankAccountTypeController {
  // $COVERAGE-OFF$
    override val authConnector = AMLSAuthConnector
    override val dataCacheConnector = DataCacheConnector
}
