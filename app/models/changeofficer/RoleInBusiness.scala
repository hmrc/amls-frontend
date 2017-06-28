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
import jto.validation.{Path, ValidationError, From, Rule}
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

object RoleInBusiness {
  import utils.MappingUtils.Implicits._

  implicit val roleReads = Rule[String, Role] {
    case "soleprop" => Valid(SoleProprietor)
    case "bensharehold" => Valid(BeneficialShareholder)
    case "director" => Valid(Director)
    case "extAccountant" => Valid(ExternalAccountant)
    case "intAccountant" => Valid(InternalAccountant)
    case "partner" => Valid(Partner)
    case "desigmemb" => Valid(DesignatedMember)
    case _ => Invalid(Seq(Path -> Seq(ValidationError("error.invalid"))))
  }

  implicit val formReads: Rule[UrlFormEncoded, RoleInBusiness] = From[UrlFormEncoded] {
    import jto.validation.forms.Rules._

    __ => (__ \ "positions").read(minLengthR[Set[Role]](1)).withMessage("changeofficer.roleinbusiness.validationerror") map { s => RoleInBusiness(s) }
  }
}
