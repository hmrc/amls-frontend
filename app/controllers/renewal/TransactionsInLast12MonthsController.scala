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

package controllers.renewal

import javax.inject.Inject

import cats.data.OptionT
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.renewal.{CustomersOutsideUK, Renewal, TransactionsInLast12Months}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.transactions_in_last_12_months
import cats.implicits._
import connectors.DataCacheConnector
import models.businessmatching._
import play.api.mvc.Result
import utils.ControllerHelper

import scala.concurrent.Future

class TransactionsInLast12MonthsController @Inject()(
                                                      val authConnector: AuthConnector,
                                                      val dataCacheConnector: DataCacheConnector,
                                                      renewalService: RenewalService) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          renewal <- OptionT(renewalService.getRenewal)
          transfers <- OptionT.fromOption[Future](renewal.transactionsInLast12Months)
        } yield {
          Ok(transactions_in_last_12_months(Form2[TransactionsInLast12Months](transfers), edit))
        }) getOrElse Ok(transactions_in_last_12_months(EmptyForm, edit))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[TransactionsInLast12Months](request.body) match {
          case f: InvalidForm => Future.successful(BadRequest(transactions_in_last_12_months(f, edit)))
          case ValidForm(_, model) =>
            dataCacheConnector.fetchAll flatMap {
              optMap =>
                (for {
                  cacheMap <- optMap
                  renewal <- cacheMap.getEntry[Renewal](Renewal.key)
                  bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                  services <- bm.msbServices
                  activities <- bm.activities
                } yield {
                  renewalService.updateRenewal(renewal.transactionsInLast12Months(model)) map { _ =>
                    redirectTo(
                      ControllerHelper.hasCustomersOutsideUK(renewal.customersOutsideUK),
                      services.msbServices,
                      activities.businessActivities, edit
                    )
                  }
                }) getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
  }

  private def redirectTo(hasCustomersOutsideUK: Boolean, services: Set[MsbService], activities: Set[BusinessActivity], edit: Boolean) =
    if (edit) {
      Redirect(routes.SummaryController.get())
    } else if (hasCustomersOutsideUK) {
      Redirect(routes.SendTheLargestAmountsOfMoneyController.get(edit))
    } else if ((services contains CurrencyExchange) && !edit) {
      Redirect(routes.CETransactionsInLast12MonthsController.get(edit))
    } else if ((activities contains HighValueDealing) && !edit) {
      Redirect(routes.PercentageOfCashPaymentOver15000Controller.get(edit))
    } else {
      Redirect(routes.SummaryController.get())
    }

}
