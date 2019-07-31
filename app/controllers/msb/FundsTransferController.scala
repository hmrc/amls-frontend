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
import controllers.DefaultBaseController
import forms._
import javax.inject.Inject
import models.moneyservicebusiness.{FundsTransfer, MoneyServiceBusiness}
import utils.AuthAction
import views.html.msb._

import scala.concurrent.Future

class FundsTransferController @Inject() ( val dataCacheConnector: DataCacheConnector,
                                          authAction: AuthAction
                                        ) extends DefaultBaseController {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map {
        response =>
          val form: Form2[FundsTransfer] = (for {
            moneyServiceBusiness <- response
            fundsTransfer <- moneyServiceBusiness.fundsTransfer
          } yield Form2[FundsTransfer](fundsTransfer)).getOrElse(EmptyForm)
          Ok(funds_transfer(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>
      Form2[FundsTransfer](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(funds_transfer(f, edit)))
        case ValidForm(_, data) =>
          for {
            moneyServiceBusiness <- dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key,
              moneyServiceBusiness.fundsTransfer(data))
          } yield edit match {
            case true if moneyServiceBusiness.transactionsInNext12Months.isDefined =>
              Redirect(routes.SummaryController.get())
            case _ => Redirect(routes.TransactionsInNext12MonthsController.get(edit))
          }
      }
  }
}
