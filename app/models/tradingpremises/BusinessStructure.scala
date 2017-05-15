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


sealed trait BusinessStructure {
  def message(implicit lang: Lang): String =
    this match {
      case SoleProprietor =>
        Messages("businessType.lbl.01")
      case LimitedLiabilityPartnership =>
        Messages("businessType.lbl.02")
      case Partnership =>
        Messages("businessType.lbl.03")
      case IncorporatedBody =>
        Messages("businessType.lbl.04")
      case UnincorporatedBody =>
        Messages("businessType.lbl.05")
    }
}

case object SoleProprietor extends BusinessStructure
case object LimitedLiabilityPartnership extends BusinessStructure
case object Partnership extends BusinessStructure
case object IncorporatedBody extends BusinessStructure
case object UnincorporatedBody extends BusinessStructure


object BusinessStructure {

  import utils.MappingUtils.Implicits._

  implicit val jsonReadsBusinessStructure = {
    (__ \ "agentsBusinessStructure").read[String].flatMap[BusinessStructure] {
      case "01" => SoleProprietor
      case "02" => LimitedLiabilityPartnership
      case "03" => Partnership
      case "04" => IncorporatedBody
      case "05" => UnincorporatedBody
      case _ =>
        play.api.data.validation.ValidationError("error.invalid")
    }
  }

  implicit val jsonWritesBusinessStructure = Writes[BusinessStructure] {
    case SoleProprietor => Json.obj("agentsBusinessStructure" -> "01")
    case LimitedLiabilityPartnership => Json.obj("agentsBusinessStructure" -> "02")
    case Partnership => Json.obj("agentsBusinessStructure" -> "03")
    case IncorporatedBody => Json.obj("agentsBusinessStructure" -> "04")
    case UnincorporatedBody => Json.obj("agentsBusinessStructure" -> "05")
  }

  implicit val agentsBusinessStructureRule: Rule[UrlFormEncoded, BusinessStructure] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    (__ \ "agentsBusinessStructure").read[String].withMessage("error.required.tp.select.business.structure") flatMap {
      case "01" => SoleProprietor
      case "02" => LimitedLiabilityPartnership
      case "03" => Partnership
      case "04" => IncorporatedBody
      case "05" => UnincorporatedBody
      case _ => (Path \ "agentsBusinessStructure") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWritesBusinessStructure: Write[BusinessStructure, UrlFormEncoded] = Write {
    case SoleProprietor =>
      Map("agentsBusinessStructure" -> Seq("01"))
    case LimitedLiabilityPartnership =>
      Map("agentsBusinessStructure" -> Seq("02"))
    case Partnership =>
      Map("agentsBusinessStructure" -> Seq("03"))
    case IncorporatedBody =>
      Map("agentsBusinessStructure" -> Seq("04"))
    case UnincorporatedBody =>
      Map("agentsBusinessStructure" -> Seq("05"))
  }
}
