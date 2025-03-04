/*
 * Copyright 2024 HM Revenue & Customs
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

package models.hvd

import models.renewal.{CashPayments => RReceiveCashPayments, CashPaymentsCustomerNotMet, HowCashPaymentsReceived, PaymentMethods => RPaymentMethods}
import play.api.libs.json.{Writes, _}

case class ReceiveCashPayments(paymentMethods: Option[PaymentMethods])

sealed trait ReceiveCashPayments0 {

  implicit val jsonReads: Reads[ReceiveCashPayments] =
    (__ \ "receivePayments").read[Boolean] flatMap {
      case true  => (__ \ "paymentMethods").read[PaymentMethods] map (x => ReceiveCashPayments(Some(x)))
      case false => Reads(_ => JsSuccess(ReceiveCashPayments(None)))
    }

  val jsonR: Reads[ReceiveCashPayments] =
    implicitly[Reads[ReceiveCashPayments]]

  val jsonW = Writes[ReceiveCashPayments] { x =>
    x.paymentMethods match {
      case Some(paymentMtds) =>
        Json.obj(
          "receivePayments" -> true,
          "paymentMethods"  -> Json.obj(
            "courier" -> paymentMtds.courier,
            "direct"  -> paymentMtds.direct,
            "other"   -> paymentMtds.other.isDefined,
            "details" -> paymentMtds.other
          )
        )
      case None              => Json.obj("receivePayments" -> false, "paymentMethods" -> Json.obj())
    }
  }
}

object ReceiveCashPayments {

  private object Cache extends ReceiveCashPayments0

  implicit val jsonR: Reads[ReceiveCashPayments]  = Cache.jsonReads
  implicit val jsonW: Writes[ReceiveCashPayments] = Cache.jsonW

  def convert(model: Hvd): RReceiveCashPayments =
    if (model.receiveCashPayments.contains(false)) {
      RReceiveCashPayments(CashPaymentsCustomerNotMet(false), None)
    } else {
      model.cashPaymentMethods.fold(RReceiveCashPayments(CashPaymentsCustomerNotMet(false), None)) { methods =>
        RReceiveCashPayments(
          CashPaymentsCustomerNotMet(true),
          Some(
            HowCashPaymentsReceived(
              RPaymentMethods(
                methods.courier,
                methods.direct,
                methods.other
              )
            )
          )
        )
      }
    }
}
