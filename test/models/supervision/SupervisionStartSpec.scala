/*
 * Copyright 2019 HM Revenue & Customs
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

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class SupervisionStartSpec extends PlaySpec with MockitoSugar {
  trait Fixture {

    val start = new LocalDate(1990, 2, 24) //scalastyle:off magic.number
  }

  "Form Rules and Writes" must {
    "given 'yes' selected with valid start date " in new Fixture {

      val urlFormEncoded = Map(
        "anotherBody" -> Seq("true"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("2"),
        "startDate.year" -> Seq("1990")
      )

      val expected = Valid(SupervisionStart(start))

      SupervisionStart.formRule.validate(urlFormEncoded) must be(expected)
    }

    "fail validation" when {
      "given a future date" in new Fixture {

        val data = SupervisionStart.formWrites.writes(SupervisionStart(LocalDate.now().plusDays(1)))
        SupervisionStart.formRule.validate(data) must be(Invalid(Seq(Path \ "startDate" -> Seq(
          ValidationError("error.future.date")))))
      }
    }
  }

  "Json read and writes" must {
    "successfully write start date" in  new Fixture {

      val expected = Map(
        "anotherBody" -> Seq("true"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("2"),
        "startDate.year" -> Seq("1990")
      )

      val input = SupervisionStart(start)

      SupervisionStart.formWrites.writes(input) must be(expected)
    }

    "Serialise SupervisionStart as expected" in new Fixture {

      val input = SupervisionStart(start)

      val expectedJson = Json.obj("startDate" -> "1990-02-24")

      Json.toJson(input) must be(expectedJson)
    }

    "Deserialise SupervisionStart as expected" in new Fixture {

      val input = Json.obj(
        "anotherBody" -> true,
        "startDate" -> "1990-02-24")

      val expected = SupervisionStart(start)

      Json.fromJson[SupervisionStart](input) must be (JsSuccess(expected, JsPath \ "startDate"))
    }

    "fail when data is missing" in {
      Json.fromJson[SupervisionStart](Json.obj()) must
        be(JsError((JsPath \ "startDate") -> play.api.data.validation.ValidationError("error.path.missing")))
    }
  }
}
