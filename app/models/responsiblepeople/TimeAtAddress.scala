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

package models.responsiblepeople

import models.{Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait TimeAtAddress {
  val value: String
}

object TimeAtAddress extends Enumerable.Implicits {

  case object Empty extends TimeAtAddress {
    override val value: String = ""
  }
  case object ZeroToFiveMonths extends WithName("zeroToFiveMonths") with TimeAtAddress {
    override val value: String = "01"
  }
  case object SixToElevenMonths extends WithName("sixToElevenMonths") with TimeAtAddress {
    override val value: String = "02"
  }
  case object OneToThreeYears extends WithName("oneToThreeYears") with TimeAtAddress {
    override val value: String = "03"
  }
  case object ThreeYearsPlus extends WithName("threeYearsPlus") with TimeAtAddress {
    override val value: String = "04"
  }

  val all: Seq[TimeAtAddress] = Seq(
    ZeroToFiveMonths,
    SixToElevenMonths,
    OneToThreeYears,
    ThreeYearsPlus
  )

  def formValues()(implicit messages: Messages): Seq[RadioItem] = all.map { item =>
    RadioItem(
      Text(messages(s"responsiblepeople.timeataddress.${item.toString}")),
      Some(item.toString),
      Some(item.toString)
    )
  }

  implicit val enumerable: Enumerable[TimeAtAddress] = Enumerable(all.map(v => v.toString -> v): _*)

  import utils.MappingUtils.Implicits._

  implicit val jsonReads: Reads[TimeAtAddress] = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "timeAtAddress").read[String].flatMap[TimeAtAddress] {
      case "01" => ZeroToFiveMonths
      case "02" => SixToElevenMonths
      case "03" => OneToThreeYears
      case "04" => ThreeYearsPlus
      case _    =>
        play.api.libs.json.JsonValidationError("error.invalid")
    }
  }

  implicit val jsonWrites: Writes[TimeAtAddress] = Writes[TimeAtAddress] {
    case Empty                        => JsNull
    case timeAtAddress: TimeAtAddress => Json.obj("timeAtAddress" -> timeAtAddress.value)
  }
}
