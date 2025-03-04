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

package models.declaration

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

  implicit val jsonReads: Reads[RoleWithinBusiness] = {
    import play.api.libs.json._

    (__ \ "roleWithinBusiness").read[String].flatMap[RoleWithinBusiness] {
      case "01" => BeneficialShareholder
      case "02" => Director
      case "03" => ExternalAccountant
      case "04" => InternalAccountant
      case "05" => NominatedOfficer
      case "06" => Partner
      case "07" => SoleProprietor
      case "08" =>
        (JsPath \ "roleWithinBusinessOther").read[String] map {
          Other(_)
        }
      case _    => play.api.libs.json.JsonValidationError("error.invalid")
    }
  }

  implicit val jsonWrites: Writes[RoleWithinBusiness] = Writes[RoleWithinBusiness] {
    case BeneficialShareholder => Json.obj("roleWithinBusiness" -> "01")
    case Director              => Json.obj("roleWithinBusiness" -> "02")
    case ExternalAccountant    => Json.obj("roleWithinBusiness" -> "03")
    case InternalAccountant    => Json.obj("roleWithinBusiness" -> "04")
    case NominatedOfficer      => Json.obj("roleWithinBusiness" -> "05")
    case Partner               => Json.obj("roleWithinBusiness" -> "06")
    case SoleProprietor        => Json.obj("roleWithinBusiness" -> "07")
    case Other(value)          =>
      Json.obj(
        "roleWithinBusiness"      -> "08",
        "roleWithinBusinessOther" -> value
      )
  }
}
