/*
 * Copyright 2017 HM Revenue & Customs
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

import jto.validation.{Write, Path, From, Rule}
import jto.validation.forms._
import jto.validation.ValidationError
import play.api.i18n.{Messages, Lang}
import play.api.libs.json.Writes
import play.api.libs.json._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

sealed trait TaxType {
  def message(implicit lang: Lang): String =
    this match {
      case TaxTypeSelfAssesment =>
        Messages("tradingpremises.youragent.taxtype.lbl.01")
      case TaxTypeCorporationTax =>
        Messages("tradingpremises.youragent.taxtype.lbl.02")
    }
}

case object TaxTypeSelfAssesment extends TaxType
case object TaxTypeCorporationTax extends TaxType

object TaxType {

  import utils.MappingUtils.Implicits._

  implicit val jsonReadsTaxType = {
    (__ \ "taxType").read[String].flatMap[TaxType] {
      case "01" => TaxTypeSelfAssesment
      case "02" => TaxTypeCorporationTax
      case _ => play.api.data.validation.ValidationError("error.invalid")
    }
  }

  implicit val jsonWritesTaxType = Writes[TaxType] {
    case TaxTypeSelfAssesment => Json.obj("taxType" -> "01")
    case TaxTypeCorporationTax => Json.obj("taxType" -> "02")
  }

  implicit val taxTypeRule: Rule[UrlFormEncoded, TaxType] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "taxType").read[String] flatMap {
      case "01" => TaxTypeSelfAssesment
      case "02" => TaxTypeCorporationTax
      case _ =>
        (Path \ "taxType") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWritesTaxType: Write[TaxType, UrlFormEncoded] = Write {
    case TaxTypeSelfAssesment =>
      Map("taxType" -> Seq("01"))
    case TaxTypeCorporationTax =>
      Map("taxType" -> Seq("02"))
    case _ => Map("" -> Seq(""))
  }
}
