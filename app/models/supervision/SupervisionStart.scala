/*
 * Copyright 2019 HM Revenue & Customs
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
import models.FormTypes.{localDateFutureRule, _}
import org.joda.time.LocalDate
import play.api.libs.json.{Json, Reads, Writes}

case class SupervisionStart(startDate: LocalDate)

object SupervisionStart {
  implicit val formRule: Rule[UrlFormEncoded, SupervisionStart] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    (__ \ "startDate").read(localDateFutureRule) map SupervisionStart.apply
  }

  implicit val formWrites: Write[SupervisionStart, UrlFormEncoded] = Write {
    case a: SupervisionStart => {
            Map(
              "anotherBody" -> Seq("true")
            ) ++ (
        localDateWrite.writes(a.startDate) map {
          case (key, value) =>
            s"startDate.$key" -> value
        })
    }
  }


  implicit val jsonReads: Reads[SupervisionStart] = {

    import play.api.libs.json.Reads._
    import play.api.libs.json._

    (__ \ "startDate").read[LocalDate].map(SupervisionStart.apply) map identity[SupervisionStart]
  }

  implicit val jsonWrites = Writes[SupervisionStart] {
    case a : SupervisionStart =>
      Json.obj(
       "startDate" -> a.startDate
    )
  }
}
