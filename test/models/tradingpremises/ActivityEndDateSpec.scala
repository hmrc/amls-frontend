/*
 * Copyright 2018 HM Revenue & Customs
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

import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}


class ActivityEndDateSpec extends PlaySpec {

  val startDateField = Map("premisesStartDate" -> Seq("1989-01-01"))

  "ActivityEndDate" must {
    "Form" must {
      "read successfully" in {
        val model =  startDateField ++ Map (
            "endDate.day" -> Seq("24"),
            "endDate.month" -> Seq("2"),
            "endDate.year" -> Seq("1990")
        )

        // scalastyle:off
        ActivityEndDate.formRule.validate(model) must be (Valid(ActivityEndDate(new LocalDate(1990, 2, 24))))

      }

      "throw error message when day entered is invalid" in {
        val model =  startDateField ++ Map (
          "endDate.day" -> Seq("2466"),
          "endDate.month" -> Seq("2"),
          "endDate.year" -> Seq("1990")
        )
        ActivityEndDate.formRule.validate(model) must be(Invalid(Seq(Path \ "endDate" -> Seq(
          ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))

      }

      "throw error message when data entered is empty" in {
        val model =  startDateField ++ Map (
          "endDate.day" -> Seq(""),
          "endDate.month" -> Seq(""),
          "endDate.year" -> Seq("")
        )
        ActivityEndDate.formRule.validate(model) must be(Invalid(Seq(
          Path \ "endDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
          )))

      }

      "successfully write the model" in {

        ActivityEndDate.formWrites.writes(ActivityEndDate(new LocalDate(1990, 2, 24)))  mustBe Map(
          "endDate.day" -> Seq("24"),
          "endDate.month" -> Seq("2"),
          "endDate.year" -> Seq("1990")
        )
      }
    }

    "Json" should {

      "Read and write successfully" in {
        ActivityEndDate.format.reads(ActivityEndDate.format.writes(ActivityEndDate(new LocalDate(1990, 2, 24)))) must be(
          JsSuccess(ActivityEndDate(new LocalDate(1990, 2, 24)), JsPath \ "endDate"))

      }

      "write successfully" in {
        ActivityEndDate.format.writes(ActivityEndDate(new LocalDate(1990, 2, 24))) must be(Json.obj("endDate" ->"1990-02-24"))
      }
    }
  }
 }
