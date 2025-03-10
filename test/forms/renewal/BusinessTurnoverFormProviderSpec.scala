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

package forms.renewal

import forms.behaviours.FieldBehaviours
import models.renewal.BusinessTurnover
import org.scalacheck.Gen
import play.api.data.FormError

class BusinessTurnoverFormProviderSpec extends FieldBehaviours {

  val form = new BusinessTurnoverFormProvider()()

  val field = "businessTurnover"

  "BusinessTurnoverFormProvider" must {

    behave like fieldThatBindsValidData(
      form,
      field,
      Gen.oneOf(BusinessTurnover.all.map(_.toString))
    )

    behave like mandatoryField(form, field, FormError(field, "error.required.renewal.ba.business.turnover"))

    "fail to bind" when {

      "input is invalid" in {

        val result = form.bind(Map(field -> "foo"))

        result.value        shouldBe None
        result.error(field) shouldBe Some(FormError(field, "error.invalid"))

      }
    }
  }
}
