package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{BusinessMatching, CurrencyExchange, MsbService}
import models.renewal.{MsbMostTransactions, Renewal}
import play.api.mvc.Result
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

@Singleton
class MsbMostTransactionsController @Inject()(val authConnector: AuthConnector,
                                           val cache: DataCacheConnector) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        cache.fetch[Renewal](Renewal.key) map {
          response =>
            val form = (for {
              msb <- response
              transactions <- msb.mostTransactions
            } yield Form2[MsbMostTransactions](transactions)).getOrElse(EmptyForm)
            Ok(views.html.renewal.most_transactions(form, edit))
        }
  }

  private def standardRouting(services: Set[MsbService], edit: Boolean): Result =
    if ((services contains CurrencyExchange) && !edit) {
      Redirect(routes.MsbCurrencyExchangeTransactionsController.get(edit))
    } else {
      Redirect(routes.SummaryController.get())
    }


  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[MsbMostTransactions](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.renewal.most_transactions(f, edit)))
          case ValidForm(_, data) =>
            cache.fetchAll flatMap {
              optMap =>
                val result = for {
                  cacheMap <- optMap
                  renewal <- cacheMap.getEntry[Renewal](Renewal.key)
                  bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                  services <- bm.msbServices
                } yield {
                  cache.save[Renewal](Renewal.key,
                    renewal.mostTransactions(data)
                  ) map { _ =>
                      standardRouting(services.msbServices, edit)
                   }
                }
                result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
  }
}
