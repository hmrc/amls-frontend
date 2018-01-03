/*
 * Copyright 2018 HM Revenue & Customs
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

import jto.validation.forms.Rules.{maxLength, notEmpty}
import jto.validation.{From, Path, Rule, ValidationError, Write}
import jto.validation.forms.UrlFormEncoded
import models.FormTypes.{basicPunctuationPattern, notEmptyStrip}
import play.api.libs.json._

sealed trait WithdrawalReason

object WithdrawalReason {

  case object OutOfScope extends WithdrawalReason

  case object NotTradingInOwnRight extends WithdrawalReason

  case object UnderAnotherSupervisor extends WithdrawalReason

  case class Other(otherReason: String) extends WithdrawalReason

  import utils.MappingUtils.Implicits._

  private val maxTextLength = 40
  private val specifyOtherReasonType = notEmptyStrip.withMessage("error.required.withdrawal.reason.other") andThen
    notEmpty.withMessage("error.required.withdrawal.reason.other") andThen
    maxLength(maxTextLength) andThen
    basicPunctuationPattern()

  implicit val formRule: Rule[UrlFormEncoded, WithdrawalReason] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "withdrawalReason").read[String].withMessage("error.required.withdrawal.reason") flatMap {
      case "01" => OutOfScope
      case "02" => NotTradingInOwnRight
      case "03" => UnderAnotherSupervisor
      case "04" => (__ \ "specifyOtherReason").read(specifyOtherReasonType) map Other.apply
      case _ =>
        (Path \ "withdrawalReason") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[WithdrawalReason, UrlFormEncoded] = Write {
    case OutOfScope => Map("withdrawalReason" -> "01")
    case NotTradingInOwnRight => Map("withdrawalReason" -> "02")
    case UnderAnotherSupervisor => Map("withdrawalReason" -> "03")
    case Other(reason) => Map("withdrawalReason" -> "04", "specifyOtherReason" -> reason)
  }

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
        play.api.data.validation.ValidationError("error.invalid")
    }
  }

  implicit val jsonRedressWrites = Writes[WithdrawalReason] {
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
