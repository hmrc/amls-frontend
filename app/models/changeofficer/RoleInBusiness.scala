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

import cats.{Applicative, Functor}
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
  private val validationErrorKey = "changeofficer.roleinbusiness.validationerror"

  //noinspection ScalaStyle
  def stringToRole(role: String, other: Option[String]): Option[Role] = role match {
    case "soleprop" => Some(SoleProprietor)
    case "director" => Some(Director)
    case "bensharehold" => Some(BeneficialShareholder)
    case "extAccountant" => Some(ExternalAccountant)
    case "intAccountant" => Some(InternalAccountant)
    case "partner" => Some(Partner)
    case "desigmemb" => Some(DesignatedMember)
    case "other" if other.isDefined => Some(Other(other.get))
    case _ => None
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

  private val strToRoleWithOther = (other: Option[String]) => Function.unlift[String, Role]((stringToRole _)(_: String, other))

  implicit val jsonWrites: Writes[RoleInBusiness] = {
    (__ \ "positions").write[Seq[String]].contramap(_.roles.toSeq.map(roleToString))
  }

  implicit val jsonReads: Reads[RoleInBusiness] = {
    ((__ \ "positions").read[Seq[String]] and
      (__ \ "otherPosition").readNullable[String]).tupled.flatMap {
        case (values, other) if values.nonEmpty && values.forall(strToRoleWithOther(other).isDefinedAt) =>
          RoleInBusiness(values.map(v => stringToRole(v, other).get).toSet)
        case _ => Reads { _ => JsError(JsPath \ "positions", validationErrorKey) }
    }
  }

  val roleFormReads = Rule.fromMapping[(Seq[String], Option[String]), Set[Role]] {
    case (values, other) if values.nonEmpty && values.forall(strToRoleWithOther(other).isDefinedAt) =>
      Valid(values.map(v => stringToRole(v, other).get).toSet)
    case _ => Invalid(ValidationError(validationErrorKey))
  }

  implicit val formReads: Rule[UrlFormEncoded, RoleInBusiness] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    ((__ \ "positions").read[Seq[String]] ~
      (__ \ "otherPosition").read[Option[String]])
        .tupled
        .andThen(roleFormReads)
        .repath(_ => Path \ "positions") map { r => RoleInBusiness(r) }
  }
}
