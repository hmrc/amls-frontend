/*
 * Copyright 2023 HM Revenue & Customs
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

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}

case class CashPaymentFirstDate(paymentDate: LocalDate)

object CashPaymentFirstDate {

  implicit val formRule: Rule[UrlFormEncoded, CashPaymentFirstDate] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
        (__ \ "paymentDate").read(newAllowedPastAndFutureDateRule("error.date.hvd",
          "error.date.hvd.past",
          "error.date.hvd.future",
          "error.date.hvd.real")) map CashPaymentFirstDate.apply
  }

  implicit def formWrites: Write[CashPaymentFirstDate, UrlFormEncoded] = Write {
    case CashPaymentFirstDate(date) =>
      Map(
        "paymentDate.day" -> Seq(date.get(DateTimeFieldType.dayOfMonth()).toString),
        "paymentDate.month" -> Seq(date.get(DateTimeFieldType.monthOfYear()).toString),
        "paymentDate.year" -> Seq(date.get(DateTimeFieldType.year()).toString))

  }
}
