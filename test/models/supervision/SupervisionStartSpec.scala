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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

import java.time.LocalDate

class SupervisionStartSpec extends PlaySpec with MockitoSugar {
  trait Fixture {

    val endDateField = Map("extraEndDate" -> Seq("2000-01-01"))
    val start        = LocalDate.of(1990, 2, 24) // scalastyle:off magic.number
  }

  "Json read and writes" must {

    "Serialise SupervisionStart as expected" in new Fixture {

      val input = SupervisionStart(start)

      val expectedJson = Json.obj("supervisionStartDate" -> "1990-02-24")

      Json.toJson(input) must be(expectedJson)
    }

    "Deserialise SupervisionStart as expected" in new Fixture {

      val input = Json.obj("anotherBody" -> true, "supervisionStartDate" -> "1990-02-24")

      val expected = SupervisionStart(start)

      Json.fromJson[SupervisionStart](input) must be(JsSuccess(expected, JsPath \ "supervisionStartDate"))
    }

    "fail when data is missing" in {
      Json.fromJson[SupervisionStart](Json.obj()) must
        be(JsError((JsPath \ "supervisionStartDate") -> play.api.libs.json.JsonValidationError("error.path.missing")))
    }
  }
}
