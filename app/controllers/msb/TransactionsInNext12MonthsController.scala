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

package controllers.msb

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.moneyservicebusiness.{MoneyServiceBusiness, TransactionsInNext12Months}
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction
import views.html.msb.transactions_in_next_12_months

import scala.concurrent.Future

class TransactionsInNext12MonthsController @Inject()(authAction: AuthAction, val ds: CommonPlayDependencies,
                                                     implicit val dataCacheConnector: DataCacheConnector,
                                                     implicit val statusService: StatusService,
                                                     implicit val serviceFlow: ServiceFlow
                                                    ) extends AmlsBaseController(ds) {


  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map {
          response =>
            val form: Form2[TransactionsInNext12Months] = (for {
              msb <- response
              transactions <- msb.transactionsInNext12Months
            } yield Form2[TransactionsInNext12Months](transactions)).getOrElse(EmptyForm)
            Ok(transactions_in_next_12_months(form, edit))
        }
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request => {
        Form2[TransactionsInNext12Months](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(transactions_in_next_12_months(f, edit)))
          case ValidForm(_, data) =>
            for {
              msb <- dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
              _ <- dataCacheConnector.save[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key,
                msb.transactionsInNext12Months(data)
              )
            } yield edit match {
              case true if msb.sendMoneyToOtherCountry.isDefined => Redirect(routes.SummaryController.get())
              case _ => Redirect(routes.SendMoneyToOtherCountryController.get(edit))
            }
        }
      }
  }
}
