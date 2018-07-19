/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessMatching, BusinessMatchingMsbService, CurrencyExchange, ForeignExchange, TransmittingMoney, MoneyServiceBusiness => MsbActivity}
import models.moneyservicebusiness.{MoneyServiceBusiness, MostTransactions}
import play.api.mvc.Result
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper

import scala.concurrent.Future

@Singleton
class MostTransactionsController @Inject()(val authConnector: AuthConnector = AMLSAuthConnector,
                                           implicit val cacheConnector: DataCacheConnector,
                                           implicit val statusService: StatusService,
                                           implicit val serviceFlow: ServiceFlow
                                          ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        ControllerHelper.allowedToEdit(MsbActivity, Some(TransmittingMoney)) flatMap {
          case true => cacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
            response =>
              val form = (for {
                msb <- response
                transactions <- msb.mostTransactions
              } yield Form2[MostTransactions](transactions)).getOrElse(EmptyForm)
              Ok(views.html.msb.most_transactions(form, edit))
          }
          case false => Future.successful(NotFound(notFoundView))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[MostTransactions](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.msb.most_transactions(f, edit)))
          case ValidForm(_, data) =>
            cacheConnector.fetchAll flatMap { maybeCache =>
              val result = for {
                cacheMap <- maybeCache
                msb <- cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
                bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                services <- bm.msbServices
                register <- cacheMap.getEntry[ServiceChangeRegister](ServiceChangeRegister.key) orElse Some(ServiceChangeRegister())
              } yield {
                cacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
                  msb.mostTransactions(data)
                ) flatMap { _ =>
                    if (edit) {
                      Future.successful(editRouting(services.msbServices, msb))
                    } else {
                      standardRouting(services.msbServices, register)
                    }
                }
              }

              result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
  }

  private def standardRouting(services: Set[BusinessMatchingMsbService], register: ServiceChangeRegister)
                             (implicit hc: HeaderCarrier, auth: AuthContext): Future[Result] =
    statusService.isPreSubmission map {
      case true =>
        if (services contains CurrencyExchange) {
          Redirect(routes.CETransactionsInNext12MonthsController.get())
        } else if (services contains ForeignExchange) {
          Redirect(routes.FXTransactionsInNext12MonthsController.get())
        } else {
          Redirect(routes.SummaryController.get())
        }
      case _ =>
        if (shouldAnswerCurrencyExchangeQuestions(services, register)) {
          Redirect(routes.CETransactionsInNext12MonthsController.get())
        } else if (shouldAnswerForeignExchangeQuestions(services, register)) {
          Redirect(routes.FXTransactionsInNext12MonthsController.get())
        } else {
          Redirect(routes.SummaryController.get())
        }
    }

  private def editRouting(services: Set[BusinessMatchingMsbService], msb: MoneyServiceBusiness) =
    if ((services contains CurrencyExchange) && msb.ceTransactionsInNext12Months.isEmpty) {
      Redirect(routes.CETransactionsInNext12MonthsController.get(true))
    } else if ((services contains ForeignExchange) && msb.ceTransactionsInNext12Months.isEmpty) {
        Redirect(routes.FXTransactionsInNext12MonthsController.get(true))
    } else {
      Redirect(routes.SummaryController.get())
    }
}
