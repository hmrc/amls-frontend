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

package models.declaration

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import models.FormTypes._
import play.api.libs.json._

sealed trait RoleWithinBusiness

case object BeneficialShareholder extends RoleWithinBusiness

case object Director extends RoleWithinBusiness

case object ExternalAccountant extends RoleWithinBusiness

case object InternalAccountant extends RoleWithinBusiness

case object NominatedOfficer extends RoleWithinBusiness

case object Partner extends RoleWithinBusiness

case object SoleProprietor extends RoleWithinBusiness

case class Other(value: String) extends RoleWithinBusiness

object RoleWithinBusiness {

  import utils.MappingUtils.Implicits._

  val maxDetailsLength = 255

  val otherDetailsType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.declaration.specify.role") andThen
    maxLength(maxDetailsLength).withMessage("error.invalid.maxlength.255") andThen
    basicPunctuationPattern()

  implicit val formRule: Rule[UrlFormEncoded, RoleWithinBusiness] =
    From[UrlFormEncoded] { readerURLFormEncoded =>
      import jto.validation.forms.Rules._

      (readerURLFormEncoded \ "roleWithinBusiness").read[String] flatMap {
        case "01" => BeneficialShareholder
        case "02" => Director
        case "03" => ExternalAccountant
        case "04" => InternalAccountant
        case "05" => NominatedOfficer
        case "06" => Partner
        case "07" => SoleProprietor
        case "08" =>
          (readerURLFormEncoded \ "roleWithinBusinessOther").read(otherDetailsType) map Other.apply
        case _ =>
          (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.invalid"))
      }
    }

  implicit val formWrite: Write[RoleWithinBusiness, UrlFormEncoded] = Write {
    case BeneficialShareholder => "roleWithinBusiness" -> "01"
    case Director => "roleWithinBusiness" -> "02"
    case ExternalAccountant => "roleWithinBusiness" -> "03"
    case InternalAccountant => "roleWithinBusiness" -> "04"
    case NominatedOfficer => "roleWithinBusiness" -> "05"
    case Partner => "roleWithinBusiness" -> "06"
    case SoleProprietor => "roleWithinBusiness" -> "07"
    case Other(value) => Map("roleWithinBusiness" -> "08",
      "roleWithinBusinessOther" -> value)
  }

  implicit val jsonReads: Reads[RoleWithinBusiness] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (__ \ "roleWithinBusiness").read[String].flatMap[RoleWithinBusiness] {
      case "01" => BeneficialShareholder
      case "02" => Director
      case "03" => ExternalAccountant
      case "04" => InternalAccountant
      case "05" => NominatedOfficer
      case "06" => Partner
      case "07" => SoleProprietor
      case "08" => (JsPath \ "roleWithinBusinessOther").read[String] map {
        Other(_)
      }
      case _ => play.api.data.validation.ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[RoleWithinBusiness] {
    case BeneficialShareholder => Json.obj("roleWithinBusiness" -> "01")
    case Director => Json.obj("roleWithinBusiness" -> "02")
    case ExternalAccountant => Json.obj("roleWithinBusiness" -> "03")
    case InternalAccountant => Json.obj("roleWithinBusiness" -> "04")
    case NominatedOfficer => Json.obj("roleWithinBusiness" -> "05")
    case Partner => Json.obj("roleWithinBusiness" -> "06")
    case SoleProprietor => Json.obj("roleWithinBusiness" -> "07")
    case Other(value) => Json.obj(
      "roleWithinBusiness" -> "08",
      "roleWithinBusinessOther" -> value
    )
  }
}
