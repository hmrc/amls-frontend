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
import jto.validation._
import models.ValidationRule
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.MappingUtils.Implicits._
import models.FormTypes._
import jto.validation.forms.Rules._

case class RoleInBusiness(roles: Set[Role])

sealed trait Role

case object SoleProprietor extends Role
case object BeneficialOwner extends Role
case object Director extends Role
case object InternalAccountant extends Role
case object Partner extends Role
case object DesignatedMember extends Role

case class Other(text: String) extends Role

object RoleInBusiness {
  val key = "changeofficerRoleinbusiness"
  private val validationErrorKey = "changeofficer.roleinbusiness.validationerror"

  //noinspection ScalaStyle
  def stringToRole(role: String, other: Option[String]): Role = role match {
    case "soleprop" => SoleProprietor
    case "benown" => BeneficialOwner
    case "director" => Director
    case "intAccountant" => InternalAccountant
    case "partner" => Partner
    case "desigmemb" => DesignatedMember
    case "other" if other.isDefined => Other(other.get)
  }

  def setRoles(roles: Seq[String], other: Option[String]) = if(roles.contains("")){
    Set.empty[Role]
  } else {
    roles.map(v => stringToRole(v, other)).toSet
  }

  def roleToString(r: Role): String = r match {
      case SoleProprietor => "soleprop"
      case Director => "director"
      case BeneficialOwner => "benown"
      case InternalAccountant => "intAccountant"
      case Partner => "partner"
      case DesignatedMember => "desigmemb"
      case Other(_) => "other"
    }

  implicit val jsonWrites: Writes[RoleInBusiness] = {
    ((__ \ "positions").write[Seq[String]] ~
      (__ \ "otherPosition").writeNullable[String]) { r =>
        val roleSet = r.roles.map(roleToString).toSeq
        val other = r.roles.collect { case Other(o) => o }.headOption
        (roleSet, other)
      }
  }

  implicit val jsonReads: Reads[RoleInBusiness] = {
    import play.api.libs.json.Reads._
    ((__ \ "positions").read[Seq[String]] and
      (__ \ "otherPosition").readNullable[String]).tupled.flatMap {
        case (roles, other) => RoleInBusiness(roles.map(v => stringToRole(v, other)).toSet)
        case _ => Reads { _ => JsError(JsPath \ "positions", validationErrorKey) }
    }
  }

  val roleFormReads = Rule.fromMapping[(Seq[String], Option[String]), Set[Role]] {
    case (roles, other) if roles.nonEmpty => Valid(setRoles(roles,other))
    case _ => Invalid(ValidationError(validationErrorKey))
  }

  val otherValidationRule: ValidationRule[(Seq[String], Option[String])] = Rule[(Seq[String], Option[String]), (Seq[String], Option[String])] {
    case (roles, None) if roles.contains("other") =>
      Invalid(Seq(Path \ "otherPosition" -> Seq(ValidationError("changeofficer.roleinbusiness.validationerror.othermissing"))))
    case x => Valid(x)
  }

  val maxDetailsLength = 255

  val otherDetailsType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.declaration.specify.role") andThen
    maxLength(maxDetailsLength).withMessage("error.invalid.maxlength.255") andThen
    basicPunctuationPattern()

  implicit val formReads: Rule[UrlFormEncoded, RoleInBusiness] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    ((__ \ "positions").read[Seq[String]] ~
      (__ \ "otherPosition").read[Option[String]])
        .tupled
        .andThen(otherValidationRule)
        .andThen(roleFormReads.repath(_ => Path \ "positions")) map { r => RoleInBusiness(r) }
  }

  implicit def formWrites: Write[RoleInBusiness, UrlFormEncoded] = Write[RoleInBusiness, UrlFormEncoded] { data =>
        val roleSet = data.roles.map(roleToString).toSeq
        val otherRole = data.roles.collect {
          case Other(o) => o
        }.toSeq
        Map(
          "positions[]" -> roleSet,
          "otherPosition" -> otherRole
        )
  }
}
