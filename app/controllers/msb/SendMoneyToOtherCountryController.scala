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
import models.businessmatching.{BusinessMatching, BusinessMatchingMsbService, CurrencyExchange}
import models.moneyservicebusiness.{MoneyServiceBusiness, SendMoneyToOtherCountry}
import play.api.mvc.Result
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
                  if (edit) {
                    Future.successful(editRouting(data.money, services.msbServices, msb))
                  } else {
                    standardRouting(data.money, services.msbServices, register)
                  }
                }
              }

              result.map(_.flatMap(identity)) getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
      }
  }

  private def standardRouting(shouldRouteToNext: Boolean, services: Set[BusinessMatchingMsbService], register: ServiceChangeRegister)
                             (implicit ac: AuthContext, hc: HeaderCarrier) = {

    statusService.isPreSubmission map { isPreSubmission =>
      (shouldRouteToNext, services, isPreSubmission) match {
        case (true, _, _) =>
          Redirect(routes.SendTheLargestAmountsOfMoneyController.get())
        case (false, _, false) if shouldAnswerCurrencyExchangeQuestions(services, register) =>
          Redirect(routes.CETransactionsInNext12MonthsController.get())
        case (false, s, true) if s contains CurrencyExchange =>
          Redirect(routes.CETransactionsInNext12MonthsController.get())
        case (false, _, _) =>
          Redirect(routes.SummaryController.get())
      }
    }
  }

  private def editRouting(next: Boolean, services: Set[BusinessMatchingMsbService], msb: MoneyServiceBusiness): Result =
    (next: Boolean, services) match {
      case (true, _) if msb.sendTheLargestAmountsOfMoney.isEmpty =>
        Redirect(routes.SendTheLargestAmountsOfMoneyController.get(true))
      case (false, s)
        if (s contains CurrencyExchange) && msb.sendTheLargestAmountsOfMoney.isEmpty =>
        Redirect(routes.CETransactionsInNext12MonthsController.get(true))
      case _ =>
        Redirect(routes.SummaryController.get())
    }

}
