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

package models.businessmatching

import models.{Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait BusinessType {
  val value: String
}

object BusinessType extends Enumerable.Implicits {

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

  implicit val writes: Writes[BusinessType] = Writes[BusinessType] {
    case LimitedCompany     => JsString("Corporate Body")
    case SoleProprietor     => JsString("Sole Trader")
    case Partnership        => JsString("Partnership")
    case LPrLLP             => JsString("LLP")
    case UnincorporatedBody => JsString("Unincorporated Body")
  }

  implicit val reads: Reads[BusinessType] = Reads[BusinessType] {
    case JsString("Corporate Body")      => JsSuccess(LimitedCompany)
    case JsString("Sole Trader")         => JsSuccess(SoleProprietor)
    case JsString("Partnership")         => JsSuccess(Partnership)
    case JsString("LLP")                 => JsSuccess(LPrLLP)
    case JsString("Unincorporated Body") => JsSuccess(UnincorporatedBody)
    case _                               =>
      JsError(JsPath, play.api.libs.json.JsonValidationError("error.invalid"))
  }

  val all: Seq[BusinessType] = Seq(
    LimitedCompany,
    SoleProprietor,
    Partnership,
    LPrLLP,
    UnincorporatedBody
  )

  def radioItems(implicit messages: Messages) =
    all map { bt =>
      RadioItem(
        Text(messages(s"businessmatching.businessType.lbl.${bt.value}")),
        Some(s"businessType-${bt.value}"),
        Some(bt.toString)
      )
    }

  implicit val enumerable: Enumerable[BusinessType] = Enumerable(all.map(v => v.toString -> v): _*)
}
