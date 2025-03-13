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

class AnotherBodySpec extends PlaySpec with MockitoSugar {

  trait Fixture {

    val start  = Some(SupervisionStart(LocalDate.of(1990, 2, 24))) // scalastyle:off magic.number
    val end    = Some(SupervisionEnd(LocalDate.of(1998, 2, 24))) // scalastyle:off magic.number
    val reason = Some(SupervisionEndReasons("Reason"))
  }

  "Json read and writes" must {

    "Serialise AnotherBodyNo as expected" in {
      Json.toJson(AnotherBodyNo.asInstanceOf[AnotherBody]) must be(Json.obj("anotherBody" -> false))
    }

    "Serialise AnotherBodyYes service as expected" in new Fixture {

      val input = AnotherBodyYes("Name", start, end, reason)

      val expectedJson = Json.obj(
        "anotherBody"    -> true,
        "supervisorName" -> "Name",
        "startDate"      -> Json.obj("supervisionStartDate" -> "1990-02-24"),
        "endDate"        -> Json.obj("supervisionEndDate" -> "1998-02-24"),
        "endingReason"   -> Json.obj("supervisionEndingReason" -> "Reason")
      )

      Json.toJson(input.asInstanceOf[AnotherBody]) must be(expectedJson)
    }

    "Deserialise AnotherBodyNo as expected" in {
      val json     = Json.obj("anotherBody" -> false)
      val expected = JsSuccess(AnotherBodyNo, JsPath)
      Json.fromJson[AnotherBody](json) must be(expected)
    }

    "Deserialise AnotherBodyYes as expected" in new Fixture {

      val input = Json.obj(
        "anotherBody"    -> true,
        "supervisorName" -> "Name",
        "startDate"      -> Json.obj("supervisionStartDate" -> "1990-02-24"),
        "endDate"        -> Json.obj("supervisionEndDate" -> "1998-02-24"),
        "endingReason"   -> Json.obj("supervisionEndingReason" -> "Reason")
      )

      val expected = AnotherBodyYes("Name", start, end, reason)

      Json.fromJson[AnotherBody](input) must be(JsSuccess(expected, JsPath))
    }

    "fail when missing all data" in {
      Json.fromJson[AnotherBody](Json.obj()) must
        be(JsError((JsPath \ "anotherBody") -> play.api.libs.json.JsonValidationError("error.path.missing")))
    }
  }

  "isComplete" must {
    "return true for complete AnotherBodyYes" in new Fixture {
      val completeAnotherBodyYes = AnotherBodyYes("Name", start, end, reason)
      completeAnotherBodyYes.isComplete() mustBe true
    }

    "return false for incomplete AnotherBodyYes" in new Fixture {
      val completeAnotherBodyYes = AnotherBodyYes("Name", start, end, reason)
      completeAnotherBodyYes.copy(startDate = None).isComplete() mustBe false
    }
  }
}
