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

package models.responsiblepeople

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

@SuppressWarnings(Array("org.brianmckenna.wartremover.warts.MutableDataStructures"))
class PersonNameSpec extends PlaySpec with MockitoSugar {

  "Form Rules and Writes" must {

    "successfully validate" when {
      "given all fields" in {

        val data = Map(
          "firstName" -> Seq("first"),
          "middleName" -> Seq("middle"),
          "lastName" -> Seq("last")
        )

        val validPerson = PersonName(
          firstName = "first",
          middleName = Some("middle"),
          lastName = "last"
        )

        PersonName.formRule.validate(data) must equal(Valid(validPerson))
      }

      "the middle name is optional and previous/other names are not required" in {

        val data = Map(
          "firstName" -> Seq("first"),
          "lastName" -> Seq("last")
        )

        val validPerson = PersonName(
          firstName = "first",
          middleName = None,
          lastName = "last"
        )

        PersonName.formRule.validate(data) must equal(Valid(validPerson))
      }
    }

    "fail validation" when {

      "required fields are missing" when {
        "nothing has been selected" in {

          PersonName.formRule.validate(Map(
            "firstName" -> Seq(""),
            "lastName" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.required.rp.first_name")),
            (Path \ "lastName") -> Seq(ValidationError("error.required.rp.last_name"))
          )))
        }
      }

      "input length is too great" in {

        val data = Map(
          "firstName" -> Seq("first" * 36),
          "middleName" -> Seq("first" * 36),
          "lastName" -> Seq("last" * 36)
        )

        PersonName.formRule.validate(data) must
          equal(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.invalid.common_name.length")),
            (Path \ "middleName") -> Seq(ValidationError("error.invalid.common_name.length")),
            (Path \ "lastName") -> Seq(ValidationError("error.invalid.common_name.length"))
          )))
      }


      "fields have invalid characters" in {

        PersonName.formRule.validate(Map(
          "firstName" -> Seq("92)(OELer"),
          "middleName" -> Seq("£*($*)(ERKLFD "),
          "lastName" -> Seq("9*£@$")
        )) must
          equal(Invalid(Seq(
            (Path \ "firstName") -> Seq(ValidationError("error.invalid.common_name.validation")),
            (Path \ "middleName") -> Seq(ValidationError("error.invalid.common_name.validation")),
            (Path \ "lastName") -> Seq(ValidationError("error.invalid.common_name.validation"))
          )))
      }

    }

  }

  "fullName" must {
    "return a correctly formatted name" in {
      PersonName("first", Some("middle"), "last").fullName must be("first middle last")
      PersonName("first", None, "last").fullName must be("first last")
    }
  }
}
