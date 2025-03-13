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

package forms.declaration

import forms.behaviours.StringFieldBehaviours
import models.declaration.BusinessPartners
import play.api.data.FormError

class BusinessPartnersFormProviderSpec extends StringFieldBehaviours {

  val formProvider = new BusinessPartnersFormProvider()
  val form         = formProvider()

  val fieldName = "value"
  val error     = "error.required.declaration.partners"

  "BusinessPartnersFormProvider" must {

    "bind with valid data" in {

      val str    = "partner1"
      val result = form.bind(Map(fieldName -> str))

      result.value shouldBe Some(BusinessPartners(str))
      assert(result.errors.isEmpty)
    }

    behave like mandatoryField(form, fieldName, FormError(fieldName, error))
  }
}
