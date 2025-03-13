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

package models.tradingpremises

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

import java.time.LocalDate

class ActivityEndDateSpec extends PlaySpec {

  val startDateField = Map("premisesStartDate" -> Seq("1989-01-01"))

  "ActivityEndDate" must {
    "Json" should {

      "Read and write successfully" in {
        ActivityEndDate.format.reads(ActivityEndDate.format.writes(ActivityEndDate(LocalDate.of(1990, 2, 24)))) must be(
          JsSuccess(ActivityEndDate(LocalDate.of(1990, 2, 24)), JsPath)
        )

      }

      "write successfully" in {
        ActivityEndDate.format.writes(ActivityEndDate(LocalDate.of(1990, 2, 24))) must be(
          Json.obj("endDate" -> "1990-02-24")
        )
      }
    }
  }
}
