package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.{CurrencyExchange, MoneyServiceBusiness, MostTransactions, MsbService}
import play.api.mvc.Result
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait MostTransactionsController extends BaseController {

  def cache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>

      cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>

          val form = (for {
            msb <- response
            transactions <- msb.mostTransactions
          } yield Form2[MostTransactions](transactions)).getOrElse(EmptyForm)

          Ok(views.html.msb.most_transactions(form, edit))
      }
  }

  private def standardRouting(services: Set[MsbService]): Result =
    if (services contains CurrencyExchange) {
      Redirect(routes.CETransactionsInNext12MonthsController.get(false))
    } else {
      Redirect(routes.SummaryController.get())
    }

  private def editRouting(services: Set[MsbService], msb: MoneyServiceBusiness): Result =
    if ((services contains CurrencyExchange) &&
      msb.ceTransactionsInNext12Months.isEmpty) {
        Redirect(routes.CETransactionsInNext12MonthsController.get(true))
    } else {
      Redirect(routes.SummaryController.get())
    }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[MostTransactions](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.msb.most_transactions(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- cache.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.mostTransactions(data)
            )
          } yield {

            val services = msb.msbServices.map(_.services).getOrElse(Set.empty)

            edit match {
              case false =>
                standardRouting(services)
              case true =>
                editRouting(services, msb)
            }
          }
      }
  }
}

object MostTransactionsController extends MostTransactionsController {
  override val cache: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}
