/*
 * Copyright 2022 HM Revenue & Customs
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

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}

case class HowCashPaymentsReceived(paymentMethods: PaymentMethods)

object HowCashPaymentsReceived {

  implicit val formRule: Rule[UrlFormEncoded, HowCashPaymentsReceived] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "cashPaymentMethods").read[PaymentMethods] map HowCashPaymentsReceived.apply
  }

  implicit def formWrites: Write[HowCashPaymentsReceived, UrlFormEncoded] = Write {
    case HowCashPaymentsReceived(paymentMethods) => Map(
      "cashPaymentMethods.courier" -> Seq(paymentMethods.courier.toString),
      "cashPaymentMethods.direct" -> Seq(paymentMethods.direct.toString),
      "cashPaymentMethods.other" -> Seq(paymentMethods.other.isDefined.toString),
      "cashPaymentMethods.details" -> Seq(paymentMethods.other match {
        case Some(other) => other
        case _ => ""
      })
    )
  }
}