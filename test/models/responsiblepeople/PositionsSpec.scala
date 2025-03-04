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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

import java.time.LocalDate

class PositionsSpec extends PlaySpec with MockitoSugar {

  "hasNominatedOfficer" must {
    "return true when there is a nominated officer RP" in {
      val positions = Positions(Set(NominatedOfficer, InternalAccountant), Some(PositionStartDate(LocalDate.now())))
      positions.isNominatedOfficer must be(true)
    }

    "return false when there is no nominated officer RP" in {
      val positions = Positions(Set(InternalAccountant), Some(PositionStartDate(LocalDate.now())))
      positions.isNominatedOfficer must be(false)
    }
  }

  "Positions" must {

    val model =
      Positions(Set(BeneficialOwner, Other("some other role")), Some(PositionStartDate(LocalDate.of(1970, 1, 1))))

    val json = Json.toJson(model)

    "write successfully to json" in {
      json mustBe Json.obj(
        "positions" -> JsArray(Seq(JsString("01"), Json.obj("other" -> "some other role"))),
        "startDate" -> "1970-01-01"
      )
    }

    "read successfully from json" in {
      Positions.jsonReads.reads(json) mustBe JsSuccess(model)
    }

    "update successfully" when {

      val positionsWithDate    = Positions(Set(InternalAccountant), Some(PositionStartDate(LocalDate.now())))
      val positionsWithoutDate = Positions(Set(InternalAccountant), None)
      val newPositions         = Set(NominatedOfficer, Director).asInstanceOf[Set[PositionWithinBusiness]]
      val newStartDate         = PositionStartDate(LocalDate.now().minusMonths(20))

      "provided with a new set of positions" in {
        val updated = Positions.update(positionsWithoutDate, newPositions)
        updated mustBe Positions(Set(NominatedOfficer, Director), None)
      }

      "provided with a new set of positions with a pre-existing date" in {
        val updated = Positions.update(positionsWithDate, newPositions)
        updated mustBe Positions(Set(NominatedOfficer, Director), positionsWithDate.startDate)
      }

      "provided with a new start date" in {
        val updated = Positions.update(positionsWithoutDate, newStartDate)
        updated mustBe Positions(Set(InternalAccountant), Some(newStartDate))
      }

      "provided with a new start date with a pre-existing date" in {
        val updated = Positions.update(positionsWithDate, newStartDate)
        updated mustBe Positions(Set(InternalAccountant), Some(newStartDate))
      }
    }
  }
}
