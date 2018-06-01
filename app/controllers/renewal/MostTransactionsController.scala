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

package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching._
import models.renewal.{MostTransactions, Renewal}
import play.api.mvc.Result
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

@Singleton
class MostTransactionsController @Inject()(val authConnector: AuthConnector,
                                           val cache: DataCacheConnector,
                                           val renewalService: RenewalService) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        cache.fetch[Renewal](Renewal.key) map {
          response =>
            val form = (for {
              msb <- response
              transactions <- msb.mostTransactions
            } yield Form2[MostTransactions](transactions)).getOrElse(EmptyForm)
            Ok(views.html.renewal.most_transactions(form, edit))
        }
  }


  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[MostTransactions](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.renewal.most_transactions(f, edit)))
          case ValidForm(_, data) =>
            cache.fetchAll flatMap {
              optMap =>
                val result = for {
                  cacheMap <- optMap
                  renewal <- cacheMap.getEntry[Renewal](Renewal.key)
                  bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                  ba <- bm.activities
                  services <- bm.msbServices
                } yield renewalService.updateRenewal(renewal.mostTransactions(data)) map { _ =>
                  redirectTo(services.msbServices, ba.businessActivities, edit)
                }
                result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
  }

  private def redirectTo(services: Set[BusinessMatchingMsbService], businessActivities: Set[BusinessActivity], edit: Boolean): Result = {
    (edit, services, businessActivities) match {
      case (true, _, _) => Redirect(routes.SummaryController.get())
      case (_, x, _) if x.contains(CurrencyExchange) => Redirect(routes.CETransactionsInLast12MonthsController.get(edit))
      case (_, _, x) if x.contains(HighValueDealing) && x.contains(AccountancyServices) => Redirect(routes.PercentageOfCashPaymentOver15000Controller.get(edit))
      case (_, _, x) if x.contains(HighValueDealing) => Redirect(routes.CustomersOutsideUKController.get(edit))
      case _ => Redirect(routes.SummaryController.get())
    }
  }

}
