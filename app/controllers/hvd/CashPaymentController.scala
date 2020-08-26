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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.hvd._
import play.api.mvc.{Call, MessagesControllerComponents}
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.hvd.cash_payment

import scala.concurrent.Future

class CashPaymentController @Inject() (val dataCacheConnector: DataCacheConnector,
                                       val authAction: AuthAction,
                                       val ds: CommonPlayDependencies,
                                       val cc: MessagesControllerComponents,
                                       cash_payment: cash_payment) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) =
    authAction.async {
      implicit request =>
        dataCacheConnector.fetch[Hvd](request.credId, Hvd.key) map {
          response =>
            val form: Form2[CashPaymentOverTenThousandEuros] = (for {
              hvd <- response
              cashPayment <- hvd.cashPayment.map(p => p.acceptedPayment)
            } yield Form2[CashPaymentOverTenThousandEuros](cashPayment)).getOrElse(EmptyForm)
            Ok(cash_payment(form, edit))
        }
    }


  def post(edit: Boolean = false) =
    authAction.async {
      implicit request => {
        Form2[CashPaymentOverTenThousandEuros](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(cash_payment(f, edit)))
          case ValidForm(_, data) =>
            for {
              hvd <- dataCacheConnector.fetch[Hvd](request.credId, Hvd.key)
              _ <- dataCacheConnector.save[Hvd](request.credId, Hvd.key, hvd.cashPayment(
                  hvd.cashPayment match {
                    case Some(cp) => CashPayment.update(cp, data)
                    case None     =>  CashPayment(data, None)
                  }
              ))
            } yield Redirect(getNextPage(edit, data))
          }
        }
      }

  private def getNextPage(edit:Boolean, data: CashPaymentOverTenThousandEuros): Call = {
    (edit, data) match {
      case (true, CashPaymentOverTenThousandEuros(false))  => routes.SummaryController.get()
      case (false, CashPaymentOverTenThousandEuros(false)) => routes.LinkedCashPaymentsController.get()
      case (_, CashPaymentOverTenThousandEuros(true))      => routes.CashPaymentFirstDateController.get(edit)
    }
  }
}