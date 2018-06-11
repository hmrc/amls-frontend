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

package models.bankdetails

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.libs.json._

sealed trait BankAccountType {
  def getBankAccountTypeID:String = {
    this match {
      case PersonalAccount => "01"
      case BelongsToBusiness => "02"
      case BelongsToOtherBusiness => "03"
      case NoBankAccountUsed => "04"
    }
  }
}

case object PersonalAccount extends BankAccountType
case object BelongsToBusiness extends BankAccountType
case object BelongsToOtherBusiness extends BankAccountType
case object NoBankAccountUsed extends BankAccountType

object BankAccountType {

  import utils.MappingUtils.Implicits._

  implicit val formReads: Rule[UrlFormEncoded, Option[BankAccountType]] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._

      (__ \ "bankAccountType").read[String].withMessage("error.bankdetails.accounttype") flatMap {
        case "01" => Some(PersonalAccount)
        case "02" => Some(BelongsToBusiness)
        case "03" => Some(BelongsToOtherBusiness)
        case "04" => Some(NoBankAccountUsed)
        case _ =>
          (Path \ "bankAccountType") -> Seq(ValidationError("error.invalid"))
      }
    }

  implicit val formWrites:Write[Option[BankAccountType], UrlFormEncoded] = Write {
    case Some(PersonalAccount) => "bankAccountType" -> "01"
    case Some(BelongsToBusiness) => "bankAccountType" -> "02"
    case Some(BelongsToOtherBusiness) => "bankAccountType" -> "03"
    case Some(NoBankAccountUsed) => "bankAccountType" -> "04"
    case _ => Map.empty
  }

  implicit val jsonReads : Reads[BankAccountType] = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "bankAccountType").read[String] flatMap {
      case "01" => PersonalAccount
      case "02" => BelongsToBusiness
      case "03" => BelongsToOtherBusiness
      case "04" => NoBankAccountUsed
      case _ =>
        play.api.data.validation.ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[BankAccountType] {
    case PersonalAccount => Json.obj("bankAccountType"->"01")
    case BelongsToBusiness => Json.obj("bankAccountType" -> "02")
    case BelongsToOtherBusiness => Json.obj("bankAccountType" -> "03")
    case NoBankAccountUsed => Json.obj("bankAccountType" -> "04")
  }

}
