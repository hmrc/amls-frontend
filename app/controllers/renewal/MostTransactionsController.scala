/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.renewal

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching._
import models.renewal.{MostTransactions, Renewal}
import play.api.mvc.{MessagesControllerComponents, Result}
import services.{AutoCompleteService, RenewalService}
import utils.{AuthAction, ControllerHelper}
import views.html.renewal.most_transactions

import scala.concurrent.Future


@Singleton
class MostTransactionsController @Inject()(val authAction: AuthAction,
                                           val ds: CommonPlayDependencies,
                                           val cache: DataCacheConnector,
                                           val renewalService: RenewalService,
                                           val autoCompleteService: AutoCompleteService,
                                           val cc: MessagesControllerComponents,
                                           most_transactions: most_transactions) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        cache.fetch[Renewal](request.credId, Renewal.key) map {
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
            cache.fetchAll(request.credId).flatMap {
              optMap =>
                val result = for {
                  cacheMap <- optMap
                  renewal <- cacheMap.getEntry[Renewal](Renewal.key)
                  bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                  ba <- bm.activities
                  services <- bm.msbServices
                } yield renewalService.updateRenewal(request.credId, renewal.mostTransactions(data)) map { _ =>
                  if (!edit) {
                    redirectTo(services.msbServices, ba.businessActivities)
                  } else {
                    Redirect(routes.SummaryController.get)
                  }
                }
                result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
  }

  def alignFormDataWithValidationErrors(form: InvalidForm): InvalidForm =
    ControllerHelper.stripEmptyValuesFromFormWithArray(form, "mostTransactionsCountries", index => index / 2)


  private def redirectTo(services: Set[BusinessMatchingMsbService], businessActivities: Set[BusinessActivity]): Result = {
      (services, businessActivities) match {
        case (x, _) if x.contains(CurrencyExchange) => Redirect(routes.CETransactionsInLast12MonthsController.get())
        case (x, _) if x.contains(ForeignExchange) => Redirect(routes.FXTransactionsInLast12MonthsController.get())
        case (_, x) if x.contains(HighValueDealing) && x.contains(AccountancyServices) => Redirect(routes.PercentageOfCashPaymentOver15000Controller.get())
        case (_, x) if x.contains(HighValueDealing) || x.contains(AccountancyServices) => Redirect(routes.CustomersOutsideIsUKController.get())
        case _ => Redirect(routes.SummaryController.get)
      }
  }
}
