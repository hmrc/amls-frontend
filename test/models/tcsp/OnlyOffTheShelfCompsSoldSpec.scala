/*
 * Copyright 2019 HM Revenue & Customs
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

/*
 * Copyright 2019 HM Revenue & Customs
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

class OnlyOffTheShelfCompsSoldSpec extends PlaySpec with MustMatchers {

  "The OnlyOffTheShelfCompsSold model" when {
    "given a valid form" when {
      "'yes' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "onlyOffTheShelfCompsSold" -> Seq("true")
          )

          val result = OnlyOffTheShelfCompsSold.formReads.validate(formData)
          result mustBe Valid(OnlyOffTheShelfCompsSoldYes)
        }
      }

      "'no' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "onlyOffTheShelfCompsSold" -> Seq("false")
          )

          val result = OnlyOffTheShelfCompsSold.formReads.validate(formData)

          result mustBe Valid(OnlyOffTheShelfCompsSoldNo)
        }
      }

      "nothing is selected" must {
        "return the validation errors" in {
          val formData = Map.empty[String, Seq[String]]

          val result = OnlyOffTheShelfCompsSold.formReads.validate(formData)

          result mustBe Invalid(Seq(Path \ "onlyOffTheShelfCompsSold" -> Seq(ValidationError("error.required.tcsp.off.the.shelf.companies"))))
        }
      }
    }

    "given a valid model" must {
      "return the form values" when {
        "onlyOffTheShelfCompsSold is 'yes'" in {
          val model = OnlyOffTheShelfCompsSoldYes
          val result = OnlyOffTheShelfCompsSold.formWrites.writes(model)

          result mustBe Map("onlyOffTheShelfCompsSold" -> Seq("true"))
        }
        "onlyOffTheShelfCompsSold is 'no'" in {
          val model = OnlyOffTheShelfCompsSoldNo
          val result = OnlyOffTheShelfCompsSold.formWrites.writes(model)

          result mustBe Map("onlyOffTheShelfCompsSold" -> Seq("false"))
        }

        "for json" when {
          "onlyOffTheShelfCompsSold is 'yes'" in {
            val model = OnlyOffTheShelfCompsSoldYes
            val result = OnlyOffTheShelfCompsSold.jsonWrite.writes(model).toString()
            val expected = "{\"onlyOffTheShelfCompsSold\":true}"

            result mustBe expected
          }
          "onlyOffTheShelfCompsSold is 'no'" in {
            val model = OnlyOffTheShelfCompsSoldNo
            val result = OnlyOffTheShelfCompsSold.jsonWrite.writes(model).toString()
            val expected = "{\"onlyOffTheShelfCompsSold\":false}"

            result mustBe expected
          }
        }
      }
    }
  }
}
