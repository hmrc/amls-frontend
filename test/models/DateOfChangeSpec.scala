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

package models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

import java.time.LocalDate

class DateOfChangeSpec extends PlaySpec {
  "DateOfChange" must {

    "read from JSON correctly" in {

      val json = JsString("2016-02-24")

      val result = Json.fromJson[DateOfChange](json)
      result.get.dateOfChange must be(LocalDate.of(2016, 2, 24))
    }

    "write to JSON correctly" in {

      val date = DateOfChange(LocalDate.of(2016, 2, 24))
      val json = JsString("2016-02-24")

      val result = Json.toJson(date)
      result must be(json)
    }
  }
}
