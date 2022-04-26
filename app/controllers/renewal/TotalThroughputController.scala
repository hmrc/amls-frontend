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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching._
import models.renewal.{Renewal, TotalThroughput}
import play.api.mvc.{MessagesControllerComponents, Result}
import services.RenewalService
import utils.AuthAction

import views.html.renewal.total_throughput

import scala.concurrent.Future

class TotalThroughputController @Inject()(val authAction: AuthAction,
                                          val ds: CommonPlayDependencies,
                                          renewals: RenewalService,
                                          dataCacheConnector: DataCacheConnector,
                                          val cc: MessagesControllerComponents,
                                          total_throughput: total_throughput) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        val maybeResult = for {
          renewal <- OptionT(renewals.getRenewal(request.credId))
          throughput <- OptionT.fromOption[Future](renewal.totalThroughput)
        } yield {
          Ok(total_throughput(Form2[TotalThroughput](throughput), edit))
        }

        maybeResult getOrElse Ok(total_throughput(EmptyForm, edit))
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[TotalThroughput](request.body) match {
          case form: InvalidForm => Future.successful(BadRequest(total_throughput(form, edit)))
          case ValidForm(_, model) =>
            dataCacheConnector.fetchAll(request.credId) flatMap {
              optMap =>
                val result = for {
                  cacheMap <- optMap
                  renewal <- cacheMap.getEntry[Renewal](Renewal.key)
                  bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                  services <- bm.msbServices
                  activities <- bm.activities
                } yield {
                  renewals.updateRenewal(request.credId, renewal.totalThroughput(model)) map { _ =>
                    if (!edit) {
                      standardRouting(services.msbServices, activities.businessActivities)
                    } else {
                      Redirect(routes.SummaryController.get)
                    }
                  }
                }
                result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
  }

  private def standardRouting(services: Set[BusinessMatchingMsbService], businessActivities: Set[BusinessActivity]): Result = {
      if(services.contains(TransmittingMoney)) { Redirect(routes.TransactionsInLast12MonthsController.get()) }
      else if(services.contains(CurrencyExchange)) { Redirect(routes.CETransactionsInLast12MonthsController.get()) }
      else if(services.contains(ForeignExchange)) { Redirect(routes.FXTransactionsInLast12MonthsController.get()) }
      else if(businessActivities.contains(HighValueDealing) || businessActivities.contains(AccountancyServices)) {
        Redirect(routes.CustomersOutsideIsUKController.get())
      } else {
        Redirect(routes.SummaryController.get)
      }
  }
}
