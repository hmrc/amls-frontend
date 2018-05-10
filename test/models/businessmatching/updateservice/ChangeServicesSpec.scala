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
import utils.AmlsSpec

class ChangeServicesSpec extends AmlsSpec {
  "The ChangeServices model" when {

    "given a valid form" when {
      "'add' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "changeServices" -> Seq("add")
          )

          val result = ChangeServices.formReads.validate(formData)

          result mustBe Valid(ChangeServicesAdd)
        }
      }


      "nothing is selected" must {
        "return the validation errors" in {
          val formData = Map.empty[String, Seq[String]]

          val result = ChangeServices.formReads.validate(formData)

          result mustBe Invalid(
            Seq(
              Path \ "changeServices" ->
                Seq(ValidationError("error.businessmatching.updateservice.changeservices"))
            ))
        }
      }
    }

  }
}
