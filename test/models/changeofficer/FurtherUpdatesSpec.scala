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

package models.changeofficer

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec

class FurtherUpdatesSpec extends PlaySpec with MustMatchers {

  "The FurtherUpdates model" when {
    "given a valid form" when {
      "'yes' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "furtherUpdates" -> Seq("true")
          )

          val result = FurtherUpdates.formReads.validate(formData)

          result mustBe Valid(FurtherUpdatesYes)
        }
      }

      "'no' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "furtherUpdates" -> Seq("false")
          )

          val result = FurtherUpdates.formReads.validate(formData)

          result mustBe Valid(FurtherUpdatesNo)
        }
      }

      "nothing is selected" must {
        "return the validation errors" in {
          val formData = Map.empty[String, Seq[String]]

          val result = FurtherUpdates.formReads.validate(formData)

          result mustBe Invalid(Seq(Path \ "furtherUpdates" -> Seq(ValidationError("changeofficer.furtherupdates.validationerror"))))
        }
      }
    }

    "given a valid model" must {
      "return the form values" when {
        "FurtherUpdates is 'yes'" in {
          val model = FurtherUpdatesYes
          val result = FurtherUpdates.formWrites.writes(model)

          result mustBe Map("furtherUpdates" -> Seq("true"))
        }
        "FurtherUpdates is 'no'" in {
          val model = FurtherUpdatesNo
          val result = FurtherUpdates.formWrites.writes(model)

          result mustBe Map("furtherUpdates" -> Seq("false"))
        }
      }
    }
  }
}
