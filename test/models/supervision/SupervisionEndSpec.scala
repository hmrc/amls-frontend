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

class SupervisionEndSpec extends PlaySpec with MockitoSugar {
  trait Fixture {

    val startDateField = Map("extraStartDate" -> Seq("2000-01-01"))
    val end            = LocalDate.of(2005, 2, 24) // scalastyle:off magic.number
  }

  "Json read and writes" must {

    "Serialise SupervisionEnd as expected" in new Fixture {

      val input = SupervisionEnd(end)

      val expectedJson = Json.obj("supervisionEndDate" -> "2005-02-24")

      Json.toJson(input) must be(expectedJson)
    }

    "Deserialise SupervisionEnd as expected" in new Fixture {

      val input = Json.obj("anotherBody" -> true, "supervisionEndDate" -> "2005-02-24")

      val expected = SupervisionEnd(end)

      Json.fromJson[SupervisionEnd](input) must be(JsSuccess(expected, JsPath \ "supervisionEndDate"))
    }

    "fail when data is missing" in {
      Json.fromJson[SupervisionEnd](Json.obj()) must
        be(JsError((JsPath \ "supervisionEndDate") -> play.api.libs.json.JsonValidationError("error.path.missing")))
    }
  }
}
