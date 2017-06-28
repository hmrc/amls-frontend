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

package models.changeofficer

import cats.data.Validated.{Invalid, Valid}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Path, Rule, ValidationError}
import play.api.libs.json._
import utils.TraversableValidators._

case class RoleInBusiness(roles: Set[Role])

sealed trait Role

object Role {

}

case object SoleProprietor extends Role
case object InternalAccountant extends Role
case object BeneficialShareholder extends Role
case object Director extends Role
case object ExternalAccountant extends Role
case object Partner extends Role
case object DesignatedMember extends Role

case class Other(text: String) extends Role

object RoleInBusiness {
  val key = "changeofficer.roleinbusiness"

  import utils.MappingUtils.Implicits._

  val stringToRole = PartialFunction[String, Role] {
    case "soleprop" => SoleProprietor
    case "bensharehold" => BeneficialShareholder
    case "director" => Director
    case "extAccountant" => ExternalAccountant
    case "intAccountant" => InternalAccountant
    case "partner" => Partner
    case "desigmemb" => DesignatedMember
  }

  def roleToString(r: Role): String = r match {
      case SoleProprietor => "soleprop"
      case BeneficialShareholder => "bensharehold"
      case Director => "director"
      case ExternalAccountant => "extAccountant"
      case InternalAccountant => "intAccountant"
      case Partner => "partner"
      case DesignatedMember => "desigmemb"
    }

  implicit val jsonWrites = new Writes[RoleInBusiness] {
    override def writes(o: RoleInBusiness) = Json.obj("positions" -> JsArray(o.roles.map(r => JsString(roleToString(r))).toSeq))
  }

  implicit val jsonReads: Reads[RoleInBusiness] = {
    import play.api.libs.json._
    import play.api.libs.json.Reads._

    (__ \ "positions").read[Seq[String]].map(x => x.map(stringToRole)).map(y => RoleInBusiness(y.toSet))
  }

  implicit val roleReads = Rule[String, Role] { r =>
    if (stringToRole.isDefinedAt(r)) {
      Valid(stringToRole(r))
    } else {
      Invalid(Seq(Path -> Seq(ValidationError("error.invalid"))))
    }
  }

  implicit val formReads: Rule[UrlFormEncoded, RoleInBusiness] = From[UrlFormEncoded] {
    import jto.validation.forms.Rules._

    __ => (__ \ "positions").read(minLengthR[Set[Role]](1)).withMessage("changeofficer.roleinbusiness.validationerror") map { s => RoleInBusiness(s) }
  }
}
