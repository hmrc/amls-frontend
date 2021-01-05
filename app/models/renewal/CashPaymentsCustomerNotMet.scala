/*
 * Copyright 2021 HM Revenue & Customs
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

case class CashPaymentsCustomerNotMet(receiveCashPayments: Boolean)

object CashPaymentsCustomerNotMet {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, CashPaymentsCustomerNotMet] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "receiveCashPayments").read[Boolean].withMessage("error.required.renewal.hvd.receive.cash.payments") flatMap {
      CashPaymentsCustomerNotMet.apply
    }
  }

  implicit def formWrites: Write[CashPaymentsCustomerNotMet, UrlFormEncoded] = Write {
    case CashPaymentsCustomerNotMet(accepted) => Map("receiveCashPayments" -> Seq(accepted.toString))
  }
}
