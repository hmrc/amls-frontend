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

package models.businessmatching.updateservice

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec

class ResponsiblePersonFitAndProperSpec extends PlaySpec with MustMatchers {

  "The ResponsiblePeopleFitAndProper model" when {
    "given a valid form" when {
      "return a valid form model" when {
        "single selection is made" in {
          val formData = Map(
            "responsiblePeople[]" -> Seq("1")
          )

          val result = ResponsiblePeopleFitAndProper.formReads.validate(formData)

          result mustBe Valid(ResponsiblePeopleFitAndProper(Set(1)))
        }
        "multiple selections are made" in {
          val formData = Map(
            "responsiblePeople[]" -> Seq("1", "2")
          )

          val result = ResponsiblePeopleFitAndProper.formReads.validate(formData)

          result mustBe Valid(ResponsiblePeopleFitAndProper(Set(1, 2)))
        }
      }

      "nothing is selected" must {
        "return the validation errors" in {
          val formData = Map.empty[String, Seq[String]]

          val result = ResponsiblePeopleFitAndProper.formReads.validate(formData)

          result mustBe Invalid(
            Seq(
              Path \ "responsiblePeople" ->
                Seq(ValidationError("error.businessmatching.updateservice.responsiblepeople"))
            ))
        }
      }
    }

    "given a valid model" must {
      "return the form values" when {
        "single selection" in {

          val result = ResponsiblePeopleFitAndProper.formWrites.writes(ResponsiblePeopleFitAndProper(Set(2)))

          result mustBe Map("responsiblePeople[]" -> Seq("2"))
        }
        "multiple selection" in {

          val result = ResponsiblePeopleFitAndProper.formWrites.writes(ResponsiblePeopleFitAndProper(Set(0, 2)))

          result mustBe Map("responsiblePeople[]" -> Seq("0", "2"))
        }
      }
    }
  }
}
