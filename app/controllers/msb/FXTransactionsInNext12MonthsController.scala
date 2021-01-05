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
import models.moneyservicebusiness.{FXTransactionsInNext12Months, MoneyServiceBusiness}
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction
import views.html.msb.fx_transaction_in_next_12_months

import scala.concurrent.Future

class FXTransactionsInNext12MonthsController @Inject()(authAction: AuthAction,
                                                       val ds: CommonPlayDependencies,
                                                       implicit val dataCacheConnector: DataCacheConnector,
                                                       implicit val statusService: StatusService,
                                                       implicit val serviceFlow: ServiceFlow,
                                                       val cc: MessagesControllerComponents,
                                                       fx_transaction_in_next_12_months: fx_transaction_in_next_12_months) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map {
          response =>
            val form: Form2[FXTransactionsInNext12Months] = (for {
              msb <- response
              transactions <- msb.fxTransactionsInNext12Months
            } yield Form2[FXTransactionsInNext12Months](transactions)).getOrElse(EmptyForm)
            Ok(fx_transaction_in_next_12_months(form, edit))
        }
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request => {
        Form2[FXTransactionsInNext12Months](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(fx_transaction_in_next_12_months(f, edit)))
          case ValidForm(_, data) =>
            for {
              msb <- dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
              _ <- dataCacheConnector.save[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key,
                msb.fxTransactionsInNext12Months(data)
              )
            } yield Redirect(routes.SummaryController.get())
        }
      }
  }
}
