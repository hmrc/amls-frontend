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

package models.supervision

import models.FormTypes._
import jto.validation.forms.UrlFormEncoded
import jto.validation._
import jto.validation.ValidationError
import play.api.libs.json._
import play.api.libs.json.Reads.StringReads
import jto.validation.forms.Rules.{minLength => _, _}
import utils.TraversableValidators.minLengthR
import cats.data.Validated.{Invalid, Valid}

sealed trait ProfessionalBodyMember

case class ProfessionalBodyMemberYes(transactionType: Set[BusinessType]) extends ProfessionalBodyMember

case object ProfessionalBodyMemberNo extends ProfessionalBodyMember

object ProfessionalBodyMember {

  import utils.MappingUtils.Implicits._

  val maxSpecifyDetailsLength = 255
  val specifyOtherType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.supervision.business.details") andThen
    maxLength(maxSpecifyDetailsLength).withMessage("error.invalid.supervision.business.details") andThen
    basicPunctuationPattern()

  implicit val formRule: Rule[UrlFormEncoded, ProfessionalBodyMember] =
    From[UrlFormEncoded] { __ =>
      (__ \ "isAMember").read[Boolean].withMessage("error.required.supervision.business.a.member") flatMap {
        case true =>
          (__ \ "businessType").read(minLengthR[Set[String]](1).withMessage("error.required.supervision.one.professional.body")) flatMap { z =>
            z.map {
              case "01" => Rule[UrlFormEncoded, BusinessType](_ => Valid(AccountingTechnicians))
              case "02" => Rule[UrlFormEncoded, BusinessType](_ => Valid(CharteredCertifiedAccountants))
              case "03" => Rule[UrlFormEncoded, BusinessType](_ => Valid(InternationalAccountants))
              case "04" => Rule[UrlFormEncoded, BusinessType](_ => Valid(TaxationTechnicians))
              case "05" => Rule[UrlFormEncoded, BusinessType](_ => Valid(ManagementAccountants))
              case "06" => Rule[UrlFormEncoded, BusinessType](_ => Valid(InstituteOfTaxation))
              case "07" => Rule[UrlFormEncoded, BusinessType](_ => Valid(Bookkeepers))
              case "08" => Rule[UrlFormEncoded, BusinessType](_ => Valid(AccountantsIreland))
              case "09" => Rule[UrlFormEncoded, BusinessType](_ => Valid(AccountantsScotland))
              case "10" => Rule[UrlFormEncoded, BusinessType](_ => Valid(AccountantsEnglandandWales))
              case "11" => Rule[UrlFormEncoded, BusinessType](_ => Valid(FinancialAccountants))
              case "12" => Rule[UrlFormEncoded, BusinessType](_ => Valid(AssociationOfBookkeepers))
              case "13" => Rule[UrlFormEncoded, BusinessType](_ => Valid(LawSociety))
              case "14" =>
                (__ \ "specifyOtherBusiness").read(specifyOtherType) map Other.apply
              case _ =>
                Rule[UrlFormEncoded, BusinessType] { _ =>
                  Invalid(Seq((Path \ "businessType") -> Seq(ValidationError("error.invalid"))))
                }
            }.foldLeft[Rule[UrlFormEncoded, Set[BusinessType]]](
              Rule[UrlFormEncoded, Set[BusinessType]](_ => Valid(Set.empty))
            ) {
              case (m, n) =>
                n flatMap { x =>
                  m map {
                    _ + x
                  }
                }
            } map ProfessionalBodyMemberYes.apply
          }

        case false => Rule.fromMapping { _ => Valid(ProfessionalBodyMemberNo) }
      }
    }

  implicit def formWrites = Write[ProfessionalBodyMember, UrlFormEncoded] {
    case ProfessionalBodyMemberNo => Map("isAMember" -> "false")
    case ProfessionalBodyMemberYes(transactions) =>
      Map(
        "isAMember" -> Seq("true"),
        "businessType[]" -> (transactions map {
          _.value
        }).toSeq
      ) ++ transactions.foldLeft[UrlFormEncoded](Map.empty) {
        case (m, Other(name)) =>
          m ++ Map("specifyOtherBusiness" -> Seq(name))
        case (m, _) =>
          m
      }
  }

  implicit val jsonReads: Reads[ProfessionalBodyMember] = {
    (__ \ "isAMember").read[Boolean] flatMap {
      case true => (__ \ "businessType").read[Set[String]] flatMap { setOfStrings: Set[String] =>
        (setOfStrings map {
          case "01" => Reads(_ => JsSuccess(AccountingTechnicians)) map identity[BusinessType]
          case "02" => Reads(_ => JsSuccess(CharteredCertifiedAccountants)) map identity[BusinessType]
          case "03" => Reads(_ => JsSuccess(InternationalAccountants)) map identity[BusinessType]
          case "04" => Reads(_ => JsSuccess(TaxationTechnicians)) map identity[BusinessType]
          case "05" => Reads(_ => JsSuccess(ManagementAccountants)) map identity[BusinessType]
          case "06" => Reads(_ => JsSuccess(InstituteOfTaxation)) map identity[BusinessType]
          case "07" => Reads(_ => JsSuccess(Bookkeepers)) map identity[BusinessType]
          case "08" => Reads(_ => JsSuccess(AccountantsIreland)) map identity[BusinessType]
          case "09" => Reads(_ => JsSuccess(AccountantsScotland)) map identity[BusinessType]
          case "10" => Reads(_ => JsSuccess(AccountantsEnglandandWales)) map identity[BusinessType]
          case "11" => Reads(_ => JsSuccess(FinancialAccountants)) map identity[BusinessType]
          case "12" => Reads(_ => JsSuccess(AssociationOfBookkeepers)) map identity[BusinessType]
          case "13" => Reads(_ => JsSuccess(LawSociety)) map identity[BusinessType]
          case "14" =>
            (JsPath \ "specifyOtherBusiness").read[String].map(Other.apply) map identity[BusinessType]
          case _ =>
            Reads(_ => JsError((JsPath \ "businessType") -> play.api.data.validation.ValidationError("error.invalid")))
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
      } map ProfessionalBodyMemberYes.apply
      case false => Reads(_ => JsSuccess(ProfessionalBodyMemberNo))
    }
  }

  implicit val jsonWrites = Writes[ProfessionalBodyMember] {
    case ProfessionalBodyMemberNo => Json.obj("isAMember" -> false)
    case ProfessionalBodyMemberYes(business) =>
      Json.obj(
        "isAMember" -> true,
        "businessType" -> (business map {
          _.value
        }).toSeq
      ) ++ business.foldLeft[JsObject](Json.obj()) {
        case (m, Other(name)) =>
          m ++ Json.obj("specifyOtherBusiness" -> name)
        case (m, _) =>
          m
      }
  }
}

