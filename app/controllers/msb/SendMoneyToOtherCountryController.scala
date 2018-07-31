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

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessMatching, BusinessMatchingMsbService, CurrencyExchange, ForeignExchange}
import models.moneyservicebusiness.{MoneyServiceBusiness, SendMoneyToOtherCountry}
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.msb.send_money_to_other_country

import scala.concurrent.Future

class SendMoneyToOtherCountryController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                  val authConnector: AuthConnector,
                                                  val statusService: StatusService
                                                 ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map { response =>
          val form: Form2[SendMoneyToOtherCountry] = (for {
            msb <- response
            money <- msb.sendMoneyToOtherCountry
          } yield Form2[SendMoneyToOtherCountry](money)).getOrElse(EmptyForm)

          Ok(send_money_to_other_country(form, edit))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        Form2[SendMoneyToOtherCountry](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(send_money_to_other_country(f, edit)))
          case ValidForm(_, data) =>
            dataCacheConnector.fetchAll flatMap { maybeCache =>
              val result = for {
                cache <- maybeCache
                msb <- cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
                bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
                services <- bm.msbServices
                register <- cache.getEntry[ServiceChangeRegister](ServiceChangeRegister.key) orElse Some(ServiceChangeRegister())
              } yield {
                dataCacheConnector.save(MoneyServiceBusiness.key, msb.sendMoneyToOtherCountry(data)) map { _ =>
                  routing(data.money, services.msbServices,register, msb, edit)
                }
              }
              result.map(_.flatMap(identity)) getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
      }
  }

  private def shouldAnswerCurrencyExchangeQuestion(
                                                          services: Set[BusinessMatchingMsbService],
                                                          register: ServiceChangeRegister,
                                                          msb: MoneyServiceBusiness,
                                                          isPreSubmission: Boolean
                                                  ): Boolean = {
    currencyExchangeAddedPostSubmission(services, register) ||
            (isPreSubmission && services.contains(CurrencyExchange) && msb.sendTheLargestAmountsOfMoney.isEmpty)
  }

  private def shouldAnswerForeignExchangeQuestion(
                                                         services: Set[BusinessMatchingMsbService],
                                                         register: ServiceChangeRegister,
                                                         msb: MoneyServiceBusiness,
                                                         isPreSubmission: Boolean
                                                 ): Boolean = {
    foreignExchangeAddedPostSubmission(services, register) ||
            (isPreSubmission && services.contains(ForeignExchange) && msb.sendTheLargestAmountsOfMoney.isEmpty)
  }

  private def routing(
                             shouldRouteToNext: Boolean,
                             services: Set[BusinessMatchingMsbService],
                             register: ServiceChangeRegister,
                             msb: MoneyServiceBusiness,
                             edit: Boolean
                     )
                             (implicit ac: AuthContext, hc: HeaderCarrier) = {
    statusService.isPreSubmission map { isPreSubmission =>
      if (shouldRouteToNext) {
        Redirect(routes.SendTheLargestAmountsOfMoneyController.get(edit))
      } else if (shouldAnswerCurrencyExchangeQuestion(services, register, msb, isPreSubmission)) {
        Redirect(routes.CETransactionsInNext12MonthsController.get(edit))
      } else if (shouldAnswerForeignExchangeQuestion(services, register, msb, isPreSubmission)) {
        Redirect(routes.FXTransactionsInNext12MonthsController.get(edit))
      } else {
        Redirect(routes.SummaryController.get())
      }
    }
  }
}
