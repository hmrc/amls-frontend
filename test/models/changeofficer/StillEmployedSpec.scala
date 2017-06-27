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

package models.changeofficer

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec

class StillEmployedSpec extends PlaySpec with MustMatchers {

  "The StillEmployed model" when {
    "given a valid form" when {
      "'yes' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "stillEmployed" -> Seq("true")
          )

          val result = StillEmployed.formReads.validate(formData)

          result mustBe Valid(StillEmployedYes)
        }
      }

      "nothing is selected" must {
        "return the validation errors" in {
          val formData = Map.empty[String, Seq[String]]

          val result = StillEmployed.formReads.validate(formData)

          result mustBe Invalid(Seq(Path \ "stillEmployed" -> Seq(ValidationError("changeofficer.stillemployed.validationerror"))))
        }
      }
    }
  }

}
