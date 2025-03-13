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
import models.declaration.BusinessNominatedOfficer
import play.api.data.FormError

class BusinessNominatedOfficerFormProviderSpec extends StringFieldBehaviours {

  val formProvider = new BusinessNominatedOfficerFormProvider()
  val form         = formProvider()

  val fieldName = "value"
  val error     = "error.required.declaration.nominated.officer"

  "BusinessNominatedOfficerFormProvider" must {

    "bind with valid data" in {

      val str    = "officer1"
      val result = form.bind(Map(fieldName -> str))

      result.value shouldBe Some(BusinessNominatedOfficer(str))
      assert(result.errors.isEmpty)
    }

    behave like mandatoryField(form, fieldName, FormError(fieldName, error))
  }
}
