/*
 * Copyright 2017 HM Revenue & Customs
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

sealed trait RejectedReason extends StatusReason

object RejectedReason {

  case object NonCompliant extends RejectedReason
  case object FailedToRespond extends RejectedReason
  case object FailedToPayCharges extends RejectedReason
  case object FitAndProperFailure extends RejectedReason
  case object OtherFailed extends RejectedReason
  case object OtherRefused extends RejectedReason

  implicit def reason(reason:String) : RejectedReason = {
    reason match {
      case "01" => NonCompliant
      case "02" => FailedToRespond
      case "03" => FailedToPayCharges
      case "04" => FitAndProperFailure
      case "98" => OtherFailed
      case "99" => OtherRefused
    }
  }

  implicit val jsonWrites = Writes[RejectedReason] {
    case NonCompliant => JsString("01")
    case FailedToRespond => JsString("02")
    case FailedToPayCharges => JsString("03")
    case FitAndProperFailure => JsString("04")
    case OtherFailed => JsString("98")
    case OtherRefused => JsString("99")
  }
}
