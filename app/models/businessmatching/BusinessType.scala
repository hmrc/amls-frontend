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

package models.businessmatching

import cats.data.Validated.{Invalid, Valid}
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import models.{Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait BusinessType {
  val value: String
}

object BusinessType extends Enumerable.Implicits {

  import jto.validation.forms.Rules._

  case object LimitedCompany extends WithName("limitedCompany") with BusinessType {
    override val value: String = "01"
  }
  case object SoleProprietor extends WithName("soleProprietor") with BusinessType {
    override val value: String = "02"
  }
  case object Partnership extends WithName("partnership") with BusinessType {
    override val value: String = "03"
  }
  case object LPrLLP extends WithName("limitedLiabilityPartnership") with BusinessType {
    override val value: String = "04"
  }
  case object UnincorporatedBody extends WithName("unincorporatedBody") with BusinessType {
    override val value: String = "05"
  }

  def errorMessageFor(businessType: BusinessType)(implicit messages: Messages): String = {
    val common = "error.required.declaration.add.position.for"

    businessType match {
      case BusinessType.LimitedCompany => Messages(s"$common.limitedcompany")
      case BusinessType.SoleProprietor => Messages(s"$common.sole.proprietor")
      case BusinessType.Partnership => Messages(s"$common.partner.ship")
      case BusinessType.LPrLLP => Messages(s"$common.lprlpp")
      case BusinessType.UnincorporatedBody => Messages(s"$common.unicorporated.body")
    }
  }

  implicit val formR: Rule[UrlFormEncoded, BusinessType] =
    From[UrlFormEncoded] { __ =>
      (__ \ "businessType").read[String] flatMap {
        case "01" => Rule(_ => Valid(LimitedCompany))
        case "02" => Rule(_ => Valid(SoleProprietor))
        case "03" => Rule(_ => Valid(Partnership))
        case "04" => Rule(_ => Valid(LPrLLP))
        case "05" => Rule(_ => Valid(UnincorporatedBody))
        case _ =>
          Rule { _ =>
            Invalid(Seq(Path \ "businessType" -> Seq(ValidationError("error.invalid"))))
          }
      }
    }

  implicit val formW: Write[BusinessType, UrlFormEncoded] =
    Write[BusinessType, UrlFormEncoded] {
      case LimitedCompany =>
        Map("businessType" -> Seq("01"))
      case SoleProprietor =>
        Map("businessType" -> Seq("02"))
      case Partnership =>
        Map("businessType" -> Seq("03"))
      case LPrLLP =>
        Map("businessType" -> Seq("04"))
      case UnincorporatedBody =>
        Map("businessType" -> Seq("05"))
    }

  implicit val writes: Writes[BusinessType] = Writes[BusinessType] {
    case LimitedCompany => JsString("Corporate Body")
    case SoleProprietor => JsString("Sole Trader")
    case Partnership => JsString("Partnership")
    case LPrLLP => JsString("LLP")
    case UnincorporatedBody => JsString("Unincorporated Body")
  }

  implicit val reads: Reads[BusinessType] = Reads[BusinessType] {
    case JsString("Corporate Body") => JsSuccess(LimitedCompany)
    case JsString("Sole Trader") => JsSuccess(SoleProprietor)
    case JsString("Partnership") => JsSuccess(Partnership)
    case JsString("LLP") => JsSuccess(LPrLLP)
    case JsString("Unincorporated Body") => JsSuccess(UnincorporatedBody)
    case _ =>
      JsError(JsPath, play.api.libs.json.JsonValidationError("error.invalid"))
  }

  val all: Seq[BusinessType] = Seq(
    LimitedCompany,
    SoleProprietor,
    Partnership,
    LPrLLP,
    UnincorporatedBody
  )

  def radioItems(implicit messages: Messages) = {
    all map { bt =>
      RadioItem(
        Text(messages(s"businessmatching.businessType.lbl.${bt.value}")),
        Some(s"businessType-${bt.value}"),
        Some(bt.toString)
      )
    }
  }

  implicit val enumerable = Enumerable(all.map(v => v.toString -> v): _*)
}
