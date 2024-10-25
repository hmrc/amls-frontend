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

package models.withdrawal

import models.{Enumerable, WithName}
import play.api.libs.json._

sealed trait WithdrawalReason {
  val value: String
}

object WithdrawalReason extends Enumerable.Implicits {

  val key: String = "withdrawalReason"

  case object OutOfScope extends WithName("outOfScope") with WithdrawalReason {
    override val value: String = "01"
  }

  case object NotTradingInOwnRight extends WithName("notTradingInOwnRight") with WithdrawalReason {
    override val value: String = "02"
  }

  case object UnderAnotherSupervisor extends WithName("underAnotherSupervisor") with WithdrawalReason {
    override val value: String = "03"
  }

  case class Other(otherReason: String) extends WithName("other") with WithdrawalReason {
    override val value: String = "04"
  }

  val all: Seq[WithdrawalReason] = Seq(
    OutOfScope,
    NotTradingInOwnRight,
    UnderAnotherSupervisor,
    Other("")
  )

  implicit val enumerable: Enumerable[WithdrawalReason] = Enumerable(all.map(v => v.toString -> v): _*)

  import utils.MappingUtils.Implicits._

  implicit val jsonReads: Reads[WithdrawalReason] = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "withdrawalReason").read[String].flatMap[WithdrawalReason] {
      case "Out of scope" => OutOfScope
      case "Not trading in own right" => NotTradingInOwnRight
      case "Under another supervisor" => UnderAnotherSupervisor
      case "Other, please specify" =>
        (JsPath \ "specifyOtherReason").read[String] map {
          Other
        }
      case _ =>
        play.api.libs.json.JsonValidationError("error.invalid")
    }
  }

  implicit val jsonRedressWrites: Writes[WithdrawalReason] = Writes[WithdrawalReason] {
    case OutOfScope => Json.obj("withdrawalReason" -> "Out of scope")
    case NotTradingInOwnRight => Json.obj("withdrawalReason" -> "Not trading in own right")
    case UnderAnotherSupervisor => Json.obj("withdrawalReason" -> "Under another supervisor")
    case Other(reason) =>
      Json.obj(
        "withdrawalReason" -> "Other, please specify",
        "specifyOtherReason" -> reason
      )
  }
}
