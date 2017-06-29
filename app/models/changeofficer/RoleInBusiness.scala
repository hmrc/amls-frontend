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

import cats.Functor
import cats.data.Validated.{Invalid, Valid}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Path, Rule, ValidationError}
import play.api.libs.json._
import utils.TraversableValidators._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._
import utils.MappingUtils.Implicits._

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

  val stringToRole: PartialFunction[String, Role] = {
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

  implicit val jsonWrites: Writes[RoleInBusiness] = {
    (__ \ "positions").write[Seq[String]].contramap(_.roles.toSeq.map(roleToString))
  }

  val roleJsonReads = new Reads[Set[Role]] {
    override def reads(json: JsValue) = json.as[Seq[String]] match {
      case strings if strings.forall(stringToRole.isDefinedAt) => JsSuccess((strings map stringToRole).toSet)
      case _ => JsError("changeofficer.roleinbusiness.validationerror")
    }
  }

  implicit val jsonReads: Reads[RoleInBusiness] = {
    (__ \ "positions").read(roleJsonReads).map(r => RoleInBusiness(r))
  }

  val roleFormReads = Rule.fromMapping[Seq[String], Set[Role]] {
    case x if x.nonEmpty && x.forall(stringToRole.isDefinedAt) => Valid(x.map(stringToRole).toSet)
    case _ => Invalid(ValidationError("changeofficer.roleinbusiness.validationerror"))
  }

  implicit val formReads: Rule[UrlFormEncoded, RoleInBusiness] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    ((__ \ "positions").read[Seq[String]] andThen roleFormReads.repath(_ => Path \ "positions")) map { r => RoleInBusiness(r) }
  }
}
