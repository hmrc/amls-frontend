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

package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import javax.inject.Inject
import models.moneyservicebusiness.{FundsTransfer, MoneyServiceBusiness}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.msb._

import scala.concurrent.Future

class FundsTransferController @Inject() ( val dataCacheConnector: DataCacheConnector,
                                          val authConnector: AuthConnector
                                        ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>
          val form: Form2[FundsTransfer] = (for {
            moneyServiceBusiness <- response
            fundsTransfer <- moneyServiceBusiness.fundsTransfer
          } yield Form2[FundsTransfer](fundsTransfer)).getOrElse(EmptyForm)
          Ok(funds_transfer(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[FundsTransfer](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(funds_transfer(f, edit)))
        case ValidForm(_, data) =>
          for {
            moneyServiceBusiness <- dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              moneyServiceBusiness.fundsTransfer(data))
          } yield edit match {
            case true if moneyServiceBusiness.transactionsInNext12Months.isDefined =>
              Redirect(routes.SummaryController.get())
            case _ => Redirect(routes.TransactionsInNext12MonthsController.get(edit))
          }
      }
  }
}
