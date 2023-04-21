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

package models.businessactivities

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import cats.data.Validated.Valid
import play.api.i18n.Messages
import play.api.libs.json._
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.RadioItem
import uk.gov.hmrc.hmrcfrontend.views.config.HmrcYesNoRadioItems

sealed trait BusinessFranchise

case class BusinessFranchiseYes(value: String) extends BusinessFranchise

case object BusinessFranchiseNo extends BusinessFranchise

object BusinessFranchise {

  def formValues(html: Html)(implicit messages: Messages): Seq[RadioItem] = HmrcYesNoRadioItems().map { radioItem =>

    if (radioItem.value.contains("true")) {
      radioItem.copy(
        id = Some("businessFranchise-true"),
        conditionalHtml = Some(html)
      )
    } else {
      radioItem.copy(
        id = Some("businessFranchise-false")
      )
    }
  }

  import models.FormTypes._
  import utils.MappingUtils.Implicits._

  private val maxFranchiseName = 140
  private val franchiseNameType =  notEmptyStrip andThen notEmpty.withMessage("error.required.ba.franchise.name") andThen
    maxLength(maxFranchiseName).withMessage("error.max.length.ba.franchise.name") andThen regexWithMsg(basicPunctuationRegex, "error.invalid.characters.ba.franchise.name")

  implicit val formRule: Rule[UrlFormEncoded, BusinessFranchise] = From[UrlFormEncoded] { __ =>
  import jto.validation.forms.Rules._
    (__ \ "businessFranchise").read[Boolean].withMessage("error.required.ba.is.your.franchise") flatMap {
      case true =>
        (__ \ "franchiseName").read(franchiseNameType) map BusinessFranchiseYes.apply
      case false => Rule.fromMapping { _ => Valid(BusinessFranchiseNo) }
    }
  }

  implicit val formWrites: Write[BusinessFranchise, UrlFormEncoded] = Write {
    case BusinessFranchiseYes(value) =>
      Map("businessFranchise" -> Seq("true"),
          "franchiseName" -> Seq(value)
      )
    case BusinessFranchiseNo => Map("businessFranchise" -> Seq("false"))
  }

  implicit val jsonReads: Reads[BusinessFranchise] =
    (__ \ "businessFranchise").read[Boolean] flatMap {
      case true => (__ \ "franchiseName").read[String] map BusinessFranchiseYes.apply
      case false => Reads(_ => JsSuccess(BusinessFranchiseNo))
    }

  implicit val jsonWrites = Writes[BusinessFranchise] {
    case BusinessFranchiseYes(value) => Json.obj(
      "businessFranchise" -> true,
      "franchiseName" -> value
    )
    case BusinessFranchiseNo => Json.obj("businessFranchise" -> false)
  }

}

