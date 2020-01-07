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

package controllers.renewal

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching._
import models.renewal.{Renewal, TransactionsInLast12Months}
import services.RenewalService
import utils.AuthAction
import views.html.renewal.transactions_in_last_12_months

import scala.concurrent.Future

class TransactionsInLast12MonthsController @Inject()(
                                                      val authAction: AuthAction,
                                                      val dataCacheConnector: DataCacheConnector,
                                                      renewalService: RenewalService) extends DefaultBaseController {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        (for {
          renewal <- OptionT(renewalService.getRenewal(request.credId))
          transfers <- OptionT.fromOption[Future](renewal.transactionsInLast12Months)
        } yield {
          Ok(transactions_in_last_12_months(Form2[TransactionsInLast12Months](transfers), edit))
        }) getOrElse Ok(transactions_in_last_12_months(EmptyForm, edit))
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[TransactionsInLast12Months](request.body) match {
          case f: InvalidForm => Future.successful(BadRequest(transactions_in_last_12_months(f, edit)))
          case ValidForm(_, model) =>
            dataCacheConnector.fetchAll(request.credId) flatMap {
              optMap =>
                (for {
                  cacheMap <- optMap
                  renewal <- cacheMap.getEntry[Renewal](Renewal.key)
                  bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                  services <- bm.msbServices
                } yield {
                  renewalService.updateRenewal(request.credId, renewal.transactionsInLast12Months(model)) map { _ =>
                    redirectTo(services.msbServices, edit)
                  }
                }) getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
  }

   private def redirectTo(services: Set[BusinessMatchingMsbService], edit: Boolean) =
     if (edit) {
       Redirect(routes.SummaryController.get())
     } else if (services contains TransmittingMoney) {
       Redirect(routes.SendMoneyToOtherCountryController.get())
     } else {
       Redirect(routes.SummaryController.get())
     }

}
