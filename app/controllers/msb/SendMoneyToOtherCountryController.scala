/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessMatching, BusinessMatchingMsbService, CurrencyExchange, ForeignExchange}
import models.moneyservicebusiness.{MoneyServiceBusiness, SendMoneyToOtherCountry}
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import utils.AuthAction
import views.html.msb.send_money_to_other_country
import scala.concurrent.Future

class SendMoneyToOtherCountryController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                  authAction: AuthAction,
                                                  val ds: CommonPlayDependencies,
                                                  val statusService: StatusService,
                                                  val cc: MessagesControllerComponents,
                                                  send_money_to_other_country: send_money_to_other_country) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map { response =>
          val form: Form2[SendMoneyToOtherCountry] = (for {
            msb <- response
            money <- msb.sendMoneyToOtherCountry
          } yield Form2[SendMoneyToOtherCountry](money)).getOrElse(EmptyForm)

          Ok(send_money_to_other_country(form, edit))
        }
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request => {
        Form2[SendMoneyToOtherCountry](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(send_money_to_other_country(f, edit)))
          case ValidForm(_, data) =>
            dataCacheConnector.fetchAll(request.credId) flatMap { maybeCache =>
              val result = for {
                cache <- maybeCache
                msb <- cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
                bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
                services <- bm.msbServices
                register <- cache.getEntry[ServiceChangeRegister](ServiceChangeRegister.key) orElse Some(ServiceChangeRegister())
              } yield {
                data.money match {
                  case true => dataCacheConnector.save(request.credId, MoneyServiceBusiness.key, msb.sendMoneyToOtherCountry(data)) map {
                    _ => Redirect(routes.SendTheLargestAmountsOfMoneyController.get(edit))
                  }
                  case _ => val newModel = msb
                    .sendMoneyToOtherCountry(data)
                    .sendTheLargestAmountsOfMoney(None)
                    .mostTransactions(None)

                    dataCacheConnector.save(request.credId, MoneyServiceBusiness.key, newModel) map {
                    _ => if(edit) {
                      Redirect(routes.SummaryController.get())
                    } else {
                      routing(services.msbServices,register, newModel, edit)
                    }
                  }
                }
              }
              result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
      }
  }

  private def shouldAnswerCurrencyExchangeQuestion(services: Set[BusinessMatchingMsbService],
                                                    register: ServiceChangeRegister,
                                                    msb: MoneyServiceBusiness): Boolean = {

    currencyExchangeAddedPostSubmission(services, register) ||
            (services.contains(CurrencyExchange) && msb.sendTheLargestAmountsOfMoney.isEmpty)
  }

  private def shouldAnswerForeignExchangeQuestion(services: Set[BusinessMatchingMsbService],
                                                   register: ServiceChangeRegister,
                                                   msb: MoneyServiceBusiness): Boolean = {

    foreignExchangeAddedPostSubmission(services, register) ||
            (services.contains(ForeignExchange) && msb.sendTheLargestAmountsOfMoney.isEmpty)
  }

  private def routing(services: Set[BusinessMatchingMsbService], register: ServiceChangeRegister, msb: MoneyServiceBusiness, edit: Boolean) = {

    val (ceQuestion, fxQuestion) = (shouldAnswerCurrencyExchangeQuestion(services, register, msb), shouldAnswerForeignExchangeQuestion(services, register, msb))

    (ceQuestion, fxQuestion) match {
      case (true, _ ) => Redirect(routes.CETransactionsInNext12MonthsController.get(edit))
      case (_, true) => Redirect(routes.FXTransactionsInNext12MonthsController.get(edit))
      case _ =>Redirect(routes.SummaryController.get())
    }
  }
}
