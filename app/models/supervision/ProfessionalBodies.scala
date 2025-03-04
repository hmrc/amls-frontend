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

package models.supervision

import models.{Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json.Reads.StringReads
import play.api.libs.json._
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem

sealed trait BusinessType {
  val value: String

  import models.supervision.ProfessionalBodies._

  def getMessage()(implicit messages: Messages): String = {
    val message = s"supervision.memberofprofessionalbody.lbl."
    this match {
      case AccountantsEnglandandWales =>
        messages(s"$message${this.value}")
          .replace("Accountants of England", "Accountants in England")
      case Other("")                  => messages(s"${message}14")
      case Other(details)             => details
      case _                          => messages(s"$message${this.value}")
    }
  }
}

case class ProfessionalBodies(businessTypes: Set[BusinessType])

object ProfessionalBodies extends Enumerable.Implicits {

  case object AccountingTechnicians extends WithName("accountingTechnicians") with BusinessType {
    override val value: String = "01"
  }

  case object CharteredCertifiedAccountants extends WithName("charteredCertifiedAccountants") with BusinessType {
    override val value: String = "02"
  }

  case object InternationalAccountants extends WithName("internationalAccountants") with BusinessType {
    override val value: String = "03"
  }

  case object TaxationTechnicians extends WithName("taxationTechnicians") with BusinessType {
    override val value: String = "04"
  }

  case object ManagementAccountants extends WithName("managementAccountants") with BusinessType {
    override val value: String = "05"
  }

  case object InstituteOfTaxation extends WithName("instituteOfTaxation") with BusinessType {
    override val value: String = "06"
  }

  case object Bookkeepers extends WithName("bookkeepers") with BusinessType {
    override val value: String = "07"
  }

  case object AccountantsIreland extends WithName("accountantsIreland") with BusinessType {
    override val value: String = "08"
  }

  case object AccountantsScotland extends WithName("accountantsScotland") with BusinessType {
    override val value: String = "09"
  }

  case object AccountantsEnglandandWales extends WithName("accountantsEnglandandWales") with BusinessType {
    override val value: String = "10"
  }

  case object FinancialAccountants extends WithName("financialAccountants") with BusinessType {
    override val value: String = "11"
  }

  case object AssociationOfBookkeepers extends WithName("associationOfBookkeepers") with BusinessType {
    override val value: String = "12"
  }

  case object LawSociety extends WithName("lawSociety") with BusinessType {
    override val value: String = "13"
  }

  case class Other(businessDetails: String) extends WithName("other") with BusinessType {
    override val value: String = "14"
  }

  val all: Seq[BusinessType] = Seq(
    AccountingTechnicians,
    CharteredCertifiedAccountants,
    InternationalAccountants,
    TaxationTechnicians,
    ManagementAccountants,
    InstituteOfTaxation,
    Bookkeepers,
    AccountantsEnglandandWales,
    AccountantsIreland,
    AccountantsScotland,
    FinancialAccountants,
    AssociationOfBookkeepers,
    LawSociety,
    Other("")
  )

  def formValues(html: Html)(implicit messages: Messages): Seq[CheckboxItem] = all.zipWithIndex.map {
    case (businessType, index) =>
      val conditional = if (businessType.value == Other("").value) Some(html) else None

      CheckboxItem(
        content = Text(businessType.getMessage()),
        value = businessType.toString,
        id = Some(s"businessType_$index"),
        name = Some(s"businessType[$index]"),
        conditionalHtml = conditional
      )
  }

  implicit val enumerable: Enumerable[BusinessType] = Enumerable(all.map(v => v.toString -> v): _*)

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
      case _    =>
        Reads(_ => JsError((JsPath \ "businessType") -> play.api.libs.json.JsonValidationError("error.invalid")))
    }).foldLeft[Reads[Set[BusinessType]]](
      Reads[Set[BusinessType]](_ => JsSuccess(Set.empty))
    ) { (start, businessTypeReader) =>
      businessTypeReader flatMap { businessType =>
        start map { businessTypes =>
          businessTypes + businessType
        }
      }
    }
  } map ProfessionalBodies.apply

  implicit val jsonWrites: Writes[ProfessionalBodies] = Writes[ProfessionalBodies] { businessTypes =>
    Json.obj(
      "businessType" -> (businessTypes.businessTypes map (_.value)).toSeq
    ) ++ businessTypes.businessTypes.foldLeft[JsObject](Json.obj()) {
      case (json, Other(name)) => json ++ Json.obj("specifyOtherBusiness" -> name)
      case (json, _)           => json
    }
  }

}
