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

package models.tcsp

import play.api.i18n.Messages
import play.api.libs.json._
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.RadioItem
import uk.gov.hmrc.hmrcfrontend.views.config.HmrcYesNoRadioItems

sealed trait ServicesOfAnotherTCSP

case class ServicesOfAnotherTCSPYes(mlrRefNumber: Option[String]) extends ServicesOfAnotherTCSP

case object ServicesOfAnotherTCSPNo extends ServicesOfAnotherTCSP

object ServicesOfAnotherTCSP {

  def formValues(html: Html)(implicit messages: Messages): Seq[RadioItem] = HmrcYesNoRadioItems().map { radioItem =>

    if (radioItem.value.contains("true")) {
      radioItem.copy(
        id = Some("servicesOfAnotherTCSP-true"),
        conditionalHtml = Some(html)
      )
    } else {
      radioItem.copy(
        id = Some("servicesOfAnotherTCSP-false")
      )
    }
  }

  implicit val jsonReads: Reads[ServicesOfAnotherTCSP] =
    (__ \ "servicesOfAnotherTCSP").read[Boolean] flatMap {
      case true => (__ \ "mlrRefNumber").readNullable[String] map ServicesOfAnotherTCSPYes.apply
      case false => Reads(__ => JsSuccess(ServicesOfAnotherTCSPNo))
    }

  implicit val jsonWrites = Writes[ServicesOfAnotherTCSP] {
    case ServicesOfAnotherTCSPYes(value) => Json.obj(
          "servicesOfAnotherTCSP" -> true,
          "mlrRefNumber" -> value
    )
    case ServicesOfAnotherTCSPNo => Json.obj("servicesOfAnotherTCSP" -> false)
  }
}
