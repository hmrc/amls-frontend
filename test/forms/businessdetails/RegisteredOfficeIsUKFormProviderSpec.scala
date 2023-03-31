/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.behaviours.BooleanFieldBehaviours
import models.businessdetails.RegisteredOfficeIsUK
import play.api.data.{Form, FormError}

class RegisteredOfficeIsUKFormProviderSpec extends BooleanFieldBehaviours {

  val formProvider: RegisteredOfficeIsUKFormProvider = new RegisteredOfficeIsUKFormProvider()
  val form: Form[RegisteredOfficeIsUK] = formProvider()

  val fieldName = "isUK"
  val errorKey = "error.required.atb.registered.office.uk.or.overseas"

  "RegisteredOfficeIsUKFormProvider" must {

    "bind true" in {

      val result = form.bind(Map(fieldName -> "true"))

      result.hasErrors shouldBe false
      result.value shouldBe Some(RegisteredOfficeIsUK(true))
    }

    "bind false" in {

      val result = form.bind(Map(fieldName -> "false"))

      result.hasErrors shouldBe false
      result.value shouldBe Some(RegisteredOfficeIsUK(false))
    }

    "fail to bind" when {

      "value is invalid" in {

        val result = form.bind(Map(fieldName -> "foo"))

        result.value shouldBe None
        result.errors shouldBe Seq(FormError(fieldName, errorKey))
      }
    }

    behave like mandatoryField(form, fieldName, FormError(fieldName, errorKey))
  }
}
