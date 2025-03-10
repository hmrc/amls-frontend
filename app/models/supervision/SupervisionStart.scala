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

import play.api.libs.json._

import java.time.LocalDate

case class SupervisionStart(startDate: LocalDate)

object SupervisionStart {

  implicit val jsonReads: Reads[SupervisionStart] =
    (__ \ "supervisionStartDate").read[LocalDate].map(SupervisionStart.apply)

  implicit val jsonWrites: Writes[SupervisionStart] = Writes[SupervisionStart] { case a: SupervisionStart =>
    Json.obj("supervisionStartDate" -> a.startDate)
  }
}
