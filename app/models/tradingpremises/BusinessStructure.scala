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

import models.{Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json.Writes
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait BusinessStructure {

  import models.tradingpremises.BusinessStructure._

  val value: String
  def message(implicit messages: Messages): String =
    this match {
      case SoleProprietor              =>
        messages("businessType.lbl.01")
      case LimitedLiabilityPartnership =>
        messages("businessType.lbl.02")
      case Partnership                 =>
        messages("businessType.lbl.03")
      case IncorporatedBody            =>
        messages("businessType.lbl.04")
      case UnincorporatedBody          =>
        messages("businessType.lbl.05")
    }
}

object BusinessStructure extends Enumerable.Implicits {

  case object SoleProprietor extends WithName("soleProprietor") with BusinessStructure {
    override val value: String = "01"
  }

  case object LimitedLiabilityPartnership extends WithName("limitedLiabilityPartnership") with BusinessStructure {
    override val value: String = "02"
  }

  case object Partnership extends WithName("partnership") with BusinessStructure {
    override val value: String = "03"
  }

  case object IncorporatedBody extends WithName("incorporatedBody") with BusinessStructure {
    override val value: String = "04"
  }

  case object UnincorporatedBody extends WithName("unincorporatedBody") with BusinessStructure {
    override val value: String = "05"
  }

  import utils.MappingUtils.Implicits._

  implicit val jsonReadsBusinessStructure: Reads[BusinessStructure] =
    (__ \ "agentsBusinessStructure").read[String].flatMap[BusinessStructure] {
      case "01" => SoleProprietor
      case "02" => LimitedLiabilityPartnership
      case "03" => Partnership
      case "04" => IncorporatedBody
      case "05" => UnincorporatedBody
      case _    =>
        play.api.libs.json.JsonValidationError("error.invalid")
    }

  implicit val jsonWritesBusinessStructure: Writes[BusinessStructure] = Writes[BusinessStructure] {
    case SoleProprietor              => Json.obj("agentsBusinessStructure" -> "01")
    case LimitedLiabilityPartnership => Json.obj("agentsBusinessStructure" -> "02")
    case Partnership                 => Json.obj("agentsBusinessStructure" -> "03")
    case IncorporatedBody            => Json.obj("agentsBusinessStructure" -> "04")
    case UnincorporatedBody          => Json.obj("agentsBusinessStructure" -> "05")
  }

  def formValues()(implicit messages: Messages): Seq[RadioItem] = all.sortBy(_.toString).map { structure =>
    RadioItem(
      content = Text(messages(s"businessType.lbl.${structure.value}")),
      id = Some(structure.toString),
      value = Some(structure.toString)
    )
  }

  val all: Seq[BusinessStructure] = Seq(
    SoleProprietor,
    LimitedLiabilityPartnership,
    Partnership,
    IncorporatedBody,
    UnincorporatedBody
  )

  implicit val enumerable: Enumerable[BusinessStructure] = Enumerable(all.map(v => v.toString -> v): _*)
}
