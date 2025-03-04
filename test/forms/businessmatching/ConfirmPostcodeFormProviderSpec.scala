/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.businessmatching

import forms.behaviours.AddressFieldBehaviours
import forms.mappings.Constraints
import models.businessmatching.ConfirmPostcode
import play.api.data.{Form, FormError}

class ConfirmPostcodeFormProviderSpec extends AddressFieldBehaviours with Constraints {

  override val form: Form[ConfirmPostcode] = new ConfirmPostcodeFormProvider()()
  override val maxLength: Int              = 8
  override val regexString: String         = ""

  "ConfirmPostcodeFormProvider" must {

    val postcodeField = "postCode"

    "bind a valid postcode" in {

      forAll(postcodeGen) { postcode =>
        val newForm = form.bind(Map(postcodeField -> postcode))

        newForm(postcodeField).value shouldBe Some(postcode)
      }
    }

    "fail to bind" when {

      "postcode is empty" in {

        val newForm = form.bind(Map(postcodeField -> ""))

        newForm(postcodeField).error shouldBe Some(
          FormError(postcodeField, "businessmatching.confirm.postcode.error.empty")
        )
      }

      "postcode is invalid" in {

        forAll(postcodeGen.suchThat(_.nonEmpty), invalidChar) { case (postcode: String, invalidChar: String) =>
          val invalidPostcode = postcode.dropRight(1) + invalidChar
          val newForm         = form.bind(Map(postcodeField -> invalidPostcode))

          newForm(postcodeField).error shouldBe Some(
            FormError(postcodeField, "error.invalid.postcode", Seq(postcodeRegex))
          )
        }
      }
    }
  }
}
