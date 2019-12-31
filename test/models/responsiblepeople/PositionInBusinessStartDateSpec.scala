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

package models.responsiblepeople

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

class PositionInBusinessStartDateSpec extends PlaySpec with MockitoSugar {

  "PositionStartDate" should {

      "successfully validate given a valid date" in {

        val data = Map(
          "startDate.day" -> Seq("15"),
          "startDate.month" -> Seq("2"),
          "startDate.year" -> Seq("1956")
        )

        PositionStartDate.formRule.validate(data) must
          be(Valid(PositionStartDate(new LocalDate(1956, 2, 15))))
      }

      "fail validation" when {
        "given a day in future beyond end of 2099" in {
          val model = PositionStartDate.formWrites.writes(PositionStartDate(new LocalDate(2100, 1, 1)))

          PositionStartDate.formRule.validate(model) must be(Invalid(Seq(
            Path \ "startDate" -> Seq(ValidationError("error.future.date"))
          )))
        }

        "given a day in the past before start of 1900" in {
          val model = PositionStartDate.formWrites.writes(PositionStartDate(new LocalDate(1089, 12, 31)))

          PositionStartDate.formRule.validate(model) must
            be(Invalid(Seq(Path \ "startDate" -> Seq(ValidationError("error.allowed.start.date")))))
        }

        "given an invalid date" in {

          val data = Map(
            "startDate.day" -> Seq("30"),
            "startDate.month" -> Seq("2"),
            "startDate.year" -> Seq("1956")
          )

          PositionStartDate.formRule.validate(data) must
            be(Invalid(Seq(Path \ "startDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
        }

        "given an future date" in {
          val model = PositionStartDate.formWrites.writes(PositionStartDate(LocalDate.now.plusMonths(1)))

          PositionStartDate.formRule.validate(model) must
            be(Invalid(Seq(Path \ "startDate" -> Seq(ValidationError("error.future.date")))))
        }

        "given a missing day" in {

          val data = Map(
            "startDate.day" -> Seq(""),
            "startDate.month" -> Seq("2"),
            "startDate.year" -> Seq("1956")
          )

          PositionStartDate.formRule.validate(data) must
            be(Invalid(Seq(Path \ "startDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
        }

        "given a missing month" in {

          val data = Map(
            "startDate.day" -> Seq("2"),
            "startDate.month" -> Seq(""),
            "startDate.year" -> Seq("1956")
          )

          PositionStartDate.formRule.validate(data) must
            be(Invalid(Seq(Path \ "startDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
        }

        "given a missing year" in {

          val data = Map(
            "startDate.day" -> Seq("1"),
            "startDate.month" -> Seq("2"),
            "startDate.year" -> Seq("")
          )

          PositionStartDate.formRule.validate(data) must
            be(Invalid(Seq(Path \ "startDate" -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
        }

      }

      "write to form given a valid start date" in {

        val startDate = PositionStartDate(new LocalDate(1990, 2, 24))

        PositionStartDate.formWrites.writes(startDate) must
          be(Map(
            "startDate.day" -> List("24"), "startDate.month" -> List("2"), "startDate.year" -> List("1990")))
      }
  }
}
