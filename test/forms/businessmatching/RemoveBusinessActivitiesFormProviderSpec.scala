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

import forms.behaviours.CheckboxFieldBehaviours
import models.businessmatching.BusinessActivity.{AccountancyServices, ArtMarketParticipant, HighValueDealing}
import models.businessmatching.{BusinessActivities, BusinessActivity}
import play.api.data.FormError

class RemoveBusinessActivitiesFormProviderSpec extends CheckboxFieldBehaviours {

  val fp = new RemoveBusinessActivitiesFormProvider()

  val fieldName = "value"

  ".value" when {

    "number of activities is 2" must {

      val form = fp(2)

      val requiredKey = "error.required.bm.remove.service"

      behave like checkboxField[BusinessActivity](
        form,
        fieldName,
        validValues = BusinessActivities.all.toSeq,
        invalidError = FormError(s"$fieldName[0]", "error.invalid")
      )

      behave like mandatoryCheckboxField(
        form,
        fieldName,
        requiredKey
      )

      "give error when all checkboxes are selected" in {

        val result = form.bind(
          Map(
            s"$fieldName[0]" -> AccountancyServices.toString,
            s"$fieldName[1]" -> ArtMarketParticipant.toString
          )
        )

        result.errors shouldBe Seq(FormError(fieldName, "error.required.bm.remove.leave.twobusinesses"))
      }
    }

    "number of activities is larger than 2" must {

      val form = fp(3)

      val requiredKey = "error.required.bm.remove.service.multiple"

      behave like checkboxField[BusinessActivity](
        form,
        fieldName,
        validValues = BusinessActivities.all.toSeq,
        invalidError = FormError(s"$fieldName[0]", "error.invalid")
      )

      behave like mandatoryCheckboxField(
        form,
        fieldName,
        requiredKey
      )

      "give error when all checkboxes are selected and number of services is not 2" in {

        val result = form.bind(
          Map(
            s"$fieldName[0]" -> AccountancyServices.toString,
            s"$fieldName[1]" -> ArtMarketParticipant.toString,
            s"$fieldName[2]" -> HighValueDealing.toString
          )
        )

        result.errors shouldBe Seq(FormError(fieldName, "error.required.bm.remove.leave.one"))
      }
    }
  }
}
