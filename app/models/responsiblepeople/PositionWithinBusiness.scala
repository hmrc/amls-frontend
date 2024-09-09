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

package models.responsiblepeople

import models.businessmatching.BusinessType
import models.businessmatching.BusinessType.{SoleProprietor => BTSoleProprietor, _}
import models.{Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json._
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.CheckboxItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

sealed trait PositionWithinBusiness {
  val value: String

  def index: String = value.substring(1)
}

case object BeneficialOwner extends WithName("beneficialOwner") with PositionWithinBusiness {
  override val value: String = "01"
}

case object Director extends WithName("director") with PositionWithinBusiness {
  override val value: String = "02"
}

case object InternalAccountant extends WithName("internalAccountant") with PositionWithinBusiness {
  override val value: String = "03"
}

case object NominatedOfficer extends WithName("nominatedOfficer") with PositionWithinBusiness {
  override val value: String = "04"
}

case object Partner extends WithName("partner") with PositionWithinBusiness {
  override val value: String = "05"
}

case object SoleProprietor extends WithName("soleProprietor") with PositionWithinBusiness {
  override val value: String = "06"
}

case object DesignatedMember extends WithName("designatedMember") with PositionWithinBusiness {
  override val value: String = "07"
}

case object ExternalAccountant extends WithName("externalAccountant") with PositionWithinBusiness {
  override val value: String = "08"
}

case class Other(override val value: String) extends WithName("other") with PositionWithinBusiness {
  override def index: String = "other"
}

object PositionWithinBusiness extends Enumerable.Implicits {

  val all: Seq[PositionWithinBusiness] = Seq(
    BeneficialOwner,
    Director,
    InternalAccountant,
    NominatedOfficer,
    Partner,
    SoleProprietor,
    DesignatedMember,
    ExternalAccountant,
    Other("")
  )

  def formValues(html: Html,
                 businessType: BusinessType,
                 displayNominatedOfficer: Boolean,
                 isDeclaration: Boolean
  )(implicit messages: Messages): Seq[CheckboxItem] = {

    val optionsList = buildOptionsList(businessType, isDeclaration, displayNominatedOfficer)

    optionsList.zipWithIndex.map { case (position, index) =>
      val conditional = if (position.toString == Other("").toString) Some(html) else None

      if(businessType == LimitedCompany && isDeclaration && index == 0) {
        CheckboxItem(
          content = Text(messages("declaration.addperson.lbl.01")),
          value = BeneficialOwner.toString,
          id = Some(s"positions_${BeneficialOwner.index}"),
          name = Some(s"positions[${BeneficialOwner.index}]")
        )
      } else if(position == Other("")) {
        CheckboxItem(
          content = Text(messages("responsiblepeople.position_within_business.lbl.09")),
          value = position.toString,
          id = Some(s"positions_${BeneficialOwner.index}"),
          name = Some(s"positions[${BeneficialOwner.index}]"),
          conditionalHtml = conditional
        )
      } else {
        CheckboxItem(
          content = Text(getPrettyName(position)),
          value = position.toString,
          id = Some(s"positions_${position.index}"),
          name = Some(s"positions[${position.index}]"),
          conditionalHtml = conditional
        )
      }
    }
  }

  def buildOptionsList(businessType: BusinessType, isDeclaration: Boolean, displayNominatedOfficer: Boolean): Seq[PositionWithinBusiness] = {

    val optionalCheckboxes = Seq(
      if (isDeclaration) Some(ExternalAccountant) else None,
      if (displayNominatedOfficer) Some(NominatedOfficer) else None
    ).flatten

    (businessType match {
      case BTSoleProprietor => optionalCheckboxes :+ SoleProprietor
      case Partnership => optionalCheckboxes :+ Partner
      case LimitedCompany if businessType == LPrLLP =>
        Seq(BeneficialOwner, DesignatedMember, Director) ++ optionalCheckboxes
      case LimitedCompany =>
        Seq(BeneficialOwner, Director) ++ optionalCheckboxes
      case LPrLLP => DesignatedMember +: optionalCheckboxes
      case UnincorporatedBody => optionalCheckboxes
    }) :+ Other("")
  }

  implicit val enumerable: Enumerable[PositionWithinBusiness] = Enumerable(all.map(v => v.toString -> v): _*)

  def getPrettyName(position:PositionWithinBusiness)(implicit message: Messages): String = {
    import play.api.i18n.Messages

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

  private[responsiblepeople] implicit val jsonWrites: Writes[PositionWithinBusiness] = Writes[PositionWithinBusiness] {
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
}
