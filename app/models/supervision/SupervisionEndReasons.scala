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

import play.api.libs.json.{Json, Reads, Writes}

case class SupervisionEndReasons(endingReason: String)

object SupervisionEndReasons {

  implicit val jsonReads: Reads[SupervisionEndReasons] = {

    import play.api.libs.json.Reads._
    import play.api.libs.json._

    (__ \ "supervisionEndingReason").read[String].map(SupervisionEndReasons.apply) map identity[SupervisionEndReasons]
  }

  implicit val jsonWrites = Writes[SupervisionEndReasons] {
    case a: SupervisionEndReasons =>
      Json.obj(
        "supervisionEndingReason" -> a.endingReason
      )
  }
}
