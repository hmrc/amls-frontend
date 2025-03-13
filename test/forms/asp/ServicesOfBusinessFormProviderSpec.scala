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

package forms.asp

import forms.behaviours.CheckboxFieldBehaviours
import models.asp.{Service, ServicesOfBusiness}
import play.api.data.FormError

class ServicesOfBusinessFormProviderSpec extends CheckboxFieldBehaviours {

  val form = new ServicesOfBusinessFormProvider()()

  ".value" must {

    val fieldName   = "services"
    val requiredKey = "error.required.asp.business.services"

    behave like checkboxFieldWithWrapper[Service, ServicesOfBusiness](
      form,
      fieldName,
      validValues = Service.all,
      x => ServicesOfBusiness(Set(x)),
      x => ServicesOfBusiness(x.toSet),
      invalidError = FormError(s"$fieldName[0]", requiredKey)
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey
    )
  }

}
