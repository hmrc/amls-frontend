package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.{TransactionsInNext12Months, MoneyServiceBusiness}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.msb.transactions_in_next_12_months

import scala.concurrent.Future

trait TransactionsInNext12MonthsController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit:Boolean = false) = Authorised.async {
   implicit authContext => implicit request =>
     dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
       response =>
         val form: Form2[TransactionsInNext12Months] = (for {
           msb <- response
           transactions <- msb.transactionsInNext12Months
         } yield Form2[TransactionsInNext12Months](transactions)).getOrElse(EmptyForm)
         Ok(transactions_in_next_12_months(form, edit))
     }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[TransactionsInNext12Months](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(transactions_in_next_12_months(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.transactionsInNext12Months(data)
            )
          } yield edit match {
            case true if msb.sendMoneyToOtherCountry.isDefined => Redirect(routes.SummaryController.get())
            case _ => Redirect(routes.SendMoneyToOtherCountryController.get(edit))
          }
      }
    }
  }
}

object TransactionsInNext12MonthsController extends TransactionsInNext12MonthsController {
  // $COVERAGE-OFF$
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
