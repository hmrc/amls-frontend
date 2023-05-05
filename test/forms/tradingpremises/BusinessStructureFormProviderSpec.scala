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

package forms.tradingpremises

import forms.behaviours.RadioFieldBehaviours
import forms.businessmatching.RegisterBusinessActivitiesFormProvider
import models.businessmatching.{BusinessActivities, BusinessActivity}
import models.tradingpremises.BusinessStructure
import play.api.data.FormError

class BusinessStructureFormProviderSpec extends RadioFieldBehaviours {

  val form = new BusinessStructureFormProvider()()

  ".agentsBusinessStructure" must {

    val fieldName = "agentsBusinessStructure"
    val requiredKey = "error.required.tp.select.business.structure"

    behave like radioField[BusinessStructure](
      form,
      fieldName,
      validValues = BusinessStructure.all,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryRadioField(
      form,
      fieldName,
      requiredKey
    )
  }
}
