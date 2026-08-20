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

import play.api.i18n.Messages
import play.api.libs.json._
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.hmrcfrontend.views.config.HmrcYesNoRadioItems

sealed trait BusinessAppliedForFrn
case class BusinessAppliedForFrnYes(regNumber: String) extends BusinessAppliedForFrn
case object BusinessAppliedForFrnNo extends BusinessAppliedForFrn

object BusinessAppliedForFrn {

  def formValues(html: Html)(implicit messages: Messages): Seq[RadioItem] = HmrcYesNoRadioItems().map { radioItem =>
    if (radioItem.value.contains("true")) {
      radioItem.copy(
        id = Some("appliedFor-true"),
        conditionalHtml = Some(html)
      )
    } else {
      radioItem.copy(
        id = Some("appliedFor-false")
      )
    }
  }

  implicit val jsonReads: Reads[BusinessAppliedForFrn] =
    (__ \ "appliedFor").read[Boolean] flatMap {
      case true  => (__ \ "regNumber").read[String] map BusinessAppliedForFrnYes.apply
      case false => Reads(_ => JsSuccess(BusinessAppliedForFrnNo))
    }

  implicit val jsonWrites: Writes[BusinessAppliedForFrn] = Writes[BusinessAppliedForFrn] {
    case BusinessAppliedForFrnYes(value) =>
      Json.obj(
        "appliedFor" -> true,
        "regNumber"  -> value
      )
    case BusinessAppliedForFrnNo         => Json.obj("appliedFor" -> false)
  }

}
