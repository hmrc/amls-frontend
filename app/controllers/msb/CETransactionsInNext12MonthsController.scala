package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.{CETransactionsInNext12Months, MoneyServiceBusiness}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.msb.ce_transaction_in_next_12_months

import scala.concurrent.Future

trait CETransactionsInNext12MonthsController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit:Boolean = false) = Authorised.async {
   implicit authContext => implicit request =>
     dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
       response =>
         val form: Form2[CETransactionsInNext12Months] = (for {
           msb <- response
           transactions <- msb.ceTransactionsInNext12Months
         } yield Form2[CETransactionsInNext12Months](transactions)).getOrElse(EmptyForm)
         Ok(ce_transaction_in_next_12_months(form, edit))
     }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CETransactionsInNext12Months](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(ce_transaction_in_next_12_months(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.ceTransactionsInNext12Months(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.SendMoneyToOtherCountryController.get())
          }
      }
    }
  }
}

object CETransactionsInNext12MonthsController extends CETransactionsInNext12MonthsController {
  // $COVERAGE-OFF$
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
