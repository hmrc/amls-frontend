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

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.renewal.{FXTransactionsInLast12Months, Renewal}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.fx_transaction_in_last_12_months

import scala.concurrent.Future

class FXTransactionsInLast12MonthsController  @Inject()(
                                                         val dataCacheConnector: DataCacheConnector,
                                                         val authConnector: AuthConnector,
                                                         val renewalService: RenewalService
                                                       ) extends BaseController {

    def get(edit:Boolean = false) = Authorised.async {
        implicit authContext => implicit request =>
                dataCacheConnector.fetch[Renewal](Renewal.key) map {
                    response =>
                        val form: Form2[FXTransactionsInLast12Months] = (for {
                            renewal <- response
                            transactions <- renewal.fxTransactionsInLast12Months
                        } yield Form2[FXTransactionsInLast12Months](transactions)).getOrElse(EmptyForm)
                        Ok(fx_transaction_in_last_12_months(form, edit))
                }
            }

    def post(edit: Boolean = false) = Authorised.async {
        implicit authContext => implicit request => {
            Form2[FXTransactionsInLast12Months](request.body) match {
                case f: InvalidForm =>
                    Future.successful(BadRequest(fx_transaction_in_last_12_months(f, edit)))
                case ValidForm(_, data) =>
                    for {
                        renewal <- dataCacheConnector.fetch[Renewal](Renewal.key)
                        _ <- renewalService.updateRenewal(renewal.fxTransactionsInLast12Months(data))
                    } yield Redirect(routes.SummaryController.get())
            }
        }
    }
}
