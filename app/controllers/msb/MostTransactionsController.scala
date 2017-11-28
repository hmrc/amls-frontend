/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{BusinessMatching, CurrencyExchange, MsbService, MoneyServiceBusiness => MsbActivity}
import models.moneyservicebusiness.{MoneyServiceBusiness, MostTransactions}
import play.api.mvc.Result
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper

import scala.concurrent.Future

@Singleton
class MostTransactionsController @Inject()(
                                            val authConnector: AuthConnector = AMLSAuthConnector,
                                            val cacheConnector: DataCacheConnector,
                                            implicit val statusService: StatusService,
                                            implicit val serviceFlow: ServiceFlow
                                          ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ControllerHelper.allowedToEdit(MsbActivity) flatMap {
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
          cacheConnector.fetchAll flatMap {
            optMap =>
              val result = for {
                cacheMap <- optMap
                msb <- cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
                bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                services <- bm.msbServices
              } yield {
                cacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
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
