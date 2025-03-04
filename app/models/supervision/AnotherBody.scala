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

package models.supervision

import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.hmrcfrontend.views.config.HmrcYesNoRadioItems
import utils.MappingUtils.constant

import java.time.LocalDate

sealed trait AnotherBody

case class AnotherBodyYes(
  supervisorName: String,
  startDate: Option[SupervisionStart] = None,
  endDate: Option[SupervisionEnd] = None,
  endingReason: Option[SupervisionEndReasons] = None
) extends AnotherBody {

  def supervisorName(p: String): AnotherBodyYes =
    this.copy(supervisorName = p)

  def startDate(p: SupervisionStart): AnotherBodyYes =
    this.copy(startDate = Some(p))

  def endDate(p: SupervisionEnd): AnotherBodyYes =
    this.copy(endDate = Some(p))

  def endingReason(p: SupervisionEndReasons): AnotherBodyYes =
    this.copy(endingReason = Some(p))

  def isComplete(): Boolean = this match {
    case AnotherBodyYes(_, Some(_), Some(_), Some(_)) => true
    case _                                            => false
  }
}

case object AnotherBodyNo extends AnotherBody

object AnotherBody {

  def formValues(html: Html)(implicit messages: Messages): Seq[RadioItem] = HmrcYesNoRadioItems().map { radioItem =>
    if (radioItem.value.contains("true")) {
      radioItem.copy(
        id = Some("anotherBody-true"),
        conditionalHtml = Some(html)
      )
    } else {
      radioItem.copy(
        id = Some("anotherBody-false")
      )
    }
  }

  import play.api.libs.json._
  import utils.MappingUtils.Implicits._

  def oldStartDateReader: Reads[Option[SupervisionStart]] =
    (__ \ "startDate").readNullable[LocalDate] map { sd =>
      sd.fold[Option[SupervisionStart]](None)(e => Some(SupervisionStart(e)))
    }

  def oldEndDateReader: Reads[Option[SupervisionEnd]] =
    (__ \ "endDate").readNullable[LocalDate] map { ed =>
      ed.fold[Option[SupervisionEnd]](None)(e => Some(SupervisionEnd(e)))
    }

  def oldEndReasonsReader: Reads[Option[SupervisionEndReasons]] =
    (__ \ "endingReason").readNullable[String] map { er =>
      er.fold[Option[SupervisionEndReasons]](None)(e => Some(SupervisionEndReasons(e)))
    }

  implicit val jsonReads: Reads[AnotherBody] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._

    (__ \ "anotherBody").read[Boolean] flatMap {
      case true =>
        (
          (__ \ "supervisorName").read[String] and
            ((__ \ "startDate").read(Reads.optionNoError[SupervisionStart]) flatMap {
              case None => oldStartDateReader
              case x    => constant(x)
            }) and
            ((__ \ "endDate").read(Reads.optionNoError[SupervisionEnd]) flatMap {
              case None => oldEndDateReader
              case x    => constant(x)
            }) and
            ((__ \ "endingReason").read(Reads.optionNoError[SupervisionEndReasons]) flatMap {
              case None => oldEndReasonsReader
              case x    => constant(x)
            })
        )(AnotherBodyYes.apply _) map identity[AnotherBody]

      case false => AnotherBodyNo
    }
  }

  implicit val jsonWrites: Writes[AnotherBody] = Writes[AnotherBody] {
    case a: AnotherBodyYes =>
      Json.obj(
        "anotherBody"    -> true,
        "supervisorName" -> a.supervisorName,
        "startDate"      -> a.startDate,
        "endDate"        -> a.endDate,
        "endingReason"   -> a.endingReason
      )
    case AnotherBodyNo     => Json.obj("anotherBody" -> false)
  }

}
