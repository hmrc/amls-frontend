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
class KnownBySpec extends PlaySpec with MockitoSugar {

  "Form Rules and Writes" must {

    "successfully validate" when {
      "given all fields" in {

        val data = Map(
          "hasOtherNames" -> Seq("true"),
          "otherNames" -> Seq("otherName")
        )

        val validPerson = KnownBy(
          hasOtherNames = Some(true),
          otherNames = Some("otherName")
        )

        KnownBy.formRule.validate(data) must equal(Valid(validPerson))
      }

    }

    "fail validation" when {

      "required fields are missing" when {
        "nothing has been selected" in {

          KnownBy.formRule.validate(Map(
            "hasOtherNames" -> Seq("")
          )) must equal(Invalid(Seq(
            (Path \ "hasOtherNames") -> Seq(ValidationError("error.required.rp.hasOtherNames"))
          )))
        }

        "fields have been selected" in {

          KnownBy.formRule.validate(Map(
            "hasOtherNames" -> Seq("true"),
            "otherNames" -> Seq("")
          )) must
            equal(Invalid(Seq(
              (Path \ "otherNames") -> Seq(ValidationError("error.required.rp.otherNames"))
            )))
        }
      }

      "input length is too great" in {

        val data = Map(
          "hasOtherNames" -> Seq("true"),
          "otherNames" -> Seq("otherName" * 36)
        )

        KnownBy.formRule.validate(data) must
          equal(Invalid(Seq(
            (Path \ "otherNames") -> Seq(ValidationError("error.invalid.maxlength.140"))
          )))
      }

      "fields have invalid characters" in {

        KnownBy.formRule.validate(Map(
          "hasOtherNames" -> Seq("true"),
          "otherNames" -> Seq("($£*£$:?/{")
        )) must
          equal(Invalid(Seq(
            (Path \ "otherNames") -> Seq(ValidationError("err.text.validation"))
          )))
      }

    }

  }
}
