/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.{Inject, Singleton}
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessMatching, BusinessMatchingMsbService, CurrencyExchange, ForeignExchange}
import models.moneyservicebusiness.{MoneyServiceBusiness, MostTransactions}
import play.api.mvc.MessagesControllerComponents
import services.businessmatching.ServiceFlow
import services.{AutoCompleteService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, ControllerHelper}
import views.html.msb.most_transactions

import scala.concurrent.Future


@Singleton
class MostTransactionsController @Inject()(authAction: AuthAction,
                                           val ds: CommonPlayDependencies,
                                           implicit val cacheConnector: DataCacheConnector,
                                           implicit val statusService: StatusService,
                                           implicit val serviceFlow: ServiceFlow,
                                           val autoCompleteService: AutoCompleteService,
                                           val cc: MessagesControllerComponents,
                                           most_transactions: most_transactions) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        cacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map {
          response =>
            val form = (for {
              msb <- response
              transactions <- msb.mostTransactions
            } yield Form2[MostTransactions](transactions)).getOrElse(EmptyForm)
            Ok(most_transactions(form, edit, autoCompleteService.getCountries))
        }
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[MostTransactions](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(most_transactions(alignFormDataWithValidationErrors(f), edit, autoCompleteService.getCountries)))
          case ValidForm(_, data) =>
            cacheConnector.fetchAll(request.credId) flatMap { maybeCache =>
              val result = for {
                cacheMap <- maybeCache
                msb <- cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
                bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                services <- bm.msbServices
                register <- cacheMap.getEntry[ServiceChangeRegister](ServiceChangeRegister.key) orElse Some(ServiceChangeRegister())
              } yield {
                cacheConnector.save[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key,
                  msb.mostTransactions(Some(data))
                ) map {
                  _ => routing(services.msbServices, register, msb, edit)
                }
              }

              result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
  }

  def alignFormDataWithValidationErrors(form: InvalidForm): InvalidForm =
    ControllerHelper.stripEmptyValuesFromFormWithArray(form, "mostTransactionsCountries", index => index / 2)


  private def shouldAnswerCurrencyExchangeQuestions(msbServices: Set[BusinessMatchingMsbService],
                                                     register: ServiceChangeRegister,
                                                     msb: MoneyServiceBusiness,
                                                     edit: Boolean): Boolean = {

    currencyExchangeAddedPostSubmission(msbServices, register) ||
            (msbServices.contains(CurrencyExchange) && (msb.ceTransactionsInNext12Months.isEmpty || !edit))
  }

  private def shouldAnswerForeignExchangeQuestions(msbServices: Set[BusinessMatchingMsbService],
                                                    register: ServiceChangeRegister,
                                                    msb: MoneyServiceBusiness,
                                                    edit: Boolean): Boolean = {

    foreignExchangeAddedPostSubmission(msbServices, register) ||
            (msbServices.contains(ForeignExchange) && (msb.fxTransactionsInNext12Months.isEmpty || !edit))
  }

  private def routing(msbServices: Set[BusinessMatchingMsbService], register: ServiceChangeRegister, msb: MoneyServiceBusiness, edit: Boolean)
                     (implicit hc: HeaderCarrier) = {

    if (shouldAnswerCurrencyExchangeQuestions(msbServices, register, msb, edit)) {
      Redirect(routes.CETransactionsInNext12MonthsController.get(edit))
    } else if (shouldAnswerForeignExchangeQuestions(msbServices, register, msb, edit)) {
      Redirect(routes.FXTransactionsInNext12MonthsController.get(edit))
    } else {
      Redirect(routes.SummaryController.get())
    }
  }
}
