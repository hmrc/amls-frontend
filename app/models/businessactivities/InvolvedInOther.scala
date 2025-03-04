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

package models.businessactivities

import play.api.libs.json._
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.RadioItem
import uk.gov.hmrc.hmrcfrontend.views.config.HmrcYesNoRadioItems

sealed trait InvolvedInOther

case class InvolvedInOtherYes(details: String) extends InvolvedInOther

case object InvolvedInOtherNo extends InvolvedInOther

object InvolvedInOther {

  def formValues(html: Html)(implicit messages: Messages): Seq[RadioItem] = HmrcYesNoRadioItems().map { radioItem =>
    if (radioItem.value.contains("true")) {
      radioItem.copy(
        id = Some("involvedInOther-true"),
        conditionalHtml = Some(html)
      )
    } else {
      radioItem.copy(
        id = Some("involvedInOther-false")
      )
    }
  }

  implicit val jsonReads: Reads[InvolvedInOther] =
    (__ \ "involvedInOther").read[Boolean] flatMap {
      case true  => (__ \ "details").read[String] map InvolvedInOtherYes.apply
      case false => Reads(_ => JsSuccess(InvolvedInOtherNo))
    }

  implicit val jsonWrites: Writes[InvolvedInOther] = Writes[InvolvedInOther] {
    case InvolvedInOtherYes(details) =>
      Json.obj(
        "involvedInOther" -> true,
        "details"         -> details
      )
    case involvedInOtherNo           => Json.obj("involvedInOther" -> false)
  }

  implicit def convert(model: InvolvedInOther): models.renewal.InvolvedInOther = model match {
    case InvolvedInOtherYes(details) => models.renewal.InvolvedInOtherYes(details)
    case InvolvedInOtherNo           => models.renewal.InvolvedInOtherNo
  }

}
