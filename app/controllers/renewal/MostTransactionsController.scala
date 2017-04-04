package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{BusinessMatching, CurrencyExchange, MsbService}
import models.moneyservicebusiness.{MoneyServiceBusiness, MostTransactions}
import play.api.mvc.Result
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper

import scala.concurrent.Future

@Singleton
class MostTransactionsController @Inject()(val authConnector: AuthConnector,
                                            val cache: DataCacheConnector)
                                          (implicit val statusService: StatusService) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ControllerHelper.allowedToEdit flatMap {
        case true => cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
          response =>
            val form = (for {
              msb <- response
              transactions <- msb.mostTransactions
            } yield Form2[MostTransactions](transactions)).getOrElse(EmptyForm)
          Ok(views.html.renewal.most_transactions(form, edit))
        }
        case false => Future.successful(NotFound(notFoundView))
      }
  }

  private def standardRouting(services: Set[MsbService]): Result =
    if (services contains CurrencyExchange) {
      Redirect(routes.SummaryController.get())
    } else {
      Redirect(routes.SummaryController.get())
    }

  private def editRouting(services: Set[MsbService], msb: MoneyServiceBusiness): Result =
    if ((services contains CurrencyExchange) &&
      msb.ceTransactionsInNext12Months.isEmpty) {
        Redirect(routes.SummaryController.get())
    } else {
      Redirect(routes.SummaryController.get())
    }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[MostTransactions](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.msb.most_transactions(f, edit)))
        case ValidForm(_, data) =>
          cache.fetchAll flatMap {
            optMap =>
              val result = for {
                cacheMap <- optMap
                msb <- cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
                bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                services <- bm.msbServices
              } yield {
                cache.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
                  msb.mostTransactions(data)
                ) map {
                  _ =>
                    edit match {
                      case false => standardRouting(services.msbServices)
                      case true => editRouting(services.msbServices, msb)
                    }
                }
              }
              result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
          }
      }
  }
}
