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

class UpdateInformationSpec extends PlaySpec with MustMatchers {

  "The UpdateInformation model" when {
    "given a valid form" when {
      "'yes' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "updateInformation" -> Seq("true")
          )

          val result = UpdateInformation.formReads.validate(formData)

          result mustBe Valid(UpdateInformationYes)
        }
      }

      "'no' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "updateInformation" -> Seq("false")
          )

          val result = UpdateInformation.formReads.validate(formData)

          result mustBe Valid(UpdateInformationNo)
        }
      }

      "nothing is selected" must {
        "return the validation errors" in {
          val formData = Map.empty[String, Seq[String]]

          val result = UpdateInformation.formReads.validate(formData)

          result mustBe Invalid(Seq(Path \ "updateInformation" -> Seq(ValidationError("changeofficer.updateinformation.validationerror"))))
        }
      }
    }

    "given a valid model" must {
      "return the form values" when {
        "UpdateInformation is 'yes'" in {
          val model = UpdateInformationYes
          val result = UpdateInformation.formWrites.writes(model)

          result mustBe Map("updateInformation" -> Seq("true"))
        }
        "UpdateInformation is 'no'" in {
          val model = UpdateInformationNo
          val result = UpdateInformation.formWrites.writes(model)

          result mustBe Map("updateInformation" -> Seq("false"))
        }
      }
    }
  }
}
