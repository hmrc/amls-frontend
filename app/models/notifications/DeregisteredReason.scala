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

package models.notifications

import play.api.libs.json._

sealed trait DeregisteredReason extends StatusReason

object DeregisteredReason {

  case object CeasedTrading extends DeregisteredReason
  case object HVDNoCashPayment extends DeregisteredReason
  case object OutOfScope extends DeregisteredReason
  case object NotTrading extends DeregisteredReason
  case object UnderAnotherSupervisor extends DeregisteredReason
  case object ChangeOfLegalEntity extends DeregisteredReason

  case object Other extends DeregisteredReason

    implicit def reason(reason:String) : DeregisteredReason = {
      reason match {
        case "01" => CeasedTrading
        case "02" => HVDNoCashPayment
        case "03" => OutOfScope
        case "04" => NotTrading
        case "05" => UnderAnotherSupervisor
        case "06" => ChangeOfLegalEntity
        case "99" => Other
      }
   }

  implicit val jsonWrites = Writes[DeregisteredReason] {
    case CeasedTrading => JsString("01")
    case HVDNoCashPayment => JsString("02")
    case OutOfScope => JsString("03")
    case NotTrading => JsString("04")
    case UnderAnotherSupervisor => JsString("05")
    case ChangeOfLegalEntity => JsString("06")
    case Other => JsString("99")
  }
}
