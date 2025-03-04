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

package models.deregister

import models.{Enumerable, WithName}
import play.api.libs.json._

sealed trait DeregistrationReason {
  val value: String
}

object DeregistrationReason extends Enumerable.Implicits {
  val key: String = "deregistrationReason"

  case object OutOfScope extends WithName("outOfScope") with DeregistrationReason {
    override val value: String = "01"
  }

  case object NotTradingInOwnRight extends WithName("notTradingInOwnRight") with DeregistrationReason {
    override val value: String = "02"
  }

  case object UnderAnotherSupervisor extends WithName("underAnotherSupervisor") with DeregistrationReason {
    override val value: String = "03"
  }

  case object ChangeOfLegalEntity extends WithName("changeOfLegalEntity") with DeregistrationReason {
    override val value: String = "04"
  }

  case object HVDPolicyOfNotAcceptingHighValueCashPayments
      extends WithName("notAcceptingPayments")
      with DeregistrationReason {
    override val value: String = "05"
  }

  case class Other(otherReason: String) extends WithName("other") with DeregistrationReason {
    override val value: String = "06"
  }

  val all: Seq[DeregistrationReason] = Seq(
    OutOfScope,
    NotTradingInOwnRight,
    UnderAnotherSupervisor,
    ChangeOfLegalEntity,
    HVDPolicyOfNotAcceptingHighValueCashPayments,
    Other("")
  )

  implicit val enumerable: Enumerable[DeregistrationReason] = Enumerable(all.map(v => v.toString -> v): _*)

  import utils.MappingUtils.Implicits._

  implicit val jsonReads: Reads[DeregistrationReason] = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "deregistrationReason").read[String].flatMap[DeregistrationReason] {
      case "Out of scope"                                           => OutOfScope
      case "Not trading in own right"                               => NotTradingInOwnRight
      case "Under another supervisor"                               => UnderAnotherSupervisor
      case "Change of Legal Entity"                                 => ChangeOfLegalEntity
      case "HVD - policy of not accepting high value cash payments" => HVDPolicyOfNotAcceptingHighValueCashPayments
      case "Other, please specify"                                  =>
        (JsPath \ "specifyOtherReason").read[String] map {
          Other
        }
      case _                                                        =>
        play.api.libs.json.JsonValidationError("error.invalid")
    }
  }

  implicit val jsonRedressWrites: Writes[DeregistrationReason] = Writes[DeregistrationReason] {
    case OutOfScope                                   => Json.obj("deregistrationReason" -> "Out of scope")
    case NotTradingInOwnRight                         => Json.obj("deregistrationReason" -> "Not trading in own right")
    case UnderAnotherSupervisor                       => Json.obj("deregistrationReason" -> "Under another supervisor")
    case ChangeOfLegalEntity                          => Json.obj("deregistrationReason" -> "Change of Legal Entity")
    case HVDPolicyOfNotAcceptingHighValueCashPayments =>
      Json.obj("deregistrationReason" -> "HVD - policy of not accepting high value cash payments")
    case Other(reason)                                =>
      Json.obj(
        "deregistrationReason" -> "Other, please specify",
        "specifyOtherReason"   -> reason
      )
  }
}
