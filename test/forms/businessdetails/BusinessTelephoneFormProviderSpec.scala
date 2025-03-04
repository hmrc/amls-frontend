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

package forms.businessdetails

import forms.behaviours.StringFieldBehaviours
import models.businessdetails.ContactingYouPhone
import org.scalacheck.Gen
import play.api.data.FormError

class BusinessTelephoneFormProviderSpec extends StringFieldBehaviours {

  val form = new BusinessTelephoneFormProvider()

  val fieldName = "phoneNumber"

  val requiredError = "error.required.phone.number"
  val lengthError   = "error.max.length.phone"
  val invalidError  = "err.invalid.phone.number"

  "BusinessEmailAddressFormProvider" must {

    "bind data" in {

      forAll(numStringOfLength(form.length)) { phoneNumber =>
        val result = form().bind(Map(fieldName -> phoneNumber))

        result.value  shouldBe Some(ContactingYouPhone(phoneNumber))
        result.errors shouldBe Nil
      }
    }

    "fail to bind" when {

      "input is empty" in {

        val result = form().bind(Map(fieldName -> ""))

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(fieldName, requiredError))
      }

      "inputs are too long" in {

        forAll(Gen.numStr.suchThat(_.length > form.length)) { phoneNumber =>
          val result = form().bind(Map(fieldName -> phoneNumber.toString))

          result.value  shouldBe None
          result.errors shouldBe Seq(
            FormError(fieldName, lengthError, Seq(form.length))
          )
        }
      }

      "inputs violate regex" in {

        forAll(numStringOfLength(form.length - 1), Gen.alphaChar) { (phoneNumber, invalidChar) =>
          val result = form().bind(Map(fieldName -> (phoneNumber + invalidChar)))

          result.value  shouldBe None
          result.errors shouldBe Seq(
            FormError(fieldName, invalidError, Seq(form.regex))
          )
        }
      }
    }
  }
}
