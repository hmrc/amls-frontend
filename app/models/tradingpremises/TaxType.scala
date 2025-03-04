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

package models.tradingpremises

import play.api.i18n.Messages
import play.api.libs.json.Writes
import play.api.libs.json._

sealed trait TaxType {
  def message(implicit messages: Messages): String =
    this match {
      case TaxTypeSelfAssesment  =>
        messages("tradingpremises.youragent.taxtype.lbl.01")
      case TaxTypeCorporationTax =>
        messages("tradingpremises.youragent.taxtype.lbl.02")
    }
}

case object TaxTypeSelfAssesment extends TaxType
case object TaxTypeCorporationTax extends TaxType

object TaxType {

  import utils.MappingUtils.Implicits._

  implicit val jsonReadsTaxType: Reads[TaxType] =
    (__ \ "taxType").read[String].flatMap[TaxType] {
      case "01" => TaxTypeSelfAssesment
      case "02" => TaxTypeCorporationTax
      case _    => play.api.libs.json.JsonValidationError("error.invalid")
    }

  implicit val jsonWritesTaxType: Writes[TaxType] = Writes[TaxType] {
    case TaxTypeSelfAssesment  => Json.obj("taxType" -> "01")
    case TaxTypeCorporationTax => Json.obj("taxType" -> "02")
  }
}
