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

package models.renewal

import play.api.libs.json._

case class CashPayments(
  cashPaymentsCustomerNotMet: CashPaymentsCustomerNotMet,
  howCashPaymentsReceived: Option[HowCashPaymentsReceived]
)

object CashPayments {

  implicit val jsonReads: Reads[CashPayments] =
    (__ \ "receivePayments").read[Boolean] flatMap {
      case true  =>
        (__ \ "paymentMethods").readNullable[PaymentMethods] map {
          case Some(x) => CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(x)))
          case _       => CashPayments(CashPaymentsCustomerNotMet(true), None)
        }
      case false => Reads(_ => JsSuccess(CashPayments(CashPaymentsCustomerNotMet(false), None)))
    }

  implicit val jsonWrites: Writes[CashPayments] =
    Writes[CashPayments] {
      case CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(cp))) =>
        val maybeOther = if (cp.other.isDefined) {
          Json.obj("details" -> cp.other)
        } else {
          Json.obj()
        }

        val payMet = Json.obj(
          "courier" -> cp.courier,
          "direct"  -> cp.direct,
          "other"   -> cp.other.isDefined
        ) ++ maybeOther

        Json.obj("receivePayments" -> true, "paymentMethods" -> payMet)
      case CashPayments(CashPaymentsCustomerNotMet(receivePayments), _)                      =>
        Json.obj("receivePayments" -> receivePayments)
    }

  implicit def convert(model: CashPayments): Option[models.hvd.PaymentMethods] = model.howCashPaymentsReceived map {
    pm =>
      models.hvd.PaymentMethods(pm.paymentMethods.courier, pm.paymentMethods.direct, pm.paymentMethods.other)
  }
}
