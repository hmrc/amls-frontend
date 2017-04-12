package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.renewal.{CETransactionsInLast12Months, Renewal}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.ce_transactions_in_last_12_months

import scala.concurrent.Future

@Singleton
class CETransactionsInLast12MonthsController @Inject()(
                                                           val dataCacheConnector: DataCacheConnector,
                                                           val authConnector: AuthConnector,
                                                           val renewalService: RenewalService
                                                         ) extends BaseController {
  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Renewal](Renewal.key) map {
        response =>
          val form: Form2[CETransactionsInLast12Months] = (for {
            renewal <- response
            transactions <- renewal.ceTransactionsInLast12Months
          } yield Form2[CETransactionsInLast12Months](transactions)).getOrElse(EmptyForm)
          Ok(ce_transactions_in_last_12_months(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CETransactionsInLast12Months](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(ce_transactions_in_last_12_months(f, edit)))
        case ValidForm(_, data) =>
          for {
            renewal <- dataCacheConnector.fetch[Renewal](Renewal.key)
            _ <- dataCacheConnector.save[Renewal](Renewal.key, renewal.ceTransactionsInLast12Months(data))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.WhichCurrenciesController.get(edit))
          }
      }
    }
  }

}
