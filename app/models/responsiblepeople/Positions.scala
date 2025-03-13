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

import play.api.libs.json._

import java.time.LocalDate

case class Positions(positions: Set[PositionWithinBusiness], startDate: Option[PositionStartDate]) {
  def isNominatedOfficer: Boolean = positions.contains(NominatedOfficer)
}

object Positions {

  implicit val jsonWrites: Writes[Positions] =
    Writes[Positions] {
      case Positions(positions, None)                               =>
        Json.obj("positions" -> positions.map(p => PositionWithinBusiness.jsonWrites.writes(p)))
      case Positions(positions, Some(PositionStartDate(firstDate))) =>
        Json.obj(
          "positions" -> positions.map(p => PositionWithinBusiness.jsonWrites.writes(p)),
          "startDate" -> firstDate.toString
        )
    }

  implicit val jsonReads: Reads[Positions] = {
    import play.api.libs.functional.syntax._
    ((__ \ "positions").read[Set[PositionWithinBusiness]] and
      (__ \ "startDate").readNullable[LocalDate].map {
        case Some(date) => Some(PositionStartDate.apply(date))
        case None       => None
      })(Positions.apply _)
  }

  def update(positions: Positions, positionSet: Set[PositionWithinBusiness]): Positions =
    positions match {
      case Positions(_, Some(date)) => Positions(positionSet, Some(date))
      case Positions(_, None)       => Positions(positionSet, None)
    }

  def update(positions: Positions, startDate: PositionStartDate): Positions =
    Positions(positions.positions, Some(startDate))
}
