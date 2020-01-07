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

package controllers.hvd

import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.hvd.{CashPayment, CashPaymentFirstDate, CashPaymentOverTenThousandEuros, Hvd}
import utils.AuthAction
import views.html.hvd.cash_payment_first_date

import scala.concurrent.Future

class CashPaymentFirstDateController @Inject()(val dataCacheConnector: DataCacheConnector,
                                               val authAction: AuthAction) extends DefaultBaseController {

  def get(edit: Boolean = false) =
    authAction.async {
      implicit request =>
        dataCacheConnector.fetch[Hvd](request.credId, Hvd.key) map {
          response =>
            val form: Form2[CashPaymentFirstDate] = (for {
              hvd <- response
              cashPayment <- hvd.cashPayment.flatMap(p => p.firstDate)
            } yield Form2[CashPaymentFirstDate](cashPayment)).getOrElse(EmptyForm)
            Ok(cash_payment_first_date(form, edit))
        }
    }


  def post(edit: Boolean = false) =
    authAction.async {
      implicit request => {
        Form2[CashPaymentFirstDate](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(cash_payment_first_date(f, edit)))
          case ValidForm(_, data) =>
            for {
              hvd <- dataCacheConnector.fetch[Hvd](request.credId, Hvd.key)
              _ <- dataCacheConnector.save[Hvd](request.credId, Hvd.key, hvd.cashPayment(
                hvd.cashPayment match {
                  case Some(cp) => CashPayment.update(cp, data)
                  case None => CashPayment(CashPaymentOverTenThousandEuros(false), None)
                }
              ))
            } yield edit match {
              case true  => Redirect(routes.SummaryController.get())
              case false => Redirect(routes.LinkedCashPaymentsController.get())
            }
        }
      }
    }
}