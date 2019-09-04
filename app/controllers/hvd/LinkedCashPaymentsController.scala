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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.hvd.{Hvd, LinkedCashPayments}
import utils.AuthAction
import views.html.hvd.linked_cash_payments

import scala.concurrent.Future

class LinkedCashPaymentsController @Inject() ( val dataCacheConnector: DataCacheConnector,
                                               val authAction: AuthAction, val ds: CommonPlayDependencies) extends AmlsBaseController(ds) {

  def get(edit: Boolean = false) =
    authAction.async {
      implicit request =>
        dataCacheConnector.fetch[Hvd](request.credId, Hvd.key) map {
          response =>
            val form: Form2[LinkedCashPayments] = (for {
              hvd <- response
              linkedCashPayment <- hvd.linkedCashPayment
            } yield Form2[LinkedCashPayments](linkedCashPayment)).getOrElse(EmptyForm)
            Ok(linked_cash_payments(form, edit))
        }
    }

  def post(edit: Boolean = false) =
    authAction.async {
      implicit request => {
        Form2[LinkedCashPayments](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(linked_cash_payments(f, edit)))
          case ValidForm(_, data) =>
            for {
              hvd <- dataCacheConnector.fetch[Hvd](request.credId, Hvd.key)
              _ <- dataCacheConnector.save[Hvd](request.credId, Hvd.key,
                hvd.linkedCashPayment(data)
              )
            } yield edit match {
              case true  => Redirect(routes.SummaryController.get())
              case false => Redirect(routes.ReceiveCashPaymentsController.get())
            }
        }
      }
    }
}
