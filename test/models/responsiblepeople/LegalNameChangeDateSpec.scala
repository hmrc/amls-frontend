/*
 * Copyright 2017 HM Revenue & Customs
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
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

@SuppressWarnings(Array("org.brianmckenna.wartremover.warts.MutableDataStructures"))
class LegalNameChangeDateSpec extends PlaySpec with MockitoSugar {

  "Form Rules and Writes" must {

    "successfully validate" when {
      "given all fields" in {

        val data = Map(
          "date.year" -> Seq("1990"),
          "date.month" -> Seq("02"),
          "date.day" -> Seq("24")
        )

        val validDate = LegalNameChangeDate(
          date = new LocalDate(1990, 2, 24)
        )

        LegalNameChangeDate.formRule.validate(data) must equal(Valid(validDate))
      }

    }

    "fail validation" when {

      "required fields are missing" when {
        "nothing has been selected" in {

          LegalNameChangeDate.formRule.validate(Map(
            "date.year" -> Seq(""),
            "date.month" -> Seq(""),
            "date.day" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "date") -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
          )))
        }
      }

      "fields are invalid" in {

        LegalNameChangeDate.formRule.validate(Map(
          "date.year" -> Seq("199000"),
          "date.month" -> Seq("02"),
          "date.day" -> Seq("24")
        )) must
          equal(Invalid(Seq(
            (Path \ "date") -> Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))
          )))
      }

    }

  }
}
