package controllers.bankdetails

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.bankdetails.BankDetails
import models.responsiblepeople.BankAccountRegistered
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}


import scala.concurrent.Future

trait BankAccountRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int) =
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetch[Seq[BankDetails]](BankDetails.key) map {
          case Some(data) => Ok(views.html.bankdetails.bank_account_registered(EmptyForm, data.size))
          case _ => Ok(views.html.bankdetails.bank_account_registered(EmptyForm, index))
        }
    }


  def post(index: Int) =
     Authorised.async {
        implicit authContext => implicit request =>
          Form2[BankAccountRegistered](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(views.html.bankdetails.bank_account_registered(f, index)))
            case ValidForm(_, data) =>
               data.registerAnotherBank match {
                case true => Future.successful(Redirect(routes.BankAccountAddController.get(false)))
                case false => Future.successful(Redirect(routes.SummaryController.get()))
              }
          }
      }

}

object BankAccountRegisteredController extends BankAccountRegisteredController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
