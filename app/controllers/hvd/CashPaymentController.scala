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

package controllers.hvd

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.hvd._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.hvd.cash_payment

import scala.concurrent.Future

class CashPaymentController @Inject() (val dataCacheConnector: DataCacheConnector,
                                       val authConnector: AuthConnector
                                        ) extends BaseController {

  def get(edit: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetch[Hvd](Hvd.key) map {
          response =>
            val form: Form2[CashPaymentOverTenThousandEuros] = (for {
              hvd <- response
              cashPayment <- hvd.cashPayment.map(p => p.acceptedPayment)
            } yield Form2[CashPaymentOverTenThousandEuros](cashPayment)).getOrElse(EmptyForm)
            Ok(cash_payment(form, edit))
        }
    }


  def post(edit: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request => {
        Form2[CashPaymentOverTenThousandEuros](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(cash_payment(f, edit)))
          case ValidForm(_, data) =>
            for {
              hvd <- dataCacheConnector.fetch[Hvd](Hvd.key)
              _ <- dataCacheConnector.save[Hvd](Hvd.key, hvd.cashPayment(
                  hvd.cashPayment match {
                    case Some(cp) => CashPayment.update(cp, data)
                    case None =>  CashPayment(data, None)
                  }
              ))
            } yield (edit, data) match {
              case (true, CashPaymentOverTenThousandEuros(false)) => Redirect(routes.SummaryController.get())
              case (_, CashPaymentOverTenThousandEuros(true)) => Redirect(routes.CashPaymentFirstDateController.get(edit))
              case (false, CashPaymentOverTenThousandEuros(false)) => Redirect(routes.LinkedCashPaymentsController.get())
          }
        }
      }
    }
}