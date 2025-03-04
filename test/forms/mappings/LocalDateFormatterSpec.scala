/*
 * Copyright 2022 HM Revenue & Customs
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

package forms.mappings

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError

import java.time.LocalDate

class LocalDateFormatterSpec extends AnyWordSpec with Matchers {

  val oneInvalidKey      = "oneInvalidKey"
  val multipleInvalidKey = "multipleInvalidKey"
  val oneRequiredKey     = "oneRequiredKey"
  val twoRequiredKey     = "twoRequiredKey"
  val allRequiredKey     = "allRequiredKey"
  val realDateKey        = "realDateKey"
  val formatter          = new LocalDateFormatter(
    oneInvalidKey,
    multipleInvalidKey,
    oneRequiredKey,
    twoRequiredKey,
    allRequiredKey,
    realDateKey
  )

  ".validateDayMonthYear" should {

    "return no form errors when all date values are valid" in {
      formatter.validateDayMonthYear("date", Some("11"), Some("11"), Some("2000")) shouldBe Nil
    }

    "return form errors" when {

      "the day is invalid" in {
        formatter.validateDayMonthYear("date", Some("f"), Some("11"), Some("2000")) shouldBe
          Seq(FormError("date.day", oneInvalidKey, Seq("day")))
      }

      "the month is invalid" in {
        formatter.validateDayMonthYear("date", Some("11"), Some("f"), Some("2000")) shouldBe
          Seq(FormError("date.month", oneInvalidKey, Seq("month")))
      }

      "the year is invalid" in {
        formatter.validateDayMonthYear("date", Some("11"), Some("11"), Some("f")) shouldBe
          Seq(FormError("date.year", oneInvalidKey, Seq("year")))
      }

      "the month and year are invalid" in {
        formatter.validateDayMonthYear("date", Some("11"), Some("20"), Some("0")) shouldBe Seq(
          FormError("date.month", multipleInvalidKey, Seq("month", "year")),
          FormError("date.year", multipleInvalidKey, Seq("month", "year"))
        )
      }

      "the day and year are invalid" in {
        formatter.validateDayMonthYear("date", Some("50"), Some("11"), Some("0")) shouldBe Seq(
          FormError("date.day", multipleInvalidKey, Seq("day", "year")),
          FormError("date.year", multipleInvalidKey, Seq("day", "year"))
        )
      }

      "the day and month are invalid" in {
        formatter.validateDayMonthYear("date", Some("5.6"), Some("9.9"), Some("2000")) shouldBe Seq(
          FormError("date.day", multipleInvalidKey, Seq("day", "month")),
          FormError("date.month", multipleInvalidKey, Seq("day", "month"))
        )
      }

      "all three fields are invalid" in {
        formatter.validateDayMonthYear("date", Some("&%£"), Some("~@;"), Some("-_-")) shouldBe
          Seq(FormError("date", multipleInvalidKey))
      }

      "the day is empty" in {
        formatter.validateDayMonthYear("date", None, Some("11"), Some("2000")) shouldBe
          Seq(FormError("date.day", oneRequiredKey, Seq("day")))
      }

      "the month is empty" in {
        formatter.validateDayMonthYear("date", Some("11"), None, Some("2000")) shouldBe
          Seq(FormError("date.month", oneRequiredKey, Seq("month")))
      }

      "the year is empty" in {
        formatter.validateDayMonthYear("date", Some("11"), Some("11"), None) shouldBe
          Seq(FormError("date.year", oneRequiredKey, Seq("year")))
      }

      "the month and year are empty" in {
        formatter.validateDayMonthYear("date", Some("11"), None, None) shouldBe Seq(
          FormError("date.month", twoRequiredKey, Seq("month", "year")),
          FormError("date.year", twoRequiredKey, Seq("month", "year"))
        )
      }

      "the day and year are empty" in {
        formatter.validateDayMonthYear("date", None, Some("11"), None) shouldBe Seq(
          FormError("date.day", twoRequiredKey, Seq("day", "year")),
          FormError("date.year", twoRequiredKey, Seq("day", "year"))
        )
      }

      "the day and month are empty" in {
        formatter.validateDayMonthYear("date", None, None, Some("2000")) shouldBe Seq(
          FormError("date.day", twoRequiredKey, Seq("day", "month")),
          FormError("date.month", twoRequiredKey, Seq("day", "month"))
        )
      }

      "all three fields are empty" in {
        formatter.validateDayMonthYear("date", None, None, None) shouldBe
          Seq(FormError("date", allRequiredKey))
      }

      "there are a mix of invalid and empty fields (empty takes precedence)" in {
        formatter.validateDayMonthYear("date", Some("f"), None, Some("f")) shouldBe
          Seq(FormError("date.month", oneRequiredKey, Seq("month")))
      }
    }
  }

  ".toDate" should {

    "return a LocalDate when the provided values make a valid date" in {
      formatter.toDate("date", 11, 11, 2000) shouldBe Right(LocalDate.parse("2000-11-11"))
    }

    "return a form error when the provided values do not make a valid date" in {
      formatter.toDate("date", 31, 11, 2000) shouldBe Left(Seq(FormError("date", realDateKey)))
    }
  }

  ".bind" should {

    "return a LocalDate when binding was successful" in {
      formatter.bind("date", Map("date.day" -> "11", "date.month" -> "11", "date.year" -> "2000")) shouldBe
        Right(LocalDate.parse("2000-11-11"))
    }

    "return the form errors when binding was unsuccessful" in {
      formatter.bind("date", Map("date.day" -> "fff", "date.month" -> "79", "date.year" -> "3.142")) shouldBe
        Left(Seq(FormError("date", multipleInvalidKey)))
    }
  }

  ".unbind" should {

    "return form data from a LocalDate" in {
      formatter.unbind("date", LocalDate.parse("2000-11-11")) shouldBe Map(
        "date.day"   -> "11",
        "date.month" -> "11",
        "date.year"  -> "2000"
      )
    }
  }
}
