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

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import models.FormTypes.{supervisionEndDateRule, localDateWrite}
import org.joda.time.LocalDate
import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.json.JodaWrites._

case class SupervisionEnd(endDate: LocalDate)

object SupervisionEnd {
  implicit val formRule: Rule[UrlFormEncoded, SupervisionEnd] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    __.read(supervisionEndDateRule) map SupervisionEnd.apply
  }

  implicit val formWrites: Write[SupervisionEnd, UrlFormEncoded] = Write {
    case a: SupervisionEnd => {
      Map(
        "anotherBody" -> Seq("true")
      ) ++ (
        localDateWrite.writes(a.endDate) map {
          case (key, value) =>
            s"endDate.$key" -> value
        })
    }
  }


  implicit val jsonReads: Reads[SupervisionEnd] = {

    import play.api.libs.json.JodaReads._
    import play.api.libs.json._

    (__ \ "supervisionEndDate").read[LocalDate].map(SupervisionEnd.apply) map identity[SupervisionEnd]
  }

  implicit val jsonWrites = Writes[SupervisionEnd] {
    case a: SupervisionEnd =>
      Json.obj(
        "supervisionEndDate" -> a.endDate
      )
  }
}
