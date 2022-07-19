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

package models.tcsp

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec

class ComplexCorpStructureCreationSpec extends PlaySpec with MustMatchers {

  "The ComplexCorpStructreCreationSpec model" when {
    "given a valid form" when {
      "'yes' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "complexCorpStructureCreation" -> Seq("true")
          )

          val result = ComplexCorpStructureCreation.formReads.validate(formData)
          result mustBe Valid(ComplexCorpStructureCreationYes)
        }
      }

      "'no' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "complexCorpStructureCreation" -> Seq("false")
          )

          val result = ComplexCorpStructureCreation.formReads.validate(formData)

          result mustBe Valid(ComplexCorpStructureCreationNo)
        }
      }

      "nothing is selected" must {
        "return the validation errors" in {
          val formData = Map.empty[String, Seq[String]]

          val result = ComplexCorpStructureCreation.formReads.validate(formData)

          result mustBe Invalid(Seq(Path \ "complexCorpStructureCreation" -> Seq(ValidationError("error.required.tcsp.complex.corporate.structures"))))
        }
      }
    }

    "given a valid model" must {
      "return the form values" when {
        "complexCorpStructureCreation is 'yes'" in {
          val model = ComplexCorpStructureCreationYes
          val result = ComplexCorpStructureCreation.formWrites.writes(model)

          result mustBe Map("complexCorpStructureCreation" -> Seq("true"))
        }
        "complexCorpStructureCreation is 'no'" in {
          val model = ComplexCorpStructureCreationNo
          val result = ComplexCorpStructureCreation.formWrites.writes(model)

          result mustBe Map("complexCorpStructureCreation" -> Seq("false"))
        }

        "for json" when {
          "complexCorpStructureCreation is 'yes'" in {
            val model = ComplexCorpStructureCreationYes
            val result = ComplexCorpStructureCreation.jsonWrite.writes(model).toString()
            val expected = "{\"complexCorpStructureCreation\":true}"

            result mustBe expected
          }
          "complexCorpStructureCreation is 'no'" in {
            val model = ComplexCorpStructureCreationNo
            val result = ComplexCorpStructureCreation.jsonWrite.writes(model).toString()
            val expected = "{\"complexCorpStructureCreation\":false}"

            result mustBe expected
          }
        }
      }
    }
  }
}
