/*
 * Copyright 2019 HM Revenue & Customs
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
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.renewal.{CETransactionsInLast12Months, Renewal}
import services.RenewalService
import utils.AuthAction
import views.html.renewal.ce_transactions_in_last_12_months

import scala.concurrent.Future

@Singleton
class CETransactionsInLast12MonthsController @Inject()(
                                                           val dataCacheConnector: DataCacheConnector,
                                                           val authAction: AuthAction,
                                                           val renewalService: RenewalService
                                                         ) extends DefaultBaseController {
  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[Renewal](request.credId, Renewal.key) map {
        response =>
          val form: Form2[CETransactionsInLast12Months] = (for {
            renewal <- response
            transactions <- renewal.ceTransactionsInLast12Months
          } yield Form2[CETransactionsInLast12Months](transactions)).getOrElse(EmptyForm)
          Ok(ce_transactions_in_last_12_months(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[CETransactionsInLast12Months](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(ce_transactions_in_last_12_months(f, edit)))
        case ValidForm(_, data) =>
          for {
            renewal <- dataCacheConnector.fetch[Renewal](request.credId, Renewal.key)
            _ <- renewalService.updateRenewal(request.credId, renewal.ceTransactionsInLast12Months(data))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.WhichCurrenciesController.get(edit))
          }
      }
    }
  }
}
