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

package models.declaration.release7

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{ValidationError, _}
import jto.validation.forms.Rules.{minLength => _, _}
import jto.validation.forms.UrlFormEncoded
import models.FormTypes._
import models.businessmatching.BusinessType
import models.businessmatching.BusinessType.{SoleProprietor => BtSoleProprietor, _}
import models.{Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json.Reads.StringReads
import play.api.libs.json.{JsError, _}
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.{CheckboxItem, Text}
import utils.TraversableValidators.minLengthR

case class RoleWithinBusinessRelease7(items: Set[RoleType])

sealed trait RoleType {
  val value: String
  val formValue: String
}

case object BeneficialShareholder extends WithName("beneficialShareholder") with RoleType {
  override val value: String = "BeneficialShareholder"
  override val formValue: String = "01"
}

case object Director extends WithName("director") with RoleType {
  override val value: String = "Director"
  override val formValue: String = "02"
}

case object Partner extends WithName("partner") with RoleType {
  override val value: String = "Partner"
  override val formValue: String = "05"
}

case object InternalAccountant extends WithName("internalAccountant") with RoleType {
  override val value: String = "InternalAccountant"
  override val formValue: String = "03"
}

case object ExternalAccountant extends WithName("externalAccountant") with RoleType {
  override val value: String = "ExternalAccountant"
  override val formValue: String = "08"
}

case object SoleProprietor extends WithName("soleProprietor") with RoleType {
  override val value: String = "SoleProprietor"
  override val formValue: String = "06"
}

case object NominatedOfficer extends WithName("nominatedOfficer") with RoleType {
  override val value: String = "NominatedOfficer"
  override val formValue: String = "04"
}

case object DesignatedMember extends WithName("designatedMember") with RoleType {
  override val value: String = "DesignatedMember"
  override val formValue: String = "07"
}

case class Other(details: String) extends WithName("other") with RoleType {
  override val value: String = "Other"
  override val formValue: String = "other"
}

object RoleWithinBusinessRelease7 extends Enumerable.Implicits {

  import utils.MappingUtils.Implicits._

  val all: Seq[RoleType] = Seq(
    BeneficialShareholder,
    Director,
    Partner,
    InternalAccountant,
    ExternalAccountant,
    SoleProprietor,
    NominatedOfficer,
    DesignatedMember,
    Other("")
  )

  def formValues(businessType: Option[BusinessType], conditional: Html)(implicit messages: Messages): Seq[CheckboxItem] = {

    val constants = Seq(ExternalAccountant, NominatedOfficer)

    val rolesForBusinessType: Seq[RoleType] = businessType match {
      case Some(BtSoleProprietor) => constants :+ SoleProprietor
      case Some(Partnership) => constants :+ Partner
      case Some(LimitedCompany) if businessType.contains(LPrLLP) => Seq(BeneficialShareholder, DesignatedMember, Director) ++ constants
      case Some(LimitedCompany) => Seq(BeneficialShareholder, Director) ++ constants
      case Some(LPrLLP) => DesignatedMember +: constants
      case Some(UnincorporatedBody) => constants
      case None => Seq.empty[RoleType]
    }

    (rolesForBusinessType :+ Other("")).zipWithIndex.map { case (role, index) =>
      CheckboxItem(
        content = if(role == Other("")){
          Text(messages(s"responsiblepeople.position_within_business.lbl.09"))
        } else {
          Text(messages(s"responsiblepeople.position_within_business.lbl.${role.formValue}"))
        },
        value = role.toString,
        id = Some(s"positions_$index"),
        name = Some(s"positions[$index]"),
        conditionalHtml = if(role == Other("")) Some(conditional) else None
      )
    }
  }

  implicit val enumerable: Enumerable[RoleType] = Enumerable(all.map(v => v.toString -> v): _*)

  val maxDetailsLength = 255

  val otherDetailsType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.declaration.specify.role") andThen
    maxLength(maxDetailsLength).withMessage("error.invalid.role.in.business.maxlength.255") andThen
    basicPunctuationPattern("err.text.role.in.business.text.validation")

  implicit val formRule: Rule[UrlFormEncoded, RoleWithinBusinessRelease7] =
    From[UrlFormEncoded] { readerURLFormEncoded =>
      (readerURLFormEncoded \ "positions").read(minLengthR[Set[String]](1).withMessage("error.invalid.position.validation")) flatMap { z =>
        z.map {
          case "01" => Rule[UrlFormEncoded, RoleType](_ => Valid(BeneficialShareholder))
          case "02" => Rule[UrlFormEncoded, RoleType](_ => Valid(Director))
          case "05" => Rule[UrlFormEncoded, RoleType](_ => Valid(Partner))
          case "03" => Rule[UrlFormEncoded, RoleType](_ => Valid(InternalAccountant))
          case "08" => Rule[UrlFormEncoded, RoleType](_ => Valid(ExternalAccountant))
          case "06" => Rule[UrlFormEncoded, RoleType](_ => Valid(SoleProprietor))
          case "04" => Rule[UrlFormEncoded, RoleType](_ => Valid(NominatedOfficer))
          case "07" => Rule[UrlFormEncoded, RoleType](_ => Valid(DesignatedMember))
          case "other" =>
            (readerURLFormEncoded \ "otherPosition").read(otherDetailsType) map Other.apply
          case _ =>
            Rule[UrlFormEncoded, RoleType] { _ =>
              Invalid(Seq((Path \ "positions") -> Seq(ValidationError("error.invalid"))))
            }
        }.foldLeft[Rule[UrlFormEncoded, Set[RoleType]]](
          Rule[UrlFormEncoded, Set[RoleType]](_ => Valid(Set.empty))
        ) {
          case (m, n) =>
            n flatMap { x =>
              m map {
                _ + x
              }
            }
        } map RoleWithinBusinessRelease7.apply
      }
    }

  implicit def formWrites = Write[RoleWithinBusinessRelease7, UrlFormEncoded] {
    case RoleWithinBusinessRelease7(transactions) =>
      Map(
        "positions[]" -> (transactions map {
          _.formValue
        }).toSeq
      ) ++ transactions.foldLeft[UrlFormEncoded](Map.empty) {
        case (m, Other(name)) =>
          m ++ Map("otherPosition" -> Seq(name))
        case (m, _) =>
          m
      }
  }

  val businessRolePathName = "roleWithinBusiness"
  val businessRolePath = JsPath \ businessRolePathName

  val preRelease7JsonRead = businessRolePath.read[String].flatMap[Set[RoleType]] {
    case "01" => Reads(_ => JsSuccess(Set(BeneficialShareholder)))
    case "02" => Reads(_ => JsSuccess(Set(Director)))
    case "03" => Reads(_ => JsSuccess(Set(ExternalAccountant)))
    case "04" => Reads(_ => JsSuccess(Set(InternalAccountant)))
    case "05" => Reads(_ => JsSuccess(Set(NominatedOfficer)))
    case "06" => Reads(_ => JsSuccess(Set(Partner)))
    case "07" => Reads(_ => JsSuccess(Set(SoleProprietor)))
    case "08" => (JsPath \ "roleWithinBusinessOther").read[String] map { x =>
      Set(Other(x))
    }
    case _ => play.api.libs.json.JsonValidationError("error.invalid")
  }

  val fallback = Reads(x => (x \ businessRolePathName).getOrElse(JsNull) match {
    case JsNull => JsError(businessRolePath -> JsonValidationError("error.path.missing"))
    case _ => JsError(businessRolePath -> JsonValidationError("error.invalid"))
  }) map identity[Set[RoleType]]

  implicit val jsonReads: Reads[RoleWithinBusinessRelease7] =
    (__ \ "roleWithinBusiness").read[Set[String]].flatMap { x: Set[String] =>
      x.map {
        case "BeneficialShareholder" => Reads(_ => JsSuccess(BeneficialShareholder)) map identity[RoleType]
        case "Director" => Reads(_ => JsSuccess(Director)) map identity[RoleType]
        case "Partner" => Reads(_ => JsSuccess(Partner)) map identity[RoleType]
        case "InternalAccountant" => Reads(_ => JsSuccess(InternalAccountant)) map identity[RoleType]
        case "ExternalAccountant" => Reads(_ => JsSuccess(ExternalAccountant)) map identity[RoleType]
        case "SoleProprietor" => Reads(_ => JsSuccess(SoleProprietor)) map identity[RoleType]
        case "NominatedOfficer" => Reads(_ => JsSuccess(NominatedOfficer)) map identity[RoleType]
        case "DesignatedMember" => Reads(_ => JsSuccess(DesignatedMember)) map identity[RoleType]
        case "Other" =>
          (JsPath \ "roleWithinBusinessOther").read[String].map(Other.apply) map identity[RoleType]
        case _ =>
          Reads(_ => JsError(businessRolePath -> JsonValidationError("error.invalid")))
      }.foldLeft[Reads[Set[RoleType]]](
        Reads[Set[RoleType]](_ => JsSuccess(Set.empty))
      ) {
        (result, data) =>
          data flatMap { m =>
            result.map { n =>
              n + m
            }
          }
      }
    }.orElse(preRelease7JsonRead).orElse(fallback)
      .map(RoleWithinBusinessRelease7.apply)

  implicit val jsonWrite = Writes[RoleWithinBusinessRelease7] {
    case RoleWithinBusinessRelease7(roleTypes) =>
      Json.obj(
        businessRolePathName -> (roleTypes map {
          _.value
        }).toSeq
      ) ++ roleTypes.foldLeft[JsObject](Json.obj()) {
        case (m, Other(name)) =>
          m ++ Json.obj("roleWithinBusinessOther" -> name)
        case (m, _) =>
          m
      }
  }
}
