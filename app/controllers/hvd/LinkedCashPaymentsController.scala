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

package controllers.hvd

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{InvalidForm, ValidForm, EmptyForm, Form2}
import models.hvd.{LinkedCashPayments, Hvd}
import views.html.hvd.linked_cash_payments

import scala.concurrent.Future

trait LinkedCashPaymentsController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetch[Hvd](Hvd.key) map {
          response =>
            val form: Form2[LinkedCashPayments] = (for {
              hvd <- response
              linkedCashPayment <- hvd.linkedCashPayment
            } yield Form2[LinkedCashPayments](linkedCashPayment)).getOrElse(EmptyForm)
            Ok(linked_cash_payments(form, edit))
        }
    }

  def post(edit: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request => {
        Form2[LinkedCashPayments](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(linked_cash_payments(f, edit)))
          case ValidForm(_, data) =>
            for {
              hvd <- dataCacheConnector.fetch[Hvd](Hvd.key)
              _ <- dataCacheConnector.save[Hvd](Hvd.key,
                hvd.linkedCashPayment(data)
              )
            } yield edit match {
              case true => Redirect(routes.SummaryController.get())
              case false => Redirect(routes.ReceiveCashPaymentsController.get())
            }
        }
      }
    }
}

object LinkedCashPaymentsController extends LinkedCashPaymentsController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
