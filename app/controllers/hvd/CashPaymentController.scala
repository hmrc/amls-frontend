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

package controllers.hvd

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.hvd.{CashPayment, Hvd}
import views.html.hvd.cash_payment

import scala.concurrent.Future

trait CashPaymentController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetch[Hvd](Hvd.key) map {
          response =>
            val form: Form2[CashPayment] = (for {
              hvd <- response
              cashPayment <- hvd.cashPayment
            } yield Form2[CashPayment](cashPayment)).getOrElse(EmptyForm)
            Ok(cash_payment(form, edit))
        }
    }


  def post(edit: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request => {
        Form2[CashPayment](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(cash_payment(f, edit)))
          case ValidForm(_, data) =>
            for {
              hvd <- dataCacheConnector.fetch[Hvd](Hvd.key)
              _ <- dataCacheConnector.save[Hvd](Hvd.key,
                hvd.cashPayment(data)
              )
            } yield edit match {
              case true => Redirect(routes.SummaryController.get())
              case false => Redirect(routes.LinkedCashPaymentsController.get())
            }
        }
      }
    }
}


object CashPaymentController extends CashPaymentController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
