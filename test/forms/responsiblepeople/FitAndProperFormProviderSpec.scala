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

package forms.responsiblepeople

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.{Form, FormError}

class FitAndProperFormProviderSpec extends BooleanFieldBehaviours[Boolean] {

  override val form: Form[Boolean]  = new FitAndProperFormProvider()()
  override val fieldName: String    = "hasAlreadyPassedFitAndProper"
  override val errorMessage: String = "error.required.rp.fit_and_proper"

  val formError: FormError = FormError(fieldName, errorMessage)

  "FitAndProperFormProvider" must {

    behave like booleanField(form, fieldName, formError)

    s"fail to bind when $fieldName is empty" in {

      val result = form.bind(
        Map(
          fieldName -> ""
        )
      )

      result.value  shouldBe None
      result.errors shouldBe (Seq(formError))
    }
  }
}
