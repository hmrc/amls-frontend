/*
 * Copyright 2023 HM Revenue & Customs
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

package models.supervision

import cats.data.Validated.{Invalid, Valid}
import jto.validation.forms.Rules.{maxLength, notEmpty, minLength => _, _}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Path, Rule, ValidationError, Write}
import models.FormTypes.{basicPunctuationPattern, notEmptyStrip}
import play.api.i18n.Messages
import play.api.libs.json.Reads.StringReads
import play.api.libs.json._
import utils.TraversableValidators.minLengthR

sealed trait BusinessType {
  val value: String =
    this match {
      case AccountingTechnicians => "01"
      case CharteredCertifiedAccountants => "02"
      case InternationalAccountants => "03"
      case TaxationTechnicians => "04"
      case ManagementAccountants => "05"
      case InstituteOfTaxation => "06"
      case Bookkeepers => "07"
      case AccountantsIreland => "08"
      case AccountantsScotland => "09"
      case AccountantsEnglandandWales => "10"
      case FinancialAccountants => "11"
      case AssociationOfBookkeepers => "12"
      case LawSociety => "13"
      case Other(_) => "14"
    }

  def getMessage()(implicit messages: Messages): String = {
    val message = s"supervision.memberofprofessionalbody.lbl."
    this match {
      case AccountingTechnicians => Messages(s"${message}01")
      case CharteredCertifiedAccountants => Messages(s"${message}02")
      case InternationalAccountants => Messages(s"${message}03")
      case TaxationTechnicians => Messages(s"${message}04")
      case ManagementAccountants => Messages(s"${message}05")
      case InstituteOfTaxation => Messages(s"${message}06")
      case Bookkeepers => Messages(s"${message}07")
      case AccountantsIreland => Messages(s"${message}08")
      case AccountantsScotland => Messages(s"${message}09")
      case AccountantsEnglandandWales => Messages(s"${message}10")
        .replace("Accountants of England", "Accountants in England")
      case FinancialAccountants => Messages(s"${message}11")
      case AssociationOfBookkeepers => Messages(s"${message}12")
      case LawSociety => Messages(s"${message}13")
      case Other(_) => Messages(s"${message}14")
    }
  }
}

case object AccountingTechnicians extends BusinessType

case object CharteredCertifiedAccountants extends BusinessType

case object InternationalAccountants extends BusinessType

case object TaxationTechnicians extends BusinessType

case object ManagementAccountants extends BusinessType

case object InstituteOfTaxation extends BusinessType

case object Bookkeepers extends BusinessType

case object AccountantsIreland extends BusinessType

case object AccountantsScotland extends BusinessType

case object AccountantsEnglandandWales extends BusinessType

case object FinancialAccountants extends BusinessType

case object AssociationOfBookkeepers extends BusinessType

case object LawSociety extends BusinessType

case class Other(businessDetails: String) extends BusinessType

case class ProfessionalBodies(businessTypes: Set[BusinessType])

object ProfessionalBodies {

  import utils.MappingUtils.Implicits._

  val maxSpecifyDetailsLength = 255
  val specifyOtherType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.supervision.business.details") andThen
    maxLength(maxSpecifyDetailsLength).withMessage("error.invalid.supervision.business.details.length.255") andThen
    basicPunctuationPattern().withMessage("error.invalid.supervision.business.details")

  def stringToRule(businessType: BusinessType): Rule[UrlFormEncoded, BusinessType] =
    Rule[UrlFormEncoded, BusinessType](_ => Valid(businessType))

  implicit val formRule: Rule[UrlFormEncoded, ProfessionalBodies] = From[UrlFormEncoded] { __ =>
    (__ \ "businessType").read(minLengthR[Set[String]](1).withMessage("error.required.supervision.one.professional.body")) flatMap { setOfStrings =>
      setOfStrings.map {
        case "01" => stringToRule(AccountingTechnicians)
        case "02" => stringToRule(CharteredCertifiedAccountants)
        case "03" => stringToRule(InternationalAccountants)
        case "04" => stringToRule(TaxationTechnicians)
        case "05" => stringToRule(ManagementAccountants)
        case "06" => stringToRule(InstituteOfTaxation)
        case "07" => stringToRule(Bookkeepers)
        case "08" => stringToRule(AccountantsIreland)
        case "09" => stringToRule(AccountantsScotland)
        case "10" => stringToRule(AccountantsEnglandandWales)
        case "11" => stringToRule(FinancialAccountants)
        case "12" => stringToRule(AssociationOfBookkeepers)
        case "13" => stringToRule(LawSociety)
        case "14" =>
          (__ \ "specifyOtherBusiness").read(specifyOtherType) map Other.apply
        case _ =>
          Rule[UrlFormEncoded, BusinessType] { _ =>
            Invalid(Seq((Path \ "businessType") -> Seq(ValidationError("error.invalid"))))
          }
      }.foldLeft[Rule[UrlFormEncoded, Set[BusinessType]]](
        Rule[UrlFormEncoded, Set[BusinessType]](_ => Valid(Set.empty))
      ) {
        case (start, businessTypeRule) =>
          businessTypeRule flatMap { businessType =>
            start map { businessTypes =>
              businessTypes + businessType
            }
          }
      } map ProfessionalBodies.apply
    }
  }

  implicit def formWrites = Write[ProfessionalBodies, UrlFormEncoded] { businessTypes =>
    Map(
      "businessType[]" -> (businessTypes.businessTypes map {
        _.value
      }).toSeq
    ) ++ businessTypes.businessTypes.foldLeft[UrlFormEncoded](Map.empty) {
      case (form, Other(name)) => form ++ Map("specifyOtherBusiness" -> Seq(name))
      case (form, _) => form
    }
  }

  def stringToReader(businessType: BusinessType): Reads[BusinessType] =
    Reads(_ => JsSuccess(businessType)) map identity[BusinessType]

  implicit val jsonReader: Reads[ProfessionalBodies] = (__ \ "businessType").read[Set[String]] flatMap { setOfStrings =>
    (setOfStrings map {
      case "01" => stringToReader(AccountingTechnicians)
      case "02" => stringToReader(CharteredCertifiedAccountants)
      case "03" => stringToReader(InternationalAccountants)
      case "04" => stringToReader(TaxationTechnicians)
      case "05" => stringToReader(ManagementAccountants)
      case "06" => stringToReader(InstituteOfTaxation)
      case "07" => stringToReader(Bookkeepers)
      case "08" => stringToReader(AccountantsIreland)
      case "09" => stringToReader(AccountantsScotland)
      case "10" => stringToReader(AccountantsEnglandandWales)
      case "11" => stringToReader(FinancialAccountants)
      case "12" => stringToReader(AssociationOfBookkeepers)
      case "13" => stringToReader(LawSociety)
      case "14" =>
        (JsPath \ "specifyOtherBusiness").read[String].map(Other.apply) map identity[BusinessType]
      case _ =>
        Reads(_ => JsError((JsPath \ "businessType") -> play.api.libs.json.JsonValidationError("error.invalid")))
    }).foldLeft[Reads[Set[BusinessType]]](
      Reads[Set[BusinessType]](_ => JsSuccess(Set.empty))
    ) {
      (start, businessTypeReader) =>
        businessTypeReader flatMap { businessType =>
          start map { businessTypes =>
            businessTypes + businessType
          }
        }
    }
  } map ProfessionalBodies.apply

  implicit val jsonWrites: Writes[ProfessionalBodies] = Writes[ProfessionalBodies]{ businessTypes =>
    Json.obj(
      "businessType" -> (businessTypes.businessTypes map (_.value)).toSeq
    ) ++ businessTypes.businessTypes.foldLeft[JsObject](Json.obj()) {
      case (json, Other(name)) => json ++ Json.obj("specifyOtherBusiness" -> name)
      case (json, _) => json
    }
  }

}