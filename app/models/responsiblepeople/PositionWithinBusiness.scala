/*
 * Copyright 2020 HM Revenue & Customs
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

package models.responsiblepeople

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Invalid, Path, Rule, Valid, ValidationError, Write}
import models.FormTypes.basicPunctuationPattern
import play.api.i18n.Messages
import play.api.libs.json._
import utils.TraversableValidators.minLengthR

sealed trait PositionWithinBusiness

case object BeneficialOwner extends PositionWithinBusiness

case object Director extends PositionWithinBusiness

case object InternalAccountant extends PositionWithinBusiness

case object NominatedOfficer extends PositionWithinBusiness

case object Partner extends PositionWithinBusiness

case object SoleProprietor extends PositionWithinBusiness

case object DesignatedMember extends PositionWithinBusiness

case class Other(value: String) extends PositionWithinBusiness

object PositionWithinBusiness
{

  import jto.validation.forms.Rules._
  import utils.MappingUtils.Implicits.RichRule

  def getPrettyName(position:PositionWithinBusiness)(implicit message: Messages): String = {
    import play.api.i18n.Messages

    import scala.language.implicitConversions
    position match {
      case BeneficialOwner => Messages("declaration.addperson.lbl.01")
      case Director => Messages("responsiblepeople.position_within_business.lbl.02")
      case InternalAccountant => Messages("responsiblepeople.position_within_business.lbl.03")
      case NominatedOfficer => Messages("responsiblepeople.position_within_business.lbl.04")
      case Partner => Messages("responsiblepeople.position_within_business.lbl.05")
      case SoleProprietor => Messages("responsiblepeople.position_within_business.lbl.06")
      case DesignatedMember => Messages("responsiblepeople.position_within_business.lbl.07")
      case Other(other) => other
      case _ => ""
    }
  }

  implicit val formWrite = Write[PositionWithinBusiness, String] {
    case BeneficialOwner => "01"
    case Director => "02"
    case InternalAccountant => "03"
    case NominatedOfficer => "04"
    case Partner => "05"
    case SoleProprietor => "06"
    case DesignatedMember => "07"
    case Other(_) => "other"
  }

  implicit def formWrites(implicit w: Write[PositionWithinBusiness, String]) =
    Write[Set[PositionWithinBusiness], UrlFormEncoded] { data =>
      Map("positions[]" -> data.toSeq.map(w.writes)) ++ {
        data.collectFirst {
          case Other(v) => "otherPosition" -> Seq(v)
        }
      }
    }

  private[responsiblepeople] implicit val jsonWrites = Writes[PositionWithinBusiness] {
    case BeneficialOwner => JsString("01")
    case Director => JsString("02")
    case InternalAccountant => JsString("03")
    case NominatedOfficer => JsString("04")
    case Partner => JsString("05")
    case SoleProprietor => JsString("06")
    case DesignatedMember => JsString("07")
    case Other(value) => Json.obj("other" -> value)
  }

  private[responsiblepeople] implicit val jsonReads: Reads[PositionWithinBusiness] =
    Reads {
      case JsString("01") => JsSuccess(BeneficialOwner)
      case JsString("02") => JsSuccess(Director)
      case JsString("03") => JsSuccess(InternalAccountant)
      case JsString("04") => JsSuccess(NominatedOfficer)
      case JsString("05") => JsSuccess(Partner)
      case JsString("06") => JsSuccess(SoleProprietor)
      case JsString("07") => JsSuccess(DesignatedMember)
      case JsObject(m) if m.contains("other") => JsSuccess(Other(m("other").as[String]))
      case _ => JsError((JsPath \ "positions") -> play.api.libs.json.JsonValidationError("error.invalid"))
    }

  private[responsiblepeople] val atLeastOneRule = minLengthR[Set[String]](1).withMessage("error.required.positionWithinBusiness")
  private val otherLength = 255
  private val maxLengthRule = optionR(maxLength(otherLength) andThen basicPunctuationPattern())

  private[responsiblepeople] val fullySpecifiedRule = Rule[(Set[String], Option[String]), Set[PositionWithinBusiness]]  {
    case (s, None) if s.contains("other") =>
      Invalid(Seq(Path \ "otherPosition" -> Seq(ValidationError("responsiblepeople.position_within_business.other_position.othermissing"))))
    case (s, o) =>
      Valid(s.map {
        case "01" => BeneficialOwner
        case "02" => Director
        case "03" => InternalAccountant
        case "04" => NominatedOfficer
        case "05" => Partner
        case "06" => SoleProprietor
        case "07" => DesignatedMember
        case "other" => Other(o.get)
        case _ => throw new IllegalArgumentException("!")
      })
  }

  implicit val positionsRule:Rule[UrlFormEncoded, Set[PositionWithinBusiness]] = From[UrlFormEncoded] { __ =>
    ((__ \ "positions").read(atLeastOneRule)  ~
      (__ \ "otherPosition").read(maxLengthRule)).tupled
      .andThen(fullySpecifiedRule)
  }
}
