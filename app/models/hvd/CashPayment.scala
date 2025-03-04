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

import play.api.libs.json._

import java.time.LocalDate

case class CashPayment(acceptedPayment: CashPaymentOverTenThousandEuros, firstDate: Option[CashPaymentFirstDate]) {

  def isCashPaymentsComplete: Boolean =
    this match {
      case CashPayment(CashPaymentOverTenThousandEuros(true), Some(_)) => true
      case CashPayment(CashPaymentOverTenThousandEuros(false), None)   => true
      case _                                                           => false
    }
}

object CashPayment {

  implicit val jsonReads: Reads[CashPayment] = {
    import play.api.libs.functional.syntax._
    ((__ \ "acceptedAnyPayment").read[Boolean] map CashPaymentOverTenThousandEuros.apply and
      (__ \ "paymentDate").readNullable[LocalDate].map {
        case Some(date) => Some(CashPaymentFirstDate.apply(date))
        case None       => None
      })(CashPayment.apply _)
  }

  implicit val jsonWrites: Writes[CashPayment] =
    Writes[CashPayment] {
      case CashPayment(CashPaymentOverTenThousandEuros(acceptedAnyPayment), None)                                  =>
        Json.obj(
          "acceptedAnyPayment" -> acceptedAnyPayment
        )
      case CashPayment(CashPaymentOverTenThousandEuros(acceptedAnyPayment), Some(CashPaymentFirstDate(firstDate))) =>
        Json.obj(
          "acceptedAnyPayment" -> acceptedAnyPayment,
          "paymentDate"        -> firstDate.toString
        )
    }

  def update(cashPayment: CashPayment, acceptedAnyPayment: CashPaymentOverTenThousandEuros): CashPayment =
    acceptedAnyPayment match {
      case CashPaymentOverTenThousandEuros(false) => CashPayment(acceptedAnyPayment, None)
      case CashPaymentOverTenThousandEuros(true)  => CashPayment(acceptedAnyPayment, cashPayment.firstDate)
    }

  def update(cashPayment: CashPayment, firstDate: CashPaymentFirstDate): CashPayment =
    CashPayment(CashPaymentOverTenThousandEuros(true), Some(firstDate))
}
