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

package models.moneyservicebusiness

import play.api.libs.json._
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.hmrcfrontend.views.config.HmrcYesNoRadioItems

sealed trait BusinessUseAnIPSP

case class BusinessUseAnIPSPYes(name: String, reference: String) extends BusinessUseAnIPSP

case object BusinessUseAnIPSPNo extends BusinessUseAnIPSP

object BusinessUseAnIPSP {

  def formValues(conditionalHtml: Html*)(implicit messages: Messages): Seq[RadioItem] =
    HmrcYesNoRadioItems().map { input =>
      if (input.value.contains("true")) {
        input.copy(conditionalHtml = Some(Html(conditionalHtml.mkString)))
      } else {
        input
      }
    }

  implicit val jsonReads: Reads[BusinessUseAnIPSP] = {
    import play.api.libs.functional.syntax._
    (__ \ "useAnIPSP").read[Boolean] flatMap {
      case true  =>
        ((__ \ "name").read[String] and
          (__ \ "referenceNumber").read[String])(BusinessUseAnIPSPYes.apply _)
      case false => Reads(_ => JsSuccess(BusinessUseAnIPSPNo))
    }
  }

  implicit val jsonWrites: Writes[BusinessUseAnIPSP] = Writes[BusinessUseAnIPSP] {
    case BusinessUseAnIPSPYes(name, referenceNumber) =>
      Json.obj(
        "useAnIPSP"       -> true,
        "name"            -> name,
        "referenceNumber" -> referenceNumber
      )
    case BusinessUseAnIPSPNo                         => Json.obj("useAnIPSP" -> false)
  }

}
