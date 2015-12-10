package utils.validation

/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.joda.time.LocalDate
import org.scalatest.{Matchers, WordSpec}
import play.api.data.FormError
import uk.gov.hmrc.play.mappers.DateFields._
import utils.validation.DateTupleValidator._

class DateTupleValidatorSpec extends WordSpec with Matchers {
  "dateTuple" should {
    def assertError(dateFields: Map[String, String]) {
      val result = dateTuple.bind(dateFields)
      result.isLeft shouldBe true
      result.left.getOrElse(Nil) shouldBe Seq(FormError("", "err.invalid.date.format"))
    }

    "create a mapping for a valid date" in {
      val dateFields = Map(day -> "1", month -> "2", year -> "2014")
      val result = dateTuple.bind(dateFields)
      result.isRight shouldBe true
      result.right.getOrElse(None) shouldBe Some(new LocalDate(2014, 2, 1))
    }

    "return None when all the fields are empty" in {
      val dateFields = Map(day -> "", month -> "", year -> "")
      val result = dateTuple.bind(dateFields)
      result.isRight shouldBe true
      result.right.getOrElse(None) shouldBe None
    }

    "return a validation error for invalid date with characters" in {
      assertError(Map(day -> "1", month -> "2", year -> "bla"))
    }

    "return a validation error for invalid date with invalid month" in {
      assertError(Map(day -> "1", month -> "23", year -> "2014"))
    }

    "return a validation error for invalid date with only 2 digit year" in {
      assertError(Map(day -> "1", month -> "2", year -> "14"))
    }

    "return a validation error for invalid date with more than 4 digit year" in {
      assertError(Map(day -> "1", month -> "01", year -> "14444"))
    }

    "return a validation error for invalid date with more than 2 digit day" in {
      assertError(Map(day -> "122", month -> "01", year -> "2014"))
    }

    "return a validation error for invalid date with more than 2 digit month" in {
      assertError(Map(day -> "1", month -> "133", year -> "2014"))
    }

    "create a mapping for an invalid date (with space after month and day)" in {
      val dateFields = Map(day -> "1 ", month -> "2 ", year -> "2014")
      val result = dateTuple.bind(dateFields)
      result.isRight shouldBe true
      result.right.getOrElse(None) shouldBe Some(new LocalDate(2014, 2, 1))
    }
  }
}