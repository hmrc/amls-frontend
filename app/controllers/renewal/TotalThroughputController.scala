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

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching._
import models.renewal.{Renewal, TotalThroughput}
import play.api.mvc.Result
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.total_throughput

import scala.concurrent.Future

class TotalThroughputController @Inject()(val authConnector: AuthConnector,
                                          renewals: RenewalService,
                                          dataCacheConnector: DataCacheConnector) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        val maybeResult = for {
          renewal <- OptionT(renewals.getRenewal)
          throughput <- OptionT.fromOption[Future](renewal.totalThroughput)
        } yield {
          Ok(total_throughput(Form2[TotalThroughput](throughput), edit))
        }

        maybeResult getOrElse Ok(total_throughput(EmptyForm, edit))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[TotalThroughput](request.body) match {
          case form: InvalidForm => Future.successful(BadRequest(total_throughput(form, edit)))
          case ValidForm(_, model) =>
            dataCacheConnector.fetchAll flatMap {
              optMap =>
                val result = for {
                  cacheMap <- optMap
                  renewal <- cacheMap.getEntry[Renewal](Renewal.key)
                  bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                  services <- bm.msbServices
                  activities <- bm.activities
                } yield {
                  renewals.updateRenewal(renewal.totalThroughput(model)) map { _ =>
                    standardRouting(services.msbServices, activities.businessActivities, edit)
                  }
                }
                result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
  }

  private def standardRouting(services: Set[BusinessMatchingMsbService], businessActivities: Set[BusinessActivity], edit: Boolean): Result = {
    (services, businessActivities, edit) match {
      case (x, _, false) if x.contains(TransmittingMoney) => Redirect(routes.TransactionsInLast12MonthsController.get())
      case (x, _, false) if x.contains(CurrencyExchange) => Redirect(routes.CETransactionsInLast12MonthsController.get())
      case (x, _, false) if x.contains(ForeignExchange) => Redirect(routes.FXTransactionsInLast12MonthsController.get())
      case (_, x, false) if x.contains(HighValueDealing) && x.contains(AccountancyServices) => Redirect(routes.PercentageOfCashPaymentOver15000Controller.get())
      case (_, x, false) if x.contains(HighValueDealing) => Redirect(routes.CustomersOutsideUKController.get())
      case _ => Redirect(routes.SummaryController.get())
    }
  }
}
