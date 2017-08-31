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

package models.renewal

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec

class UpdateAnyInformationSpec extends PlaySpec with MustMatchers {

  "The UpdateAnyInformation model" when {
    "given a valid form" when {
      "'yes' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "updateAnyInformation" -> Seq("true")
          )

          val result = UpdateAnyInformation.formReads.validate(formData)

          result mustBe Valid(UpdateAnyInformationYes)
        }
      }

      "'no' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "updateAnyInformation" -> Seq("false")
          )

          val result = UpdateAnyInformation.formReads.validate(formData)

          result mustBe Valid(UpdateAnyInformationNo)
        }
      }

      "nothing is selected" must {
        "return the validation errors" in {
          val formData = Map.empty[String, Seq[String]]

          val result = UpdateAnyInformation.formReads.validate(formData)

          result mustBe Invalid(Seq(Path \ "updateAnyInformation" -> Seq(ValidationError("renewal.updateanyInformation.validationerror"))))
        }
      }
    }

    "given a valid model" must {
      "return the form values" when {
        "UpdateAnyInformation is 'yes'" in {
          val model = UpdateAnyInformationYes
          val result = UpdateAnyInformation.formWrites.writes(model)

          result mustBe Map("updateAnyInformation" -> Seq("true"))
        }
        "UpdateAnyInformation is 'no'" in {
          val model = UpdateAnyInformationNo
          val result = UpdateAnyInformation.formWrites.writes(model)

          result mustBe Map("updateAnyInformation" -> Seq("false"))
        }
      }
    }
  }
}
