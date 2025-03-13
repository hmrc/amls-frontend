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

import play.api.libs.json.{Writes => _}

sealed trait ExperienceTraining

case class ExperienceTrainingYes(experienceInformation: String) extends ExperienceTraining

case object ExperienceTrainingNo extends ExperienceTraining

object ExperienceTraining {

  import play.api.libs.json._

  implicit val jsonReads: Reads[ExperienceTraining] =
    (__ \ "experienceTraining").read[Boolean] flatMap {
      case true  => (__ \ "experienceInformation").read[String] map (ExperienceTrainingYes.apply _)
      case false => Reads(_ => JsSuccess(ExperienceTrainingNo))
    }

  implicit val jsonWrites: Writes[ExperienceTraining] = Writes[ExperienceTraining] {
    case ExperienceTrainingYes(information) =>
      Json.obj(
        "experienceTraining"    -> true,
        "experienceInformation" -> information
      )
    case ExperienceTrainingNo               => Json.obj("experienceTraining" -> false)
  }

}
