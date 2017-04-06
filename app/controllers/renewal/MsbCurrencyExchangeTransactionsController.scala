package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.renewal.{CETransactions, Renewal}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.ce_transactions

import scala.concurrent.Future

@Singleton
class MsbCurrencyExchangeTransactionsController @Inject()(
                                                           val dataCacheConnector: DataCacheConnector,
                                                           val authConnector: AuthConnector,
                                                           val renewalService: RenewalService
                                                         ) extends BaseController {
  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Renewal](Renewal.key) map {
        response =>
          val form: Form2[CETransactions] = (for {
            renewal <- response
            transactions <- renewal.ceTransactions
          } yield Form2[CETransactions](transactions)).getOrElse(EmptyForm)
          Ok(ce_transactions(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CETransactions](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(ce_transactions(f, edit)))
        case ValidForm(_, data) =>
          for {
            renewal <- dataCacheConnector.fetch[Renewal](Renewal.key)
            _ <- dataCacheConnector.save[Renewal](Renewal.key, renewal.ceTransactions(data))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.MsbWhichCurrenciesController.get(edit))
          }
      }
    }
  }

}
