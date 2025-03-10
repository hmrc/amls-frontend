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
import play.api.data.{Form, FormError}

class InvolvedInOtherFormProviderSpec extends FieldBehaviours {

  val formProvider: InvolvedInOtherFormProvider = new InvolvedInOtherFormProvider()

  val form: Form[Boolean]  = formProvider()
  val fieldName: String    = "involvedInOther"
  val errorMessage: String = "error.required.renewal.ba.involved.in.other"

  "form" must {
    "bind" when {
      "'No' is submitted" in {
        val boundForm = form.bind(Map(fieldName -> "false"))
        boundForm.value  shouldBe Some(false)
        boundForm.errors shouldBe Nil
      }

      "'Yes' is submitted" in {
        val boundForm = form.bind(Map(fieldName -> "true"))
        boundForm.value  shouldBe Some(true)
        boundForm.errors shouldBe Nil
      }
    }

    "not bind" when {
      "nothing is submitted" in {
        val boundForm = form.bind(Map(fieldName -> ""))
        boundForm.errors.head shouldBe FormError(fieldName, "error.required.renewal.ba.involved.in.other")
      }
    }
  }
}
