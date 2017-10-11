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

package models.businessmatching.updateservice

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import utils.GenericTestHelper

class FitAndProperSpec extends GenericTestHelper {
  "The FitAndProper model" when {

    "given a valid form" when {
      "'yes' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "passedFitAndProper" -> Seq("true")
          )

          val result = PassedFitAndProper.formReads.validate(formData)

          result mustBe Valid(PassedFitAndProperYes)
        }
      }

      "'no' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "passedFitAndProper" -> Seq("false")
          )

          val result = PassedFitAndProper.formReads.validate(formData)

          result mustBe Valid(PassedFitAndProperNo)
        }
      }

      "nothing is selected" must {
        "return the validation errors" in {
          val formData = Map.empty[String, Seq[String]]

          val result = PassedFitAndProper.formReads.validate(formData)

          result mustBe Invalid(
            Seq(
              Path \ "passedFitAndProper" ->
                Seq(ValidationError("error.businessmatching.updateservice.fitandproper"))
            ))
        }
      }
    }
  }
}
