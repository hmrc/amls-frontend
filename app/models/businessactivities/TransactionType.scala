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

import models.{Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json.{JsonValidationError => VE, _}
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.{CheckboxItem, Text}

sealed trait TransactionType {
  val value: String
}

case class TransactionTypes(types: Set[TransactionType])

object TransactionTypes extends Enumerable.Implicits {

  case object Paper extends WithName("paper") with TransactionType {
    override val value: String = "01"
  }

  case object DigitalSpreadsheet extends WithName("digitalSpreadsheet") with TransactionType {
    override val value: String = "02"
  }

  case object DigitalOther extends WithName("digitalOther") with TransactionType {
    override val value: String = "03"
  }

  case class DigitalSoftware(name: String) extends WithName("digitalSoftware") with TransactionType {
    override val value: String = "03"
  }

  def formValues(conditional: Html)(implicit messages: Messages): Seq[CheckboxItem] =
    Seq(
      (1, Paper, None),
      (2, DigitalSpreadsheet, None),
      (3, DigitalOther, Some(conditional))
    ).map(i =>
      CheckboxItem(
        content = Text(messages(s"businessactivities.transactiontype.lbl.${i._2.value}")),
        value = i._2.toString,
        id = Some(s"types_${i._1}"),
        name = Some(s"types[${i._1}]"),
        conditionalHtml = i._3
      )
    )

  import utils.MappingUtils.constant

  implicit val jsonReads: Reads[TransactionTypes] = new Reads[TransactionTypes] {
    override def reads(json: JsValue) = {
      val t           = (json \ "types").asOpt[Set[String]]
      val n           = (json \ "software").asOpt[String]
      val validValues = Set("01", "02", "03")

      (t, n) match {
        case (None, _)                                            => JsError(__ \ "types" -> VE("error.missing"))
        case (Some(types), None) if types.contains("03")          => JsError(__ \ "software" -> VE("error.missing"))
        case (Some(types), _) if types.diff(validValues).nonEmpty => JsError(__ \ "types" -> VE("error.invalid"))
        case (Some(types), Some(name))                            =>
          JsSuccess(
            TransactionTypes(
              types.map { `type` =>
                val obj = toType(`type`)
                if (obj == DigitalOther) DigitalSoftware(name) else obj
              }
            )
          )
        case (Some(types), None) if !types.contains("03")         => JsSuccess(TransactionTypes(types.map(toType)))
      }
    }
  }

  val oldTransactionTypeReader: Reads[Option[TransactionTypes]] =
    (__ \ "isRecorded").read[Boolean] flatMap {
      case true  =>
        (__ \ "transactions").read[Set[String]].flatMap { x: Set[String] =>
          x.map {
            case "01" => constant(Paper) map identity[TransactionType]
            case "02" => constant(DigitalSpreadsheet) map identity[TransactionType]
            case "03" =>
              (__ \ "digitalSoftwareName").read[String].map(DigitalSoftware.apply _) map identity[TransactionType]
            case _    =>
              Reads(_ => JsError((__ \ "transactions") -> play.api.libs.json.JsonValidationError("error.invalid")))
          }.foldLeft[Reads[Set[TransactionType]]](
            Reads[Set[TransactionType]](_ => JsSuccess(Set.empty))
          ) { (result, data) =>
            data flatMap { m =>
              result.map { n =>
                n + m
              }
            }
          }
        } map (t => Option(TransactionTypes(t)))
      case false => constant(None)
    }

  implicit val jsonWrites: Writes[TransactionTypes] = Writes[TransactionTypes] { t =>
    Json.obj(
      "types" -> t.types.map(_.value)
    ) ++ (t.types.collectFirst { case DigitalSoftware(name) =>
      Json.obj("software" -> name)
    } getOrElse Json.obj())
  }

  private def toType(value: String): TransactionType = value match {
    case "01" => Paper
    case "02" => DigitalSpreadsheet
    case "03" => DigitalOther
  }

  val all: Seq[TransactionType] = Seq(
    Paper,
    DigitalSpreadsheet,
    DigitalOther
  )

  implicit val enumerable: Enumerable[TransactionType] = Enumerable(all.map(v => v.toString -> v): _*)
}
