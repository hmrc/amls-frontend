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

class ActivityStartDateSpec extends PlaySpec {
  // scalastyle:off

  "Json validation" must {

    "Read and write successfully" in {

      ActivityStartDate.format.reads(
        ActivityStartDate.format.writes(ActivityStartDate(LocalDate.of(1990, 2, 24)))
      ) must be(JsSuccess(ActivityStartDate(LocalDate.of(1990, 2, 24)), JsPath))
    }

    "write successfully" in {
      ActivityStartDate.format.writes(ActivityStartDate(LocalDate.of(1990, 2, 24))) must be(
        Json.obj("startDate" -> "1990-02-24")
      )
    }
  }
}
