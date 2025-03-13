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

package models.businessdetails

import play.api.libs.json._

sealed trait PreviouslyRegistered

case class PreviouslyRegisteredYes(value: Option[String]) extends PreviouslyRegistered
case object PreviouslyRegisteredNo extends PreviouslyRegistered

object PreviouslyRegistered {

  implicit val jsonReads: Reads[PreviouslyRegistered] =
    (__ \ "previouslyRegistered").read[Boolean] flatMap {
      case true  => (__ \ "prevMLRRegNo").readNullable[String].map(PreviouslyRegisteredYes)
      case false => Reads(_ => JsSuccess(PreviouslyRegisteredNo))
    }

  implicit val jsonWrites: Writes[PreviouslyRegistered] = Writes[PreviouslyRegistered] {
    case PreviouslyRegisteredYes(Some(value)) =>
      Json.obj(
        "previouslyRegistered" -> true,
        "prevMLRRegNo"         -> value
      )
    case PreviouslyRegisteredYes(None)        =>
      Json.obj(
        "previouslyRegistered" -> true
      )
    case PreviouslyRegisteredNo               => Json.obj("previouslyRegistered" -> false)
  }
}
