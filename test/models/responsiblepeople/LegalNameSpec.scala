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
class LegalNameSpec extends PlaySpec with MockitoSugar {

  "Form Rules and Writes" must {

    "successfully validate" when {
      "given all fields" in {

        val data = Map(
          "hasPreviousName" -> Seq("true"),
          "previous.firstName" -> Seq("oldFirst"),
          "previous.middleName" -> Seq("oldMiddle"),
          "previous.lastName" -> Seq("oldLast")
        )

        val validPerson = LegalName(
          previousName = Some(
            PreviousName(
              firstName = Some("oldFirst"),
              middleName = Some("oldMiddle"),
              lastName = Some("oldLast"),
              // scalastyle:off magic.number
              date = new LocalDate(1990, 2, 24)
            )
          )
        )

        LegalName.formRule.validate(data) must equal(Valid(validPerson))
      }

    }

    "fail validation" when {

      "required fields are missing" when {
        "nothing has been selected" in {

          LegalName.formRule.validate(Map(
            "hasPreviousName" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "hasPreviousName") -> Seq(ValidationError("error.required.rp.hasPreviousName"))
          )))
        }

        "fields have been selected" in {

          LegalName.formRule.validate(Map(
            "hasPreviousName" -> Seq("true")
          )) must
            equal(Invalid(Seq(
              (Path \ "previous") -> Seq(ValidationError("error.rp.previous.invalid"))
            )))
        }
      }

      "input length is too great" in {

        val data = Map(
          "hasPreviousName" -> Seq("true"),
          "previous.firstName" -> Seq("oldFirst" * 36),
          "previous.middleName" -> Seq("oldMiddle" * 36),
          "previous.lastName" -> Seq("oldLast" * 36)
        )

        LegalName.formRule.validate(data) must
          equal(Invalid(Seq(
            (Path \ "previous" \ "firstName") -> Seq(ValidationError("error.invalid.common_name.length")),
            (Path \ "previous" \ "middleName") -> Seq(ValidationError("error.invalid.common_name.length")),
            (Path \ "previous" \ "lastName") -> Seq(ValidationError("error.invalid.common_name.length"))
          )))
      }

      "fields have invalid characters" in {

        LegalName.formRule.validate(Map(
          "hasPreviousName" -> Seq("true"),
          "previous.firstName" -> Seq("($£*£$"),
          "previous.middleName" -> Seq(")£(@$)$( "),
          "previous.lastName" -> Seq("$&£@$*&$%&$")
        )) must
          equal(Invalid(Seq(
            (Path \ "previous" \ "firstName") -> Seq(ValidationError("error.invalid.common_name.validation")),
            (Path \ "previous" \ "middleName") -> Seq(ValidationError("error.invalid.common_name.validation")),
            (Path \ "previous" \ "lastName") -> Seq(ValidationError("error.invalid.common_name.validation"))
          )))
      }

    }

  }
}
