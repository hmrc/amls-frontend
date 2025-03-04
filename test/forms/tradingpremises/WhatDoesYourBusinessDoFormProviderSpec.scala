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

package forms.tradingpremises

import forms.behaviours.CheckboxFieldBehaviours
import models.businessmatching.{BusinessActivities, BusinessActivity}
import models.tradingpremises.WhatDoesYourBusinessDo
import play.api.data.FormError

class WhatDoesYourBusinessDoFormProviderSpec extends CheckboxFieldBehaviours {

  val form = new WhatDoesYourBusinessDoFormProvider()()

  ".value" must {

    val fieldName   = "value"
    val requiredKey = "error.required.tp.activity.your.business.do"

    behave like checkboxFieldWithWrapper[BusinessActivity, WhatDoesYourBusinessDo](
      form,
      fieldName,
      validValues = BusinessActivities.all.toSeq,
      x => WhatDoesYourBusinessDo(Set(x)),
      x => WhatDoesYourBusinessDo(x.toSet),
      invalidError = FormError(s"$fieldName[0]", "error.invalid")
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey
    )
  }
}
