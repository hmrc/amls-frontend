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

package models.hvd

import cats.data.Validated.Valid
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.libs.json.Reads

sealed trait CashPayment

case class CashPaymentYes(paymentDate: Option[LocalDate]) extends CashPayment

case object CashPaymentNo extends CashPayment

object CashPayment {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, CashPayment] = From[UrlFormEncoded] { __ =>
    import jto.validation

    // TODO: Need to separate out model objects as can't use same rule for both forms

    if ((__ \ "acceptedAnyPayment").nonEmpty) {
      (__ \ "acceptedAnyPayment").read[Boolean].withMessage("error.required.hvd.accepted.cash.payment") flatMap {
        case true => Rule.fromMapping { _ => Valid(CashPaymentYes(None)) }
        case false => Rule.fromMapping { _ => Valid(CashPaymentNo) }
      }
    } else {
      (__ \ "paymentDate").read(localDateFutureRule) map Option.apply map CashPaymentYes.apply
    }
  }

  implicit def formWrites: Write[CashPayment, UrlFormEncoded] = Write {
    case CashPaymentYes(None) =>  Map("acceptedAnyPayment" -> Seq("true"))
    case CashPaymentYes(Some(date)) =>
      Map("acceptedAnyPayment" -> Seq("true"),
        "paymentDate.day" -> Seq(date.get(DateTimeFieldType.dayOfMonth()).toString),
        "paymentDate.month" -> Seq(date.get(DateTimeFieldType.monthOfYear()).toString),
        "paymentDate.year" -> Seq(date.get(DateTimeFieldType.year()).toString)
    )
    case CashPaymentNo => Map("acceptedAnyPayment" -> Seq("false"))
  }


  implicit val jsonReads: Reads[CashPayment] = {
    import play.api.libs.json.Reads._
    import play.api.libs.json._

    (__ \ "acceptedAnyPayment").read[Boolean] flatMap {
      case true => (__ \ "paymentDate").read[LocalDate] map Option.apply map CashPaymentYes.apply
      case false => Reads(_ => JsSuccess(CashPaymentNo))
    }
  }

  implicit val jsonWrites = {
    import play.api.libs.json.Writes._
    import play.api.libs.json._

    Writes[CashPayment] {
      case CashPaymentYes(None) => Json.obj(
        "acceptedAnyPayment" -> true
      )
      case CashPaymentYes(Some(b)) => Json.obj(
        "acceptedAnyPayment" -> true,
        "paymentDate" -> b.toString
      )
      case CashPaymentNo => Json.obj("acceptedAnyPayment" -> false)
    }
  }
}
